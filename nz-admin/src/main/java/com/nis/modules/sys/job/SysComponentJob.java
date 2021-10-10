package com.nis.modules.sys.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.sys.entity.SysComponent;
import com.nis.modules.sys.service.SysComponentService;
import com.nis.modules.sys.service.SysConfigService;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;

/**
 * 系统组件 状态检查线程 1、更新当前组件的状态 a、当设置了host时，直接使用 host b、没有设置host时，通过socket 连接 数据库
 * 获取local address 2、主进程 a、清除超过两个周期没有更新的记录 b、检查 sys_config 表 alert_api
 * 是否在sys_component 存在，不存在更新
 * 
 */
@DisallowConcurrentExecution
public class SysComponentJob extends QuartzJobBean {
	private static final Log log = Log.get();

	@Autowired
	private SysComponentService sysComponentService;
	@Autowired
	private SysConfigService sysConfigService;
	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	@Value("${server.host:}")
	private String host;
	@Value("${server.port}")
	private Integer port;
	@Value("${database.host}")
	private String dbHost;
	@Value("${database.port:3306}")
	private Integer dbPort;
	@Value("${server.ssl.enabled:false}")
	private boolean sslEnabled;

	@Override
	@SuppressWarnings("unchecked")
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		// 获取本机ip
		String localHost = Tool.StrUtil.isNotBlank(host) ? host : Tool.NetUtil.getLocalIPBySocket(dbHost, dbPort);
		String protocol = sslEnabled ? "https" : "http";
		// 正确得到本机ip，更新 sys_component 表
		if (Tool.StrUtil.isNotBlank(localHost)) {
			SysComponent component = new SysComponent();
			component.setServerid(Constant.SERVER_ID);
			component.setHost(localHost);
			component.setPort(port);
			component.setProtocol(protocol);
			component.setComponent(Constant.NZ_WEB_COMPONENT_NAME);
			component.setUtime(System.currentTimeMillis());
			sysComponentService.updateComponent(component);
		}

		// 检查 haMode
		String serverId = redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
		if (!StrUtil.equals(serverId, Constant.SERVER_ID)) {
			return;
		}
		// 删除超过3min没有更新的component
		long ltime = System.currentTimeMillis() - 3 * 60 * 1000;
		sysComponentService.remove(Wrappers.lambdaQuery(new SysComponent()).lt(SysComponent::getUtime, ltime));
		// 检查 alert_api是否需要更新
		String alertApi = sysConfigService.getValue(Constant.ALERT_API);
		SysComponent sc = sysComponentService.findByUrl(alertApi);
		if (sc == null) {
			SysComponent one = sysComponentService.getOne(Wrappers.lambdaQuery(new SysComponent())
					.eq(SysComponent::getComponent, Constant.NZ_WEB_COMPONENT_NAME)
					.orderByDesc(SysComponent::getUtime));
			if (one != null) {
				alertApi = String.format("%s://%s:%s", one.getProtocol(), one.getHost(), one.getPort());
				sysConfigService.updateValueByKey(Constant.ALERT_API, alertApi);
				log.info("update alert api : {}", alertApi);
			}else {
				log.warn("No nz-web components available");
			}
		}
	}

}
