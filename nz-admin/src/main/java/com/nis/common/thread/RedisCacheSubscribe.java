package com.nis.common.thread;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.nis.common.config.SNMPTrapServer;
import com.nis.common.interceptor.TokenCheckFilter;
import com.nis.common.utils.Constant;
import com.nis.common.utils.SpringContextUtils;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.shiro.MySessionManager;

import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.crazycake.shiro.RedisCacheManager;
import org.crazycake.shiro.RedisSessionDAO;
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

import javax.servlet.Filter;
import java.io.IOException;
import java.util.Map;

@Component
public class RedisCacheSubscribe  implements MessageListener{

	private static Log log = Log.get();

	@Autowired
	private RedisTemplate redisTemplate;
	
	@Autowired
	private ShiroFilterFactoryBean shiroFilterFactoryBean;
	
	@Autowired
	private RedisSessionDAO redisSessionDao;
	
	@Autowired
	private RedisCacheManager redisCacheManager;
	
	@Autowired
	private SysConfigService sysConfigService;
	
	@Autowired
	private MySessionManager mySessionManager;
	
	@Override
	public void onMessage(Message message, byte[] pattern) {
		byte[] body = message.getBody();
		byte[] channel = message.getChannel();  
		RedisSerializer stringSerializer = redisTemplate.getStringSerializer();
		String value = new String(body);
		String topic = (String)stringSerializer.deserialize(channel);
		
		String snmpPort = StrUtil.equals(topic, Constant.SYSCONFIG_SNMP_PORT_KEY) ? value : null;
		String sessionTimeOut = StrUtil.equals(topic, Constant.SYSCONFIG_SESSION_TIMEOUT_KEY) ? value : null;
	
		if(StrUtil.isNotBlank(snmpPort)) {
			// 监听到snmp端口变化 
			if(ToolUtil.isNotEmpty(SNMPTrapServer.getSnmp())) {
				try {
					SNMPTrapServer.getSnmp().close();
					SNMPTrapServer server = (SNMPTrapServer) SpringContextUtils.getBean(SNMPTrapServer.class);
					server.initServer();
				} catch (IOException e) {
					log.error(e);
				}
				
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
	}

	@Bean
	RedisMessageListenerContainer container(MessageListenerAdapter listenerAdapter) {
	
	    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
	    container.setConnectionFactory(redisTemplate.getConnectionFactory());
	    container.addMessageListener(listenerAdapter, new PatternTopic(Constant.SYSCONFIG_SESSION_TIMEOUT_KEY));
	    container.addMessageListener(listenerAdapter, new PatternTopic(Constant.SYSCONFIG_SNMP_PORT_KEY));
	    return container;
	}
	
	@Bean
	MessageListenerAdapter listenerAdapter() {
	    return new MessageListenerAdapter(this);
	}
	
}
