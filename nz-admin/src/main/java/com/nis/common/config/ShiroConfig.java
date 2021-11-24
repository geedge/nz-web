package com.nis.common.config;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.Filter;

import org.apache.catalina.connector.Connector;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.crazycake.shiro.RedisCacheManager;
import org.crazycake.shiro.RedisManager;
import org.crazycake.shiro.RedisSessionDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.RedisTemplate;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.interceptor.LicenseShiroFilter;
import com.nis.common.interceptor.MetricsShiroFilter;
import com.nis.common.interceptor.TokenCheckFilter;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.sys.dao.SysApiKeyDao;
import com.nis.modules.sys.dao.SysUserDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.shiro.MySessionManager;
import com.nis.modules.sys.shiro.ShiroUtils;
import com.nis.modules.sys.shiro.UserRealm;

import cn.hutool.core.util.StrUtil;

/**
 * Shiro的配置文件
 *
 */
@Configuration
@DependsOn(value = { "mybatisPlusConfig", "redisConfig" })
public class ShiroConfig {

	@Autowired
	private RedisConfig config;

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@Autowired
	private SysUserDao sysUserDao;

	@Autowired
	private SysConfigService sysconfigService;

	@Autowired
	private SysApiKeyDao sysApiKeyDao;
	
	@Autowired
	private LicenseShiroFilter licenseFilter;

	@Autowired
	private MetricsShiroFilter metricsShiroFilter;

	@Bean("shiroFilter")
	public ShiroFilterFactoryBean shiroFilter(SecurityManager securityManager) {
		ShiroFilterFactoryBean shiroFilter = new ShiroFilterFactoryBean();
		shiroFilter.setSecurityManager(securityManager);
		shiroFilter.setLoginUrl("/sys/login");
		shiroFilter.setUnauthorizedUrl("/");

		Map<String, String> filterMap = new LinkedHashMap<>();
		filterMap.put("/sys/i18n/lang", "anon");
		filterMap.put("/metrics", "anon");
		filterMap.put("/prometheus", "anon");
		// filterMap.put("/prom/**", "anon");
		filterMap.put("/swagger/**", "anon");
		filterMap.put("/v2/api-docs", "anon");
		filterMap.put("/swagger-ui.html", "anon");
		filterMap.put("/webjars/**", "anon");
		filterMap.put("/swagger-resources/**", "anon");
		filterMap.put("/static/**", "anon");
		filterMap.put("/nezha/**", "anon");
		filterMap.put("/", "anon");
		filterMap.put("/config.json", "anon");
		filterMap.put("/index.html", "anon");
		filterMap.put("/login.html", "anon");
		filterMap.put("/sys/login", "anon");
		filterMap.put("/mfa/**", "anon");
		filterMap.put("/healthy", "anon");
		filterMap.put("/buildInfo", "anon");
		filterMap.put("/favicon.ico", "anon");
		filterMap.put("/captcha.jpg", "anon");
		filterMap.put("/api/**", "anon");
		filterMap.put("/logs/loki/**", "anon");
		filterMap.put("/terminal.ws", "anon");
		filterMap.put("/terminal/monitor.ws", "anon");
		filterMap.put("/sys/license/**", "anon");// license 相关接口
		filterMap.put("/setup/**", "anon");// license 相关接口
		filterMap.put("/actuator/prometheus", "metricsFilter");
		filterMap.put("/**", "tokenCheckFilter");
		
		
		Integer loginExpiration = getTimeOutData();
		Map<String, Filter> cumstomFilterMap = new LinkedHashMap<String, Filter>();
		TokenCheckFilter tokenCheckFilter = new TokenCheckFilter(redisTemplate, loginExpiration, sysUserDao,
				sysApiKeyDao);
		cumstomFilterMap.put("licenseFilter", licenseFilter);
		cumstomFilterMap.put("tokenCheckFilter", tokenCheckFilter);
		cumstomFilterMap.put("metricsFilter", metricsShiroFilter);
		shiroFilter.setFilters(cumstomFilterMap);
		shiroFilter.setFilterChainDefinitionMap(filterMap);
		
		return shiroFilter;
	}

	@Bean
	@ConditionalOnExpression("${nezha.shiroPermisSupport:true}")
	public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
		AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
		advisor.setSecurityManager(securityManager);
		return advisor;
	}

	@Bean(name = { "tomcatServletWebServerFactory" })
	public ConfigurableServletWebServerFactory webServerFactory() {
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
		factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {
			@Override
			public void customize(Connector connector) {
				connector.setProperty("relaxedPathChars", "\"<>[\\]^`{|}");

				connector.setProperty("relaxedQueryChars", "\"<>[\\]^`{|}");
			}
		});

		return factory;
	}

	/**
	 * 配置shiro redisManager 使用的是shiro-redis开源插件
	 *
	 * @return
	 */
	public RedisManager redisManager() {
		RedisManager redisManager = new RedisManager();
		redisManager.setHost(config.getRedisHost() + ":" + config.getRedisPort());
		redisManager.setTimeout(Tool.NumberUtil.parseInt(Tool.StrUtil.toString(config.getRedisTimeout())));
		redisManager.setDatabase(config.getRedisDatabase());
		if (Tool.StrUtil.isNotBlank(config.getRedisPin())) {
			redisManager.setPassword(config.getRedisPin());
		}
		return redisManager;
	}

	/**
	 * cacheManager 缓存 redis实现 使用的是shiro-redis开源插件
	 *
	 * @return
	 */
	@Bean
	public RedisCacheManager cacheManager() {
		Integer loginExpiration = getTimeOutData();
		RedisCacheManager redisCacheManager = new RedisCacheManager();
		redisCacheManager.setRedisManager(redisManager());
		// 配置缓存过期时间
		redisCacheManager.setExpire(loginExpiration * 60);
		redisCacheManager.setKeyPrefix(ShiroUtils.REDIS_KEYPREFIX);
		// user entity ID
		redisCacheManager.setPrincipalIdFieldName("userId");
		return redisCacheManager;
	}

	/**
	 * RedisSessionDAO shiro sessionDao层的实现 通过redis 使用的是shiro-redis开源插件
	 */
	@Bean
	public RedisSessionDAO redisSessionDAO() {
		Integer loginExpiration = getTimeOutData();
		RedisSessionDAO redisSessionDAO = new RedisSessionDAO();
		redisSessionDAO.setRedisManager(redisManager());
		redisSessionDAO.setExpire(loginExpiration * 60);
		redisSessionDAO.setKeyPrefix(ShiroUtils.REDIS_KEYPREFIX);
		return redisSessionDAO;
	}

	/**
	 * 身份认证realm; (这个需要自己写，账号密码校验；权限等)
	 *
	 * @return
	 */
	@Bean
	public UserRealm myShiroRealm() {
		UserRealm myShiroRealm = new UserRealm();
		// 关闭 cache
		myShiroRealm.setCachingEnabled(false);
		return myShiroRealm;
	}

	/**
	 * 自定义 sessionManager
	 *
	 * @return
	 */
	@Bean
	public SessionManager sessionManager() {
		Integer loginExpiration = getTimeOutData();
		MySessionManager mySessionManager = new MySessionManager();
		mySessionManager.setGlobalSessionTimeout(loginExpiration * 60 * 1000);
		mySessionManager.setSessionDAO(redisSessionDAO());
		return mySessionManager;
	}

	@Bean
	public SecurityManager securityManager() {
		DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
		// 设置realm.
		securityManager.setRealm(myShiroRealm());
		// 自定义缓存实现 使用redis
		securityManager.setCacheManager(cacheManager());
		// 自定义session管理 使用redis
		securityManager.setSessionManager(sessionManager());
		return securityManager;
	}

	private Integer getTimeOutData() {
		SysConfigEntity configEntity = sysconfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda()
				.eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_SESSION_TIMEOUT_KEY));
		Integer loginExpiration = 30;
		if (ToolUtil.isNotEmpty(configEntity)) {
			loginExpiration = StrUtil.isNotBlank(configEntity.getParamValue())
					? Integer.valueOf(configEntity.getParamValue())
					: loginExpiration;
		}
		return loginExpiration;
	}
}
