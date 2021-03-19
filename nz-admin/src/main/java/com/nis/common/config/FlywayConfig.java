package com.nis.common.config;

import cn.hutool.core.util.ReflectUtil;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class FlywayConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private FlywayProperties flywayProperties;

    @PostConstruct
    public void migrate() {

        if (!flywayProperties.getEnabled()) {
            return;
        }

        Map<String, String> map = new HashMap();
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(flywayProperties.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor property : propertyDescriptors) {
                String key = property.getName();
                if (!"class".equals(key) && !"enabled".equals(key)) {
                    //Method getter = property.getReadMethod();
                    //Object value = getter.invoke(flywayProperties);
                    Object value = ReflectUtil.invoke(flywayProperties, property.getReadMethod(), key);
                    map.put("flyway." + key, value.toString());
                }
            }
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }

        FluentConfiguration configure = Flyway.configure();
        configure.dataSource(dataSource);
        configure.configuration(map);
        Flyway flyway = configure.load();
        try {
            flyway.migrate();
        } catch (FlywayException e) {
            flyway.repair();
            log.error("flyway执行脚本失败，错误信息是：" + e.getMessage(), e);
        }
    }
}
