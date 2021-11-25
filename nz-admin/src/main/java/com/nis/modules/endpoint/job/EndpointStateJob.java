package com.nis.modules.endpoint.job;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.modules.endpoint.dao.MonitorEndpointStateDao;
import com.nis.modules.endpoint.entity.MonitorEndpointConfig;
import com.nis.modules.endpoint.entity.MonitorEndpointState;
import com.nis.modules.endpoint.service.MonitorEndpointConfigService;
import com.nis.modules.endpoint.service.MonitorEndpointStateService;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
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
    private MonitorEndpointConfigService endpointConfigService;

    @Autowired
    private MonitorEndpointStateDao endpointStateDao;

    @Autowired
    private MonitorEndpointStateService endpointStateService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
    	Thread.currentThread().setName("EndpointStateJob");
    	// 检查 haMode
        String serverId = (String) redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
        if (!StrUtil.equals(serverId, Constant.SERVER_ID)) return;
        log.info("Endpoint state job 开始执行");
        StopWatch sw = new StopWatch();
        sw.start();
        try {
            this.updateEndpointStatus();
        } catch (RuntimeException e) {
            log.error(e);
        } finally {
            sw.stop();
            log.info("Endpoint state 执行结束，Run Time: {}", sw.toString());
        }
    }

    /**
     * 更新 endpoint 状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateEndpointStatus() {
        // 随机获取一个 Global Prometheus
        Promserver promserver = promserverService.getOneAvaPromServer(null, null, Constant.PromserverType.GLOBAL.getValue());
        if (promserver == null) {
            log.debug("Endpoint 状态为全部为 0 (DOWN),当前暂无可用 promserver");
            this.allMetricsEndpointStateDown();
            return;
        }

        // 所有的 metrics endpoint config
        List<MonitorEndpointConfig> metricsEndpointConfigList = endpointConfigService.getEndpointConfigInfoListByType(Constant.MonitorConfType.METRICS.getValue());

        // up 的 metrics / logs endpoint
        List<MonitorEndpointState> upMetricsEndpointStateList = this.getUpMetricsEndpointStateList(promserver, metricsEndpointConfigList);

        // 处理 Down 掉的 endpoint / 设置对应的状态
        Set<Integer> upMetricsConfId = upMetricsEndpointStateList.stream().map(MonitorEndpointState::getConfigId).collect(Collectors.toSet());
        List<MonitorEndpointConfig> downEndpointList = metricsEndpointConfigList.stream().filter(conf -> !upMetricsConfId.contains(conf.getId())).collect(Collectors.toList());

        // 设置 down state
        List<MonitorEndpointState> downEndpointStateList = new ArrayList<>();
        for (MonitorEndpointConfig conf : downEndpointList) {
            Integer state = 0;
            // 0：关闭 1：开启
            Integer monitor = conf.getStateConf().getMonitor();
            // 0：停用 1：启用
            Integer enabled = conf.getEnable();
            // 开启：ON, 关闭：OFF
            String dcStateStr = conf.getDc().getState();
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
            downEndpointStateList.add(new MonitorEndpointState(conf.getEndpointId(), conf.getId(), conf.getType(), state, conf.getVersion(), new Date()));
        }

        List allState = new ArrayList();
        allState.addAll(upMetricsEndpointStateList);
        allState.addAll(downEndpointStateList);
        // 保存
        endpointStateDao.deleteConfStateByType(Constant.MonitorConfType.METRICS.getValue());
        endpointStateService.saveBatch(allState);
    }

    /**
     * 返回 up metrics endpoint state list
     *
     * @param promserver
     * @param endpointConfigList
     * @return
     */
    private List<MonitorEndpointState> getUpMetricsEndpointStateList(Promserver promserver, List<MonitorEndpointConfig> endpointConfigList) {
    	String promPathPrefix = promserverService.getPromPathPrefix();
        String urlPath = promPathPrefix + "/api/v1/query";
        String queryString = "query=up";

        Map<Integer, String> metricsEndpointIdAndState = getMetricsEndpointIdAndStateMapByPath(promserver, urlPath, queryString);

        List<MonitorEndpointState> upMetricsEndpointStateList = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : metricsEndpointIdAndState.entrySet()) {
            Integer endpointId = entry.getKey();
            String stateStr = entry.getValue();
            if ("1".equals(stateStr)) {
                MonitorEndpointConfig endpointConfig = endpointConfigList.stream().filter(conf -> conf.getEndpointId().equals(endpointId) && StrUtil.equals(Constant.MonitorConfType.METRICS.getValue(), conf.getType())).findFirst().orElse(null);
                if (endpointConfig == null) continue;
                upMetricsEndpointStateList.add(new MonitorEndpointState(endpointId, endpointConfig.getId(), Constant.MonitorConfType.METRICS.getValue(), 1, endpointConfig.getVersion(), new Date()));
            }
        }
        return upMetricsEndpointStateList;
    }

    /**
     * 全部 metrics endpoint 状态为 down
     */
    private void allMetricsEndpointStateDown() {
        endpointStateDao.deleteConfStateByType(Constant.MonitorConfType.METRICS.getValue());
        List<MonitorEndpointConfig> metricsEndpointConfigList = endpointConfigService.list(new LambdaQueryWrapper<MonitorEndpointConfig>().eq(MonitorEndpointConfig::getType, Constant.MonitorConfType.METRICS.getValue()));
        List<MonitorEndpointState> stateList = new ArrayList<>(metricsEndpointConfigList.size());
        metricsEndpointConfigList.forEach(conf -> stateList.add(new MonitorEndpointState(conf.getEndpointId(), conf.getId(), conf.getType(), 0, conf.getVersion(), new Date())));
        endpointStateService.saveBatch(stateList);
    }

    /**
     * 根据 url 请求带理接口并返回 endpoint_id / state 数据
     *
     * @param promserver
     * @param urlPath
     * @param queryString
     * @return
     */
    private Map<Integer, String> getMetricsEndpointIdAndStateMapByPath(Promserver promserver, String urlPath, String queryString) {
        JSONObject jsonObject = promserverService.requestAgent(promserver, urlPath, queryString);
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
}
