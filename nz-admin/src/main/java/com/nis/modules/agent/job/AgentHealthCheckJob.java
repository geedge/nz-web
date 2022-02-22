package com.nis.modules.agent.job;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.Constant;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.agent.util.AgentUtil;
import com.nis.modules.endpoint.entity.MonitorEndpointState;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.sys.service.SysConfigService;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;

@DisallowConcurrentExecution
public class AgentHealthCheckJob extends QuartzJobBean {

	private static Log log = Log.get();

	@Value("${connect.timeout:30000}")
	private Integer connectTimeout;

	@Autowired
	private PromserverService promService;
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
		// 检查 haMode
		String serverId = (String) redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
		if (!StrUtil.equals(serverId, Constant.SERVER_ID))
			return;

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
	 * 调用 agent/healthy 更新 prom_server 数据状态
	 */
	@SuppressWarnings("unchecked")
	public void updateAgentStatus() {
		List<Promserver> allPromServers = promService.list();
		String metricsStorageType = configService.getValueOrDefault(Constant.SYSCONFIG_KEY_METRICS_STORAGE_TYPE, "1");
		allPromServers.stream().forEach(promserver -> promserver.setStatus(null));

		List<Callable<Promserver>> tasks = new ArrayList<>();
		allPromServers.stream().forEach(promServer -> {
			Callable<Promserver> task = new Callable<Promserver>() {
				@Override
				public Promserver call() throws Exception {
					String result = null;
					try {
						result = AgentUtil.requestGet(promServer, "/healthy", null, String.class);
					} catch (RuntimeException e) {
						log.error(e, "request error,agent:{}", Tool.JSONUtil.toJsonStr(promServer));
					}
					if (log.isDebugEnabled()) {
						log.debug("request result,agent:{},result:{}", Tool.JSONUtil.toJsonStr(promServer), result);
					}
					// 接口请求失败
					if (result == null) {
						promServer.setStatus(0);
						promServer.setStatusInfo(getPromStatusInfoErrorMsg());
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
										|| (down.equals(cortexState) && promServer.getType() == Constant.PromserverType.GLOBAL.getValue() && "2".equals(metricsStorageType))
										|| (down.equals(lokiState) && promServer.getType() == Constant.PromserverType.GLOBAL.getValue() )) {
									// 异常
									promServer.setStatus(0);
								} else {
									if ("FALSE".equals(authState)) {
										// 认证失败
										promServer.setStatus(-1);
									} else {
										// 状态不为 down ，继续判断 可用状态
										if (unavailable.equals(cortexState)) {
											Boolean cortexUpdateExist = redisTemplate
													.hasKey(Constant.REDIS_KEY_CORTEX_UPDATE);
											// 返回为 UNAVAILABLE 但不存在更新标识，认为其不可用
											if (!cortexUpdateExist)
												promServer.setStatus(0);
										}
										if (unavailable.equals(lokiState)) {
											Boolean lokiUpdateExist = redisTemplate
													.hasKey(Constant.REDIS_KEY_LOKI_UPDATE);
											// 返回为 UNAVAILABLE 但不存在更新标识，认为其不可用
											if (!lokiUpdateExist)
												promServer.setStatus(0);
										}
										// 服务可用
										if (promServer.getStatus() == null)
											promServer.setStatus(1);
									}
								}
								data.remove("auth");
								promServer.setStatusInfo(JSONObject.toJSONString(data));
							} else {
								promServer.setStatus(0);
								promServer.setStatusInfo(getPromStatusInfoErrorMsg());
							}
						} catch (Exception e) {
							log.error(e, "request error,agent:{}", Tool.JSONUtil.toJsonStr(promServer));
							promServer.setStatus(0);
							promServer.setStatusInfo(getPromStatusInfoErrorMsg());
						}
					}
					promServer.setCheckTime(new Date());
					return promServer;
				}
			};
			tasks.add(task);
		});

		try {
			List<Future<Promserver>> futures = exec.invokeAll(tasks);
			for (Future<Promserver> future : futures) {
				if (!future.isCancelled()) {
					Promserver promserver = future.get();
					Object agent = redisTemplate.opsForHash().get(Constant.AGENT_STATE_CACHE_KEY,promserver.getId().toString());
					Promserver promserverCache = JSON.parseObject((String)agent,Promserver.class);
					if(counter % forceSync == 0 || ToolUtil.isEmpty(promserverCache) || !ObjectUtil.equal(promserverCache.getStatus(), promserver.getStatus())) {						
						// 判断状态是否缓存与数据库数据是否一致 不一致需要修改数据库信息
						promService.updateStatusInfo(promserver);
					}
					redisTemplate.opsForHash().put(Constant.AGENT_STATE_CACHE_KEY,promserver.getId().toString(),JSON.toJSONString(promserver));
					// 超时周期设置为两个agent周期
			        redisTemplate.expire(Constant.AGENT_STATE_CACHE_KEY, 1, TimeUnit.MINUTES);
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
