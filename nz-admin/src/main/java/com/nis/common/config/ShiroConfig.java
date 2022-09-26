package com.nis.common.config;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.Connector;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.filter.AccessControlFilter;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.crazycake.shiro.RedisCacheManager;
import org.crazycake.shiro.RedisManager;
import org.crazycake.shiro.RedisSessionDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.interceptor.TokenCheckFilter;
import com.nis.common.utils.Constant;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.metric.dto.NezhaMetrics;
import com.nis.modules.sys.dao.SysApiKeyDao;
import com.nis.modules.sys.dao.SysUserDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.LicenseService;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.shiro.MySessionManager;
import com.nis.modules.sys.shiro.ShiroUtils;
import com.nis.modules.sys.shiro.UserRealm;

import Aladdin.HaspStatus;
import cn.hutool.core.util.StrUtil;
import io.micrometer.core.instrument.MeterRegistry;

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
	
	@Value("${compileType:release}")
	private String compileType;
	@Value("${vendorCode}")
	private String vendorCode;
	@Autowired
    private NezhaMetrics nezhaMetrics;

    @Autowired
    private MeterRegistry meterRegistry;
	@Autowired
	private LicenseService licenseService;
	
	@Bean("shiroFilter")
	public ShiroFilterFactoryBean shiroFilter(SecurityManager securityManager) {
		ShiroFilterFactoryBean shiroFilter = new ShiroFilterFactoryBean();
		shiroFilter.setSecurityManager(securityManager);
		shiroFilter.setLoginUrl("/sys/login");
		shiroFilter.setUnauthorizedUrl("/");

		Map<String, String> filterMap = new LinkedHashMap<>();
		
		
		/*
		 * 静态资源 
		 */
		filterMap.put("/", "anon");
		filterMap.put("/static/**", "anon");
		filterMap.put("/**/*.html", "anon");
		filterMap.put("/**/*.js", "anon");
		filterMap.put("/**/*.png", "anon");
		filterMap.put("/**/*.jpg", "anon");
		filterMap.put("/**/*.css", "anon");
		filterMap.put("/**/*.ico", "anon");
		
		/*
		 * 开放权限接口
		 */
		filterMap.put("/sys/login", "anon"); // 登录接口
		filterMap.put("/api/**", "anon"); // 告警消息接收 接口
		filterMap.put("/sys/i18n/lang", "anon"); //国际化文件获取接口
		filterMap.put("/mfa/**", "anon"); // mfa 认证接口
		filterMap.put("/healthy", "anon"); //健康检查接口
		filterMap.put("/buildInfo", "anon"); //编译信息查询接口
		filterMap.put("/terminal.ws", "anon"); //terminal websocket 接口
		filterMap.put("/terminal/monitor.ws", "anon"); //terminal/monitor websocket 接口
		filterMap.put("/sys/license/**", "anon");// license 相关接口
		filterMap.put("/setup/**", "anon");// 程序配置 相关接口
		filterMap.put("/sys/appearance","anon"); //系统外观配置查询接口
		filterMap.put("/ctl/**","anon"); //管理员账号密码重置接口
		filterMap.put("/ui/**", "anon"); // UI 接口
		filterMap.put("/file/download/**", "anon"); // 文件下载接口
		
		/*
		 * 自定义 过滤器
		 */
		filterMap.put("/actuator/prometheus", "metricsFilter"); //prometheus metrics
		filterMap.put("/actuator/**", "anon"); // 诊断接口
		filterMap.put("/**", "tokenCheckFilter,licenseFilter");

		Integer loginExpiration = getTimeOutData();
		Map<String, Filter> cumstomFilterMap = new LinkedHashMap<String, Filter>();
		TokenCheckFilter tokenCheckFilter = new TokenCheckFilter(redisTemplate, loginExpiration, sysUserDao,sysApiKeyDao);
		LicenseShiroFilter licenseShiroFilter = new LicenseShiroFilter();
		MetricsShiroFilter metricsShiroFilter = new MetricsShiroFilter();
		cumstomFilterMap.put("tokenCheckFilter", tokenCheckFilter);
		cumstomFilterMap.put("licenseFilter", licenseShiroFilter);
		cumstomFilterMap.put("metricsFilter", metricsShiroFilter);
		shiroFilter.setFilters(cumstomFilterMap);
		shiroFilter.setFilterChainDefinitionMap(filterMap);
		
		return shiroFilter;
	}

	@Component
	public class LicenseShiroFilter extends AccessControlFilter {
		
		@Override
		protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue)
				throws Exception {
			// debug 模式不校验 license
			if(Tool.StrUtil.equalsIgnoreCase(compileType, "debug")) {
				return true;
			}
			
			Integer status = licenseService.verify();
			if( ! Tool.NumberUtil.equals(status, HaspStatus.HASP_STATUS_OK)) {
				HttpServletResponse httpResponse=(HttpServletResponse) response;
				httpResponse.setStatus(HttpStatus.FORBIDDEN.value());
				httpResponse.getWriter().write(Tool.JSONUtil.toJsonStr(R.error(RCode.LICENSE_FILE_INVALID.setParam(status))));
				return false;
			}
			return true;
		}

		@Override
		protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
			return false;
		}

	}
	
	public class MetricsShiroFilter extends AccessControlFilter {
	    @Override
	    protected boolean isAccessAllowed(ServletRequest servletRequest, ServletResponse servletResponse, Object o) throws Exception {
	        nezhaMetrics.bindTo(meterRegistry);
	        return true;
	    }

	    @Override
	    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
	        return false;
	    }
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
		// Disable Session ID cookie
		mySessionManager.setSessionIdCookieEnabled(false);
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
