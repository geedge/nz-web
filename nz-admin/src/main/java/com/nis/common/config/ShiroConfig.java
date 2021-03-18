package com.nis.common.config;

import com.nis.common.interceptor.TokenCheckFilter;
import com.nis.common.utils.StringUtils;
import com.nis.modules.sys.dao.SysUserDao;
import com.nis.modules.sys.shiro.MySessionManager;
import com.nis.modules.sys.shiro.ShiroUtils;
import com.nis.modules.sys.shiro.UserRealm;
import org.apache.catalina.connector.Connector;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.crazycake.shiro.RedisCacheManager;
import org.crazycake.shiro.RedisManager;
import org.crazycake.shiro.RedisSessionDAO;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import javax.servlet.Filter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shiro的配置文件
 *
 */
@Configuration
public class ShiroConfig implements ApplicationContextAware {
    private ApplicationContext applicationContext;
    @Value("${nezha.loginExpiration:30}")
    private Integer loginExpiration;

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private Integer redisPort;

    @Value("${spring.redis.timeout}")
    private String redisTimeout;

    @Value("${spring.redis.password}")
    private String redisPassword;

    @Value("${spring.redis.database}")
    private Integer redisDatabase;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Autowired
    private SysUserDao sysUserDao;

    /**
     * 单机环境，session交给shiro管理
     */
    /*@Bean
    @ConditionalOnProperty(prefix = "nezha", name = "cluster", havingValue = "false")
    public DefaultWebSessionManager sessionManager(@Value("${nezha.globalSessionTimeout:3600}") long globalSessionTimeout){
        DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
        sessionManager.setSessionValidationSchedulerEnabled(true);
        sessionManager.setSessionIdUrlRewritingEnabled(false);
        sessionManager.setSessionValidationInterval(globalSessionTimeout * 1000);
        sessionManager.setGlobalSessionTimeout(globalSessionTimeout * 1000);

        return sessionManager;
    }

    *//**
     * 集群环境，session交给spring-session管理
     *//*
    @Bean
    @ConditionalOnProperty(prefix = "nezha", name = "cluster", havingValue = "true")
    public ServletContainerSessionManager servletContainerSessionManager() {
        return new ServletContainerSessionManager();
    }

    @Bean("securityManager")
    public SecurityManager securityManager(UserRealm userRealm, SessionManager sessionManager) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(userRealm);
        securityManager.setSessionManager(sessionManager);
        securityManager.setRememberMeManager(null);

        return securityManager;
    }*/


    @Bean("shiroFilter")
    public ShiroFilterFactoryBean shiroFilter(SecurityManager securityManager) {
        ShiroFilterFactoryBean shiroFilter = new ShiroFilterFactoryBean();
        shiroFilter.setSecurityManager(securityManager);
        shiroFilter.setLoginUrl("/sys/login");
//        shiroFilter.setLoginUrl("/login.html");
        shiroFilter.setUnauthorizedUrl("/");

        Map<String, String> filterMap = new LinkedHashMap<>();
        filterMap.put("/metrics","anon");
        filterMap.put("/prometheus","anon");
        filterMap.put("/prom/**","anon");
        filterMap.put("/swagger/**", "anon");
        filterMap.put("/v2/api-docs", "anon");
        filterMap.put("/swagger-ui.html", "anon");
        filterMap.put("/webjars/**", "anon");
        filterMap.put("/swagger-resources/**", "anon");
//        filterMap.put("/statics/**", "anon");
        filterMap.put("/static/**","anon");
        filterMap.put("/nezha/**", "anon");
        filterMap.put("/", "anon");
        filterMap.put("/config.json", "anon");
        filterMap.put("/index.html", "anon");
        filterMap.put("/login.html", "anon");
        filterMap.put("/sys/login", "anon");
        filterMap.put("/healthy", "anon");
        filterMap.put("/buildInfo", "anon");
//        filterMap.put("/logout","anon");
        filterMap.put("/favicon.ico", "anon");
        filterMap.put("/captcha.jpg", "anon");
        filterMap.put("/api/**", "anon");
        filterMap.put("/terminal.ws","anon");
        filterMap.put("/terminal/monitor.ws","anon");
//        filterMap.put("/**", "authc");
        filterMap.put("/**", "tokenCheckFilter");
        Map<String, Filter> cumstomFilterMap=new LinkedHashMap<String,Filter>();
        TokenCheckFilter tokenCheckFilter = new TokenCheckFilter(redisTemplate, loginExpiration, sysUserDao);
        cumstomFilterMap.put("tokenCheckFilter",tokenCheckFilter);
        shiroFilter.setFilters(cumstomFilterMap);
        shiroFilter.setFilterChainDefinitionMap(filterMap);



        return shiroFilter;
    }
    //将这个bean移到单独的配置类中，不然无法自动注入bean
//    @Bean("lifecycleBeanPostProcessor")
//    public LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
//        return new LifecycleBeanPostProcessor();
//    }

    @Bean
    @ConditionalOnExpression("${nezha.shiroPermisSupport:true}")
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }
    @Bean(name={"tomcatServletWebServerFactory"})
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
     * 配置shiro redisManager
     * 使用的是shiro-redis开源插件
     *
     * @return
     */
    public RedisManager redisManager() {
        RedisManager redisManager = new RedisManager();
        redisManager.setHost(redisHost + ":" + redisPort);
        Integer timeout;
        if (StringUtils.isEmpty(redisTimeout)) {
            timeout = 6000;
        } else {
            Matcher matcher = Pattern.compile("[^0-9]").matcher(redisTimeout);
            timeout = Integer.valueOf(matcher.replaceAll("").trim());
        }
        redisManager.setTimeout(timeout);
        if (StringUtils.isNotEmpty(redisPassword)) {
            redisManager.setPassword(redisPassword);
        }
        redisManager.setDatabase(redisDatabase);
        return redisManager;
    }

    /**
     * cacheManager 缓存 redis实现
     * 使用的是shiro-redis开源插件
     *
     * @return
     */
    @Bean
    public RedisCacheManager cacheManager() {
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
     * RedisSessionDAO shiro sessionDao层的实现 通过redis
     * 使用的是shiro-redis开源插件
     */
    @Bean
    public RedisSessionDAO redisSessionDAO() {
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
        MySessionManager mySessionManager = new MySessionManager();
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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}