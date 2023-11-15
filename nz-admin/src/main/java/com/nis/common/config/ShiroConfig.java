package com.nis.common.config;

import Aladdin.HaspStatus;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.interceptor.TokenCheckFilter;
import com.nis.common.utils.*;
import com.nis.modules.metric.dto.NezhaMetrics;
import com.nis.modules.sys.dao.SysApiKeyDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.LicenseService;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.shiro.*;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.Connector;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.filter.AccessControlFilter;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shiro的配置文件
 *
 */
@Configuration
@DependsOn(value = { "mybatisPlusConfig", "redisConfig" })
public class ShiroConfig {

	@Autowired
	private SysApiKeyDao sysApiKeyDao;

	@Autowired
	private SysConfigService sysconfigService;

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
		filterMap.put("/sys/i18n/lang", "anon"); // 国际化文件获取接口
		filterMap.put("/mfa/**", "anon"); // mfa 认证接口
		filterMap.put("/healthy", "anon"); // 健康检查接口
		filterMap.put("/buildInfo", "anon"); // 编译信息查询接口
		filterMap.put("/terminal.ws", "anon"); // terminal websocket 接口
		filterMap.put("/terminal/monitor.ws", "anon"); // terminal/monitor websocket 接口
		filterMap.put("/sys/license/**", "anon");// license 相关接口
		filterMap.put("/monitor/project/topo/icon/**", "anon"); // topo icon 相关接口
		filterMap.put("/setup/**", "anon");// 程序配置 相关接口
		filterMap.put("/sys/timezone", "anon");// timezone
		filterMap.put("/sys/appearance", "anon"); // 系统外观配置查询接口
		filterMap.put("/sys/backup/sync", "anon");// 系统备份同步接口
		filterMap.put("/ctl/**", "anon"); // 管理员账号密码重置接口
		filterMap.put("/ui/**", "anon"); // UI 接口
		filterMap.put("/topology/**", "anon"); // topology 接口
		filterMap.put("/file/download/**", "anon"); // 文件下载接口

		/*
		 * 自定义 过滤器
		 */
		filterMap.put("/actuator/prometheus", "metricsFilter"); // prometheus metrics
		filterMap.put("/actuator/**", "anon"); // 诊断接口
		filterMap.put("/**", "tokenCheckFilter,licenseFilter");

		Map<String, Filter> cumstomFilterMap = new LinkedHashMap<String, Filter>();
		LicenseShiroFilter licenseShiroFilter = new LicenseShiroFilter();
		MetricsShiroFilter metricsShiroFilter = new MetricsShiroFilter();
		TokenCheckFilter tokenCheckFilter = new TokenCheckFilter(sysApiKeyDao);
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
			if (Tool.StrUtil.equalsIgnoreCase(compileType, "debug")) {
				return true;
			}

			Integer status = licenseService.verify();
			if (!Tool.NumberUtil.equals(status, HaspStatus.HASP_STATUS_OK)) {
				HttpServletResponse httpResponse = (HttpServletResponse) response;
				httpResponse.setStatus(HttpStatus.FORBIDDEN.value());
				httpResponse.getWriter()
						.write(Tool.JSONUtil.toJsonStr(R.error(RCode.LICENSE_FILE_INVALID.setParam(status))));
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
		protected boolean isAccessAllowed(ServletRequest servletRequest, ServletResponse servletResponse, Object o)
				throws Exception {
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



	@Bean
	public SessionDAO sessionDAO(){
		MariadbSessionDao sessionDao = new MariadbSessionDao();
		return sessionDao;
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
		MariadbSessionManager mySessionManager = new MariadbSessionManager();
		mySessionManager.setSessionDAO(sessionDAO());
		mySessionManager.setGlobalSessionTimeout(loginExpiration * 60 * 1000);
		// Disable Session ID cookie
		mySessionManager.setSessionIdCookieEnabled(false);
		mySessionManager.setSessionFactory(new MariadbSessionFactory());
		return mySessionManager;
	}

	@Bean
	public SecurityManager securityManager() {
		DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
		// 设置realm.
		securityManager.setRealm(myShiroRealm());
		// 自定义session管理 使用sessionDao
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