package com.nis.modules.endpoint.job;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.endpoint.dao.MonitorEndpointStateDao;
import com.nis.modules.endpoint.entity.MonitorEndpointConfig;
import com.nis.modules.endpoint.entity.MonitorEndpointState;
import com.nis.modules.endpoint.service.MonitorEndpointConfigService;
import com.nis.modules.endpoint.service.MonitorEndpointStateService;
import com.nis.modules.promserver.entity.Agent;
import com.nis.modules.promserver.service.PromApiService;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@DisallowConcurrentExecution
public class EndpointStateJob extends QuartzJobBean {

    private Log log = Log.get();

    @Autowired
    private PromApiService promApiService;

    @Autowired
    private MonitorEndpointConfigService endpointConfigService;

    @Autowired
    private MonitorEndpointStateDao endpointStateDao;

    @Autowired
    private MonitorEndpointStateService endpointStateService;

    @Autowired
    private RedisTemplate redisTemplate;

    // 计数器，每 forceSync 个周期 执行一次强制执行
  	private volatile static Integer counter = 1;

  	@Value("${endpoint.state.force.sync:8}")
  	private Integer forceSync;
  	
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
    @SuppressWarnings("unlikely-arg-type")
	@Transactional(rollbackFor = Exception.class)
    public void updateEndpointStatus() {
        // 随机获取一个 Global Prometheus
        Agent agent = promApiService.getOneAvaliableGlobalAgent();
        if (agent == null) {
            log.debug("Endpoint 状态为全部为 0 (DOWN),当前暂无可用 agent");
            this.allMetricsEndpointStateDown();
            return;
        }

        // 所有的 metrics endpoint config
        List<MonitorEndpointConfig> metricsEndpointConfigList = endpointConfigService.getEndpointConfigInfoListByType(Constant.MonitorConfType.METRICS.getValue());

        // up 的 metrics / logs endpoint
        List<MonitorEndpointState> upMetricsEndpointStateList = this.getUpMetricsEndpointStateList(metricsEndpointConfigList);

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

        List<MonitorEndpointState> allState = new ArrayList<MonitorEndpointState>();
        allState.addAll(upMetricsEndpointStateList);
        allState.addAll(downEndpointStateList);
        
        //判断查询数据与缓存中变化的信息
        Map endpointStates = redisTemplate.opsForHash().entries(Constant.ENDPOINT_METRICS_STATE_CACHE_KEY);
        if(ToolUtil.isNotEmpty(endpointStates) && counter % forceSync != 0) {
        	for(int i=allState.size()-1;i>=0;i--) {
        		MonitorEndpointState endpointState = allState.get(i);
        		if(endpointStates.containsKey(endpointState.getEndpointId().toString())) {
        			MonitorEndpointState cacheEndpointState = JSON.parseObject((String)endpointStates.get(endpointState.getEndpointId().toString()),MonitorEndpointState.class);
        			redisTemplate.opsForHash().put(Constant.ENDPOINT_METRICS_STATE_CACHE_KEY, endpointState.getEndpointId().toString(), JSON.toJSONString(endpointState));
        			if(cacheEndpointState.getState().equals(endpointState.getState())) {
        				// 如果状态相同则只更新缓存中的数据信息
        				allState.remove(i);
        			}
        		}else{
        			// 数据库存在 缓存中不存在
        			redisTemplate.opsForHash().put(Constant.ENDPOINT_METRICS_STATE_CACHE_KEY, endpointState.getEndpointId().toString(), JSON.toJSONString(endpointState));
        		}
            }
        }else {
        	for(MonitorEndpointState endpointState:allState) {
        		redisTemplate.opsForHash().put(Constant.ENDPOINT_METRICS_STATE_CACHE_KEY, endpointState.getEndpointId().toString(), JSON.toJSONString(endpointState));
        	}
        }
        // 超时周期设置为两个endpointState周期
        redisTemplate.expire(Constant.ENDPOINT_METRICS_STATE_CACHE_KEY, 2, TimeUnit.MINUTES);
        // 保存 根据类型以及endpointId进行新增或者修改
        if(ToolUtil.isNotEmpty(allState)) {
        	// 将数据分为新增或者修改
        	List<Integer> endpointIds = allState.stream().map(MonitorEndpointState::getEndpointId).collect(Collectors.toList());
        	List<MonitorEndpointState> existStates = endpointStateService.list(new LambdaQueryWrapper<MonitorEndpointState>()
        			.in(MonitorEndpointState::getEndpointId, endpointIds).eq(MonitorEndpointState::getType, Constant.MonitorConfType.METRICS.getValue()));
        	List<Integer> existIds = existStates.stream().map(MonitorEndpointState::getEndpointId).collect(Collectors.toList());
        	List<MonitorEndpointState> batchUpdateStates = allState.stream().filter(s->existIds.contains(s.getEndpointId())).collect(Collectors.toList());
        	List<MonitorEndpointState> batchAddStates = allState.stream().filter(s->!existIds.contains(s.getEndpointId())).collect(Collectors.toList());
        	if(ToolUtil.isNotEmpty(batchAddStates)) {
        		log.debug("Endpoint state batch add datas info : %s ",JSON.toJSON(batchAddStates));
        		endpointStateService.saveBatch(batchAddStates);
        	}
        	if(ToolUtil.isNotEmpty(batchUpdateStates)) {
        		log.debug("Endpoint state batch update datas info : %s ",JSON.toJSON(batchAddStates));
        		endpointStateService.modifyBatchEndpointState(batchUpdateStates, Constant.MonitorConfType.METRICS.getValue());
        	}
        }
        if(counter % forceSync == 0) {
        	counter = 1;
        }else {
        	counter++;
        }
    }

    /**
     * 返回 up metrics endpoint state list
     *
     * @param endpointConfigList
     * @return
     */
    private List<MonitorEndpointState> getUpMetricsEndpointStateList(List<MonitorEndpointConfig> endpointConfigList) {
        Map<Integer, String> metricsEndpointIdAndState = getMetricsEndpointIdAndStateMapByPath();

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

    private Map<Integer, String> getMetricsEndpointIdAndStateMapByPath() {
        Map requestParam = Tool.MapUtil.newHashMap(2);
        requestParam.put("query", "up");

        JSONObject jsonObject = promApiService.query(HttpEntity.EMPTY, requestParam);
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
