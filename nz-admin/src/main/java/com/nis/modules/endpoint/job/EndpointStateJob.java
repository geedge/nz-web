package com.nis.modules.endpoint.job;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jcraft.jsch.JSchException;
import com.nis.common.utils.Constant;
import com.nis.common.utils.DateUtil;
import com.nis.modules.endpoint.dao.EndpointDao;
import com.nis.modules.endpoint.entity.EndpointState;
import com.nis.modules.endpoint.service.EndpointStateService;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.promserver.utils.HttpClientUtils;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@DisallowConcurrentExecution
public class EndpointStateJob extends QuartzJobBean {
    private Log logger = Log.get();
    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private EndpointDao endpointDao;

    @Autowired
    private PromserverService promserverService;

    @Autowired
    private EndpointStateService endpointStateService;

    @Autowired
    private RedisTemplate redisTemplate;
    
    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
    	// 检查 haMode
    	String serverId = (String) redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
        if(!StrUtil.equals(serverId, Constant.SERVER_ID)) return;

        StopWatch sw = new StopWatch();
        sw.start();
        try {
            this.getEndpointStatusFromPrometheus();
        } catch (RuntimeException e) {
            logger.error(e);
        } finally {
            sw.stop();
        }
        log.info("Endpoint 状态定时任务执行时长: {}", sw.toString());
    }

    private void getEndpointStatusFromPrometheus() {
        SysConfigEntity endpointStateSwitchConfig = sysConfigService.getOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, Constant.ENDPOINT_ALERT_PUSH));
        String endpointStateSwitch = endpointStateSwitchConfig.getParamValue();
        if (!"2".equals(endpointStateSwitch)) {
            log.debug("Endpoint 状态定时任务关闭");
            return;
        }
        log.debug("Endpoint 状态定时任务开启");

        // 随机获取一个 Global Prometheus
        Promserver promserver = promserverService.getOneAvaPromServer(null, null, 1);
        if (promserver == null) {
            log.debug("Endpoint 状态定时任务不执行,当前暂无可用 prometheus");
            return;
        }

        try {
            URL url = new URL("http", promserver.getHost(), promserver.getPort(), "/api/v1/query?query=up");
            JSONObject jsonObject = HttpClientUtils.httpGet(url.toString());

            if (jsonObject == null) {
                log.debug("Endpoint 状态获取失败,URL: " + url.toString());
            } else {
                Object status = jsonObject.get("status");
                if (status != null && "success".equalsIgnoreCase(status.toString())) {
                    Object data = jsonObject.get("data");
                    Map dataMap = JSONObject.parseObject(data.toString(), Map.class);

                    List<EndpointState> saveEndpointStateList = new ArrayList<>();
                    List<Map> result = JSONObject.parseArray(JSONObject.toJSONString(dataMap.get("result")), Map.class);
                    EndpointState endpointState;
                    Map metricMap;
                    List<String> timeAndState;
                    // 用于判断 endpoint id 是否重复
                    Set<Integer> tempEndpointIds = new HashSet<>();
                    // 根据 asset 停用状态 获取对应 endpoint ids
                    List<Integer> suspendedEndpointIds = endpointDao.getSuspendedEndpointIdsByAssetState();
                    // 停用的 endpoint ids
                    List<Integer> disabledEndpointIds = endpointDao.getDisabledEndpointIds();
                    suspendedEndpointIds.addAll(disabledEndpointIds);
                    suspendedEndpointIds = suspendedEndpointIds.stream().distinct().collect(Collectors.toList());
                    for (Map pojo : result) {

                        metricMap = (Map) pojo.get("metric");
                        Object endpoint = metricMap.get(Constant.PROMSERVERUP_ENDPOINT_LABEL);

                        if (endpoint == null) continue;

                        Integer endpointId = Integer.valueOf(endpoint.toString());
                        // endpoint 未重复 并且 不是停用状态
                        if (!tempEndpointIds.contains(endpointId) && !suspendedEndpointIds.contains(endpointId)) {
                            endpointState = new EndpointState();
                            endpointState.setCreateTime(DateUtil.getUTCTimeByConfigTimeZone());

                            timeAndState = (List<String>) pojo.get("value");
                            endpointState.setState(Integer.valueOf(timeAndState.get(1)));

                            endpointState.setId(endpointId);
                            endpointState.setEndpointId(endpointId);
                            saveEndpointStateList.add(endpointState);

                            tempEndpointIds.add(endpointId);
                        }
                    }

                    // 停用状态 endpoint
                    for (Integer suspendedEndpointId : suspendedEndpointIds) {
                        endpointState = new EndpointState(suspendedEndpointId, suspendedEndpointId, DateUtil.getUTCTimeByConfigTimeZone(), 2);
                        saveEndpointStateList.add(endpointState);
                    }

                    List<EndpointState> list = endpointStateService.list();
                    Map<Integer, Integer> idAndStateMap = list.stream().collect(Collectors.toMap(EndpointState::getId, EndpointState::getState));

                    Iterator<EndpointState> iterator = saveEndpointStateList.iterator();
                    while (iterator.hasNext()){
                        EndpointState pojo = iterator.next();

                        Integer state = idAndStateMap.get(pojo.getId());
                        // 状态没有改变就不进行修改
                        if(state != null && state.equals(pojo.getState())){
                            iterator.remove();
                        }
                    }
                    if (CollectionUtils.isNotEmpty(saveEndpointStateList))
                        endpointStateService.saveOrUpdateBatch(saveEndpointStateList);
                } else {
                    log.error(String.format("Endpoint 状态信息获取失败,URL: %s, 查询状态是: %s, 查询结果是: ", url.toString(), status == null ? "null" : status.toString(), jsonObject));
                }
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }
}