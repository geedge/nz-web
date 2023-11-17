package com.nis.modules.agent.job;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.*;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.AgentService;
import com.nis.modules.agent.util.AgentUtil;
import com.nis.modules.election.dao.LeaderElectionDao;
import com.nis.modules.sys.service.SysConfigService;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.*;
import java.util.concurrent.*;

@DisallowConcurrentExecution
public class AgentHealthCheckJob extends QuartzJobBean {

	private static Log log = Log.get();

	@Autowired
	private AgentService agentService;
	@Autowired
	private SysConfigService configService;
	@Autowired
	private RedisTemplate redisTemplate;
	private static ExecutorService exec = Executors.newCachedThreadPool();

	// 计数器，每 forceSync 个周期 执行一次强制执行
	private volatile static Integer counter = 1;

	@Value("${agent.healthy.check.force.sync:15}")
	private Integer forceSync;
	
	@Override
	protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		Thread.currentThread().setName("AgentHealthCheckJob");

		// 检查 HA_LOCK
		LeaderElectionDao dao = LeaderElectionDao.getInstance();
		boolean isLeader = dao.isLeader(Constant.SYS_HA_LOCK, Constant.SERVER_ID);
		log.info("[AgentHealthCheckJob] [Get HA_LOCK] [isLeader: {}]", isLeader);
		if (BooleanUtil.negate(isLeader)) return;

		log.info("agent healthy 开始执行");
		StopWatch sw = new StopWatch();
		sw.start();
		try {
			// 状态更新
			this.updateAgentStatus();
		} catch (RuntimeException e) {
			log.error("agent healthy 更新异常", e);
		} finally {
			sw.stop();
		}
		log.info("agent healthy 执行结束，Run Time: {}", sw.toString());
	}

	/**
	 * 调用 agent/healthy 更新 agent 数据状态
	 */
	@SuppressWarnings("unchecked")
	public void updateAgentStatus() {
		List<Agent> allAgents = agentService.list();
		String metricsStorageType = configService.getValueOrDefault(Constant.SYSCONFIG_KEY_METRICS_STORAGE_TYPE, "1");
		allAgents.stream().forEach(agent -> agent.setStatus(null));

		List<Callable<Agent>> tasks = new ArrayList<>();
		allAgents.stream().forEach(agent -> {
			Callable<Agent> task = new Callable<Agent>() {
				@Override
				public Agent call() throws Exception {
					String result = null;
					try {
						result = AgentUtil.requestGet(agent, "/healthy", null, String.class);
					} catch (RuntimeException e) {
						log.error(e, "request error,agent:{}", Tool.JSONUtil.toJsonStr(agent));
					}
					if (log.isDebugEnabled()) {
						log.debug("request result,agent:{},result:{}", Tool.JSONUtil.toJsonStr(agent), result);
					}
					// 接口请求失败
					if (result == null) {
						agent.setStatus(0);
						agent.setStatusInfo(getPromStatusInfoErrorMsg());
					} else {
						try {
							R resultObj = JSONObject.parseObject(result, R.class);
							if (RCode.SUCCESS.getCode().equals((Integer) resultObj.get("code"))) {
								Map<String, Object> data = (Map) resultObj.get("data");

								String prometheusState = (String) data.get("prometheus");
								String confagentState = (String) data.get("confagent");
								String snmpExporterState = (String) data.get("snmp_exporter");
								String blackboxExporterState = (String) data.get("blackbox_exporter");
								String cortexState = (String) data.get("cortex");
								String lokiState = (String) data.get("loki");
								String authState = (String) data.get("auth");
								String unavailable = "UNAVAILABLE", up = "UP", down = "DOWN";
								if (down.equals(prometheusState) || down.equals(confagentState)
										|| down.equals(snmpExporterState) 
										|| down.equals(blackboxExporterState)
										|| (down.equals(cortexState) && agent.getType() == Constant.AgentType.GLOBAL.getValue() && "2".equals(metricsStorageType))
										|| (down.equals(lokiState) && agent.getType() == Constant.AgentType.GLOBAL.getValue() )) {
									// 异常
									agent.setStatus(0);
								} else {
									if ("FALSE".equals(authState)) {
										// 认证失败
										agent.setStatus(-1);
									} else {
										// 状态不为 down ，继续判断 可用状态
										if (unavailable.equals(cortexState)) {
											Boolean cortexUpdateExist = redisTemplate
													.hasKey(Constant.REDIS_KEY_CORTEX_UPDATE);
											// 返回为 UNAVAILABLE 但不存在更新标识，认为其不可用
											if (!cortexUpdateExist)
												agent.setStatus(0);
										}
										if (unavailable.equals(lokiState)) {
											Boolean lokiUpdateExist = redisTemplate
													.hasKey(Constant.REDIS_KEY_LOKI_UPDATE);
											// 返回为 UNAVAILABLE 但不存在更新标识，认为其不可用
											if (!lokiUpdateExist)
												agent.setStatus(0);
										}
										// 服务可用
										if (agent.getStatus() == null)
											agent.setStatus(1);
									}
								}
								// data.remove("auth");
								agent.setStatusInfo(JSONObject.toJSONString(data));
								// 从 nz-agent /healthy 接口中获取返回得 ts 信息
								Object tsObj = data.get("ts");
								agent.setTs(Tool.ObjectUtil.isNull(tsObj) ? null : Tool.NumberUtil.parseLong(tsObj.toString()));
							} else {
								agent.setStatus(0);
								agent.setStatusInfo(getPromStatusInfoErrorMsg());
							}
						} catch (Exception e) {
							log.error(e, "request error,agent:{}", Tool.JSONUtil.toJsonStr(agent));
							agent.setStatus(0);
							agent.setStatusInfo(getPromStatusInfoErrorMsg());
						}
					}
					agent.setCheckTime(new Date());
					return agent;
				}
			};
			tasks.add(task);
		});

		try {
			List<Future<Agent>> futures = exec.invokeAll(tasks);
			TimedCache<String, Object> cache = TimedCacheManager.getCache(Constant.SYS_STATE_CACHE_PREFIX, Constant.AGENT_STATE_CACHE_KEY);
			for (Future<Agent> future : futures) {
				if (!future.isCancelled()) {
					Agent agent = future.get();
					Object agentInfoInCache = cache.get(agent.getId().toString());
					Agent agentCache = JSON.parseObject((String) agentInfoInCache, Agent.class);
					if(counter % forceSync == 0 || ToolUtil.isEmpty(agentCache) || !ObjectUtil.equal(agentCache.getStatus(), agent.getStatus())) {
						// 判断状态是否缓存与数据库数据是否一致 不一致需要修改数据库信息
						agentService.updateStatusInfo(agent);
					}
					// update
					cache.put(agent.getId().toString(), JSON.toJSONString(agent));
				}
			}
			if(counter % forceSync == 0) {
				counter=1;
			}else {
				counter++;
			}
		} catch (InterruptedException | ExecutionException e) {
			log.error(e);
		}
	}

	/**
	 * 请求接口异常时,status info 信息
	 *
	 * @return
	 */
	private static String getPromStatusInfoErrorMsg() {
		Map errorData = new HashMap(4);
		errorData.put("prometheus", "DOWN");
		errorData.put("confagent", "DOWN");
		errorData.put("snmp_exporter", "DOWN");
		errorData.put("loki", "DOWN");
		errorData.put("cortex", "DOWN");
		return JSONObject.toJSONString(errorData);
	}

}
