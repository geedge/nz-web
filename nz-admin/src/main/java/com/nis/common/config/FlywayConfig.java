package com.nis.common.config;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.nis.common.update.VersionUpdateInterface;
import com.nis.common.utils.Tool;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.log.Log;

@Configuration
@DependsOn(value = {"dataSourceConfig"})
public class FlywayConfig implements ApplicationContextAware{

    private static Log log = Log.get();

    @Autowired
    private DataSource dataSource;

    @Autowired
    private FlywayProperties flywayProperties;
    
    private Collection<VersionUpdateInterface> versionUpdateBeans;
    
    @PostConstruct
    public void migrate() {

        if (!flywayProperties.getEnabled()) {
            return;
        }

        Map<String, String> map = new HashMap<String, String>();
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
        	log.error("flyway执行脚本失败", e);
        }

        FluentConfiguration configure = Flyway.configure();
        configure.dataSource(dataSource);
        configure.configuration(map);
        Flyway flyway = configure.load();
        try {
            flyway.migrate();
        } catch (FlywayException e) {
            flyway.repair();
            log.error("Flyway migrate error,shutdown service{}", e);
            // 停止项目运行
            System.exit(1);
        }
        
        if(Tool.CollUtil.isNotEmpty(versionUpdateBeans)) {
        	versionUpdateBeans.forEach((vu)->{
        		//执行update 操作
        		log.debug("version update start,name: {}",vu.getClass().getSimpleName());
        		vu.update();
        		log.debug("version update end,name: {}",vu.getClass().getSimpleName());
        	});
        }
    }

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		//根据接口类型返回相应的所有bean
		Map<String,VersionUpdateInterface> map = applicationContext.getBeansOfType(VersionUpdateInterface.class);
		versionUpdateBeans = map.values();
		log.debug("version update size: {}",Tool.CollUtil.size(versionUpdateBeans));
		if(Tool.CollUtil.isNotEmpty(versionUpdateBeans)) {
			//按照 class name 排序
			versionUpdateBeans = Tool.CollUtil.sort(versionUpdateBeans, (v1,v2)->{
				String name1 = v1.getClass().getSimpleName();
				String name2 = v2.getClass().getSimpleName();
				return name1.compareTo(name2);
			});
		}
	}
}
