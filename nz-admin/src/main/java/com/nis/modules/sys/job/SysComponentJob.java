package com.nis.modules.sys.job;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.service.MonitorEventLogService;
import com.nis.modules.election.dao.LeaderElectionDao;
import com.nis.modules.sys.entity.SysComponent;
import com.nis.modules.sys.service.SysComponentService;
import com.nis.modules.sys.service.SysConfService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

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
	private SysConfService sysConfService;
	@Autowired
	private MonitorEventLogService eventLogService;
	@Value("${server.host:}")
	private String host;
	@Value("${server.port}")
	private Integer port;
	@Value("${database.url}")
	private String databaseUrl;
	@Value("${server.ssl.enabled:false}")
	private boolean sslEnabled;

	@Override
	@SuppressWarnings("unchecked")
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		Thread.currentThread().setName("SysComponentJob");
		// 获取本机ip
		List<String> urlList = Tool.ListUtil.list(false, this.databaseUrl.split(","));
		String[] urlArr = urlList.get(0).split(":");
		String localHost = Tool.StrUtil.isNotBlank(host) ? host : Tool.NetUtil.getLocalIPBySocket(urlArr[0], Integer.valueOf(urlArr[1]));
		String protocol = sslEnabled ? "https" : "http";
		// 正确得到本机ip，更新 sys_component 表
		if (Tool.StrUtil.isNotBlank(localHost)) {
			SysComponent component = new SysComponent();
			
			component.setHost(localHost);
			component.setPort(port);
			component.setProtocol(protocol);
			component.setComponent(Constant.NZ_WEB_COMPONENT_NAME);
			String hostName = StrUtil.EMPTY;
			try {
				hostName = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				log.warn("Failed to get hostname, host: {}", host);
			}
			component.setHostname(hostName);
			SysComponent sysComponent = sysComponentService.getOne(Wrappers.lambdaQuery(component));
			component.setServerid(Constant.SERVER_ID);
			component.setUtime(System.currentTimeMillis());
			if(sysComponent != null) {
				component.setId(sysComponent.getId());
			}
			sysComponentService.saveOrUpdate(component);
		}

		// 检查 HA_LOCK
		LeaderElectionDao dao = LeaderElectionDao.getInstance();
		if (BooleanUtil.negate(dao.isLeader(Constant.SYS_HA_LOCK, Constant.SERVER_ID))) return;
		// 删除超过3min没有更新的component
		long ltime = System.currentTimeMillis() - 3 * 60 * 1000;
		sysComponentService.remove(Wrappers.lambdaQuery(new SysComponent()).lt(SysComponent::getUtime, ltime));
		// 检查 alert_api是否需要更新
		String alertApi = sysConfService.getValue(Constant.ALERT_API);
		SysComponent sc = sysComponentService.findByUrl(alertApi);
		if (sc == null) {
			SysComponent one = sysComponentService.getOne(Wrappers.lambdaQuery(new SysComponent())
					.eq(SysComponent::getComponent, Constant.NZ_WEB_COMPONENT_NAME)
					.orderByDesc(SysComponent::getUtime));
			if (one != null) {
				alertApi = String.format("%s://%s:%s", one.getProtocol(), one.getHost(), one.getPort());
				// update alert_api
				sysConfService.updateValueByKey(Constant.ALERT_API, alertApi);
				// increment monitor_event_log.prometheus
				eventLogService.increment("prometheus");
				log.info("update alert api : {}", alertApi);
			}else {
				log.warn("No nz-web components available");
			}
		}
	}

}
