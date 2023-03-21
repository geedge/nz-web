package com.nis.common.config;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.log.Log;
import com.nis.common.utils.Tool;
import org.apache.commons.lang3.time.StopWatch;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@DependsOn(value = {"dataSourceConfig"})
public class FlywayConfig implements ApplicationContextAware {

    private static final Log log = Log.get();

    private List<Callback> migrateCallbackList;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private FlywayProperties flywayProperties;

    @PostConstruct
    public void migrate() {
        log.info("[FlywayConfig] [migrate] [enable: {}]", flywayProperties.getEnabled());
        if (!flywayProperties.getEnabled()) {
            log.warn("[FlywayConfig] [migrate] [not enabled, do not execute flyway migrate]");
            return;
        }

        // build Fluent Configuration
        FluentConfiguration configure = this.buildFluentConfiguration();

        log.info("[FlywayConfig] [flyway migrate] [begin]");
        StopWatch sw = new StopWatch();
        sw.start();
        Flyway flyway = configure.load();
        try {
            flyway.migrate();
        } catch (Exception e) {
            log.error(e, "[FlywayConfig] [migrate error.]");

            // repair
            flyway.repair();

            // 停止服务,避免 systemd 服务重启机制,覆盖 flyway
            Tool.RuntimeUtil.exec("systemctl stop nz-web.service");
        } finally {
            // stop
            sw.stop();
        }
        log.info("[FlywayConfig] [flyway migrate] [finshed] [Run time: {}]", sw.toString());
    }

    /**
     * build Fluent Configuration
     *
     * @return A FluentConfiguration Object
     */
    private FluentConfiguration buildFluentConfiguration() {
        log.info("[buildFluentConfiguration] [begin]");

        Map<String, String> configMap = Tool.MapUtil.newHashMap();
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(flywayProperties.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor property : propertyDescriptors) {
                String key = property.getName();
                if (!"class".equals(key) && !"enabled".equals(key)) {
                    Object value = ReflectUtil.invoke(flywayProperties, property.getReadMethod(), key);
                    configMap.put("flyway." + key, value.toString());
                }
            }
        } catch (IntrospectionException e) {
            log.error(e, "[buildFluentConfiguration] [error]");
        }

        FluentConfiguration configure = Flyway.configure();
        configure.dataSource(dataSource);
        configure.configuration(configMap);

        // callbacks
        log.info("[buildFluentConfiguration] [migrate callbacks List] [size: {}]", migrateCallbackList.size());
        if (Tool.CollUtil.isNotEmpty(migrateCallbackList)) {
            log.info("[buildFluentConfiguration] [configure migrate callbacks]");
            configure.callbacks(migrateCallbackList.stream().toArray(Callback[]::new));
        }

        log.info("[buildFluentConfiguration] [finshed]");
        return configure;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // org.flywaydb.core.api.callback;
        Map<String, Callback> callbackMap = applicationContext.getBeansOfType(Callback.class);
        callbackMap = Tool.MapUtil.defaultIfEmpty(callbackMap, Tool.MapUtil.newHashMap());

        List<Callback> callbackList = callbackMap.values().stream().collect(Collectors.toList());
        migrateCallbackList = Tool.CollUtil.defaultIfEmpty(callbackList, Tool.ListUtil.list(true));
        if (log.isDebugEnabled()) {
            log.debug("[FlywayConfig] [Migrate Callback List] [size: {}]", migrateCallbackList.size());
        }
    }
}
