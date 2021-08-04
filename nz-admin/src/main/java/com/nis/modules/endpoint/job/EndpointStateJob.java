package com.nis.modules.endpoint.job;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.modules.endpoint.dao.MonitorEndpointStateDao;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.entity.MonitorEndpointState;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.endpoint.service.MonitorEndpointStateService;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@DisallowConcurrentExecution
public class EndpointStateJob extends QuartzJobBean {

    private Log log = Log.get();

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private PromserverService promserverService;

    @Autowired
    private MonitorEndpointService endpointService;

    @Autowired
    private MonitorEndpointStateDao endpointStateDao;

    @Autowired
    private MonitorEndpointStateService endpointStateService;

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
            this.updateEndpointStatus();
        } catch (RuntimeException e) {
            log.error(e);
        } finally {
            sw.stop();
        }
        log.info("Endpoint 状态定时任务执行时长: {}", sw.toString());
    }

    /**
     * 更新 endpoint 状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateEndpointStatus() {
        // 随机获取一个 Global Prometheus
        /*Promserver promserver = promserverService.getOneAvaPromServer(null, null, 1);
        if (promserver == null) {
            log.debug("Endpoint 状态为全部为 0 (DOWN),当前暂无可用 promserver");
            this.allEndpointStateDown();
            return;
        }

        SysConfigEntity configEntity = sysConfigService.getOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, Constant.CONFAGENT_PATH));
        String confagentPath = configEntity.getParamValue();
        String urlPath = confagentPath + "/prometheus/proxy/api/v1/query?query=up";

        Map<Integer, String> endpointIdAndState = getEndpointIdAndStateMapByPath(promserver, urlPath);

        List<MonitorEndpointState> upEndpointStateList = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : endpointIdAndState.entrySet()) {
            Integer endpointId = entry.getKey();
            String stateStr = entry.getValue();
            if ("1".equals(stateStr)) {
                upEndpointStateList.add(new MonitorEndpointState(endpointId, 1, new Date()));
            }
        }

        // 所有的 endpoint
        List<MonitorEndpoint> allEndpointList = endpointService.endpointDetailList();
        // down 掉的 endpoint
        List<Integer> upIdList = upEndpointStateList.stream().map(MonitorEndpointState::getEndpointId).distinct().collect(Collectors.toList());
        List<MonitorEndpoint> downEndpointList = allEndpointList.stream().filter(entity -> !upIdList.contains(entity.getId())).collect(Collectors.toList());

        // 设置 down state
        List<MonitorEndpointState> downEndpointStateList = new ArrayList<>();
        for (MonitorEndpoint endpoint : downEndpointList) {
            Integer state = 0;
            // 0：关闭 1：开启
            Integer monitor = endpoint.getAsset().getState().getMonitor();
            // 0：停用 1：启用
            Integer enabled = endpoint.getEnabled();
            // 开启：ON, 关闭：OFF
            String dcStateStr = endpoint.getDc().getState();
            boolean dcState = dcStateStr.equals("ON") ? true : false;
            if (!dcState && monitor.equals(0) && enabled.equals(1)) {
                state = 2;
            } else if (!dcState && monitor.equals(1) && enabled.equals(0)) {
                state = 4;
            } else if (dcState && monitor.equals(0) && enabled.equals(0)) {
                state = 8;
            } else if (!dcState && monitor.equals(1) && enabled.equals(1)) {
                state = 6;
            } else if (dcState && monitor.equals(0) && enabled.equals(1)) {
                state = 10;
            } else if (dcState && monitor.equals(1) && enabled.equals(0)) {
                state = 12;
            }
            downEndpointStateList.add(new MonitorEndpointState(endpoint.getId(), state, new Date()));
        }

        upEndpointStateList.addAll(downEndpointStateList);

        // 保存
        endpointStateDao.deleteAll();
        endpointStateService.saveBatch(upEndpointStateList);*/
    }

    /**
     * 全部 endpoint 状态为 down
     */
    private void allEndpointStateDown() {
        /*endpointStateDao.deleteAll();
        List<MonitorEndpoint> endpointList = endpointService.list();
        List<MonitorEndpointState> stateList = new ArrayList<>(endpointList.size());
        endpointList.forEach(endpoint -> stateList.add(new MonitorEndpointState(endpoint.getId(), 0, new Date())));
        endpointStateService.saveBatch(stateList);*/
    }

    /**
     * 根据 url 请求带理接口并返回 endpoint_id / state 数据
     *
     * @param promserver
     * @param urlPath
     * @return
     */
    private Map<Integer, String> getEndpointIdAndStateMapByPath(Promserver promserver, String urlPath) {
        JSONObject jsonObject = promserverService.requestAgent(promserver, urlPath);
        Map dataMap = (Map) jsonObject.get("data");
        List<Map> resultList = JSONObject.parseArray(JSONObject.toJSONString(dataMap.get("result")), Map.class);

        Map<Integer, String> map = new HashMap<>();
        for (Map pojo : resultList) {
            Map metricMap = (Map) pojo.get("metric");
            Object endpointId = metricMap.get("endpoint_id");
            if (endpointId == null) continue;
            List<String> timeAndData = (List<String>) pojo.get("value");
            map.put(Integer.valueOf(endpointId.toString()), timeAndData.get(1));
        }
        return map;
    }

    /*private void getEndpointStatusFromPrometheus() {
        try {
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
                            endpointState.setCreateTime(new Date());

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
                        endpointState = new EndpointState(suspendedEndpointId, suspendedEndpointId, new Date(), 2);
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
    }*/
}
