package com.nis.modules.endpoint.job;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.DateUtil;
import com.nis.modules.endpoint.dao.EndpointStateDao;
import com.nis.modules.endpoint.entity.EndpointState;
import com.nis.modules.endpoint.service.EndpointStateService;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.promserver.utils.HttpClientUtils;
import com.nis.modules.sys.dao.SysConfigDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.net.URL;
import java.util.*;

@Slf4j
@DisallowConcurrentExecution
public class EndpointStateJob extends QuartzJobBean {

    @Autowired
    private SysConfigDao sysConfigDao;

    @Autowired
    private EndpointStateDao endpointStateDao;

    @Autowired
    private PromserverService promserverService;

    @Autowired
    private EndpointStateService endpointStateService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        StopWatch sw = new StopWatch();
        sw.start();
        try {
            this.getEndpointStatusFromPrometheus();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sw.stop();
        }
        log.info("Endpoint 状态定时任务执行时长: {}", sw.toString());
    }

    private void getEndpointStatusFromPrometheus() {
        SysConfigEntity endpointStateSwitchConfig = sysConfigDao.selectOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, Constant.ENDPOINT_ALERT_PUSH));
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
                    for (Map pojo : result) {

                        metricMap = (Map) pojo.get("metric");
                        Object endpoint = metricMap.get(Constant.PROMSERVERUP_ENDPOINT_LABEL);

                        if (endpoint == null) continue;

                        Integer endpointId = Integer.valueOf(endpoint.toString());
                        if (!tempEndpointIds.contains(endpointId)) {
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

                    endpointStateDao.deleteAll();
                    endpointStateService.saveBatch(saveEndpointStateList);
                } else {
                    log.error(String.format("Endpoint 状态信息获取失败,URL: %s, 查询状态是: %s, 查询结果是: ", url.toString(), status == null ? "null" : status.toString(), jsonObject));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}