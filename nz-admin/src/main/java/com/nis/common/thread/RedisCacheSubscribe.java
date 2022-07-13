package com.nis.common.thread;

import java.util.Map;

import javax.servlet.Filter;

import com.alibaba.fastjson.JSONPath;
import com.nis.common.utils.Tool;
import com.nis.modules.report.job.ReportJobManager;
import com.nis.modules.sys.job.SysBackJobManager;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.crazycake.shiro.RedisCacheManager;
import org.crazycake.shiro.RedisSessionDAO;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import com.nis.common.interceptor.TokenCheckFilter;
import com.nis.common.utils.Constant;
import com.nis.modules.alert.backend.SNMPTrapReceiver;
import com.nis.modules.sys.shiro.MySessionManager;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;

@Component
public class RedisCacheSubscribe  implements MessageListener{

	private static Log log = Log.get();

	@Autowired
	private RedisTemplate<?, ?> redisTemplate;
	
	@Autowired
	private ShiroFilterFactoryBean shiroFilterFactoryBean;
	
	@Autowired
	private RedisSessionDAO redisSessionDao;
	
	@Autowired
	private RedisCacheManager redisCacheManager;
	
	@Autowired
	private MySessionManager mySessionManager;
	
	@Autowired
	private SNMPTrapReceiver trapReceiver;

	@Autowired
	private ReportJobManager reportJobManager;

	@Autowired
	private SysBackJobManager sysBackJobManager;
	
	@Override
	public void onMessage(Message message, byte[] pattern) {
		byte[] body = message.getBody();
		byte[] channel = message.getChannel();  
		RedisSerializer<?> stringSerializer = redisTemplate.getStringSerializer();
		String value = new String(body);
		String topic = (String)stringSerializer.deserialize(channel);
		
		String snmpPort = StrUtil.equals(topic, Constant.SYSCONFIG_SNMP_PORT_KEY) ? value : null;
		String sessionTimeOut = StrUtil.equals(topic, Constant.SYSCONFIG_SESSION_TIMEOUT_KEY) ? value : null;
	
		if(StrUtil.isNotBlank(snmpPort)) {
			// 监听到snmp端口变化 
			try {
				trapReceiver.restart(Integer.valueOf(snmpPort));
			} catch (Exception e) {
				log.error(e,"snmp trap receiver restart error");
			}
		}else if(StrUtil.isNotBlank(sessionTimeOut)) {
			// 监听到session过期时间变化
			Map<String, Filter> filters = shiroFilterFactoryBean.getFilters();
			TokenCheckFilter filter = (TokenCheckFilter) filters.get("tokenCheckFilter");
//			Integer loginExpiration = filter.getLoginExpiration();
//			System.out.println(loginExpiration);
			filter.setLoginExpiration(Integer.valueOf(sessionTimeOut));
			redisCacheManager.setExpire(Integer.valueOf(sessionTimeOut) * 60);
			redisSessionDao.setExpire(Integer.valueOf(sessionTimeOut) * 60);
			mySessionManager.setGlobalSessionTimeout(Integer.valueOf(sessionTimeOut) * 60 * 1000);
		}

		// dashboard report
		if(StrUtil.equals(topic, Constant.REPORT_TOPIC_NAME)){
			log.info("Report consumer message, report begin ...");
			log.info("consumer message :{}", value);
			String reportEnable = JSONPath.read(value, "report.enable").toString();
			String panelId = JSONPath.read(value, "panelId").toString();

			if (Tool.StrUtil.equalsIgnoreCase("true", StrUtil.nullToDefault(reportEnable, "false"))) {
				try {
					reportJobManager.delJobByName(panelId);
					reportJobManager.createReportJob(value, panelId);
					log.info("createReportJob success.");
				} catch (SchedulerException e) {
					log.error("report job create or update error,panelId:{}", e, panelId);
				}
			} else {
				try {
					reportJobManager.delJobByName(panelId);
				} catch (SchedulerException e) {
					log.error("delete report job error, panelId:{}", panelId, e);
				}
			}
			log.info("Report consumer message, report finished");
		}

		// sys backup
		if (StrUtil.equals(topic, Constant.BACKUP_TOPIC_NAME)) {
			log.info("Sys-backup consumer message begin ...");
			log.info("consumer message :{}", value);
			// 1 开启 0 关闭
			String state = JSONPath.read(value, "state").toString();

			if (Tool.StrUtil.equalsIgnoreCase("1", StrUtil.nullToDefault(state, "1"))) {
				try {
					sysBackJobManager.delJob();
					sysBackJobManager.createSysBackJob(value);
				} catch (SchedulerException e) {
					log.error("report job create or update error", e);
				}
			} else {
				try {
					sysBackJobManager.delJob();
				} catch (SchedulerException e) {
					log.error("delete sys-backup job error", e);
				}
			}
			log.info("Sys-backup consumer message, finished");
		}
	}

	@Bean
	RedisMessageListenerContainer container(MessageListenerAdapter listenerAdapter) {
	
	    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
	    container.setConnectionFactory(redisTemplate.getConnectionFactory());
	    container.addMessageListener(listenerAdapter, new PatternTopic(Constant.SYSCONFIG_SESSION_TIMEOUT_KEY));
	    container.addMessageListener(listenerAdapter, new PatternTopic(Constant.SYSCONFIG_SNMP_PORT_KEY));
		container.addMessageListener(listenerAdapter, new PatternTopic(Constant.REPORT_TOPIC_NAME));
		container.addMessageListener(listenerAdapter, new PatternTopic(Constant.BACKUP_TOPIC_NAME));
	    return container;
	}
	
	@Bean
	MessageListenerAdapter listenerAdapter() {
	    return new MessageListenerAdapter(this);
	}
	
}
