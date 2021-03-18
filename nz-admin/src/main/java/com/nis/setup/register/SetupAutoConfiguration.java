package com.nis.setup.register;

import cn.hutool.core.util.StrUtil;
import com.nis.setup.util.Utils;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;

import java.io.File;
import java.util.Properties;

public class SetupAutoConfiguration implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Resource resource = Utils.getResource(null);
        YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
        // 2:将加载的配置文件交给 YamlPropertiesFactoryBean
        yamlPropertiesFactoryBean.setResources(resource);
        // 3：将yml转换成 key：val
        Properties properties = yamlPropertiesFactoryBean.getObject();
        String inited = null;
        if (properties != null) {
            inited = properties.getProperty("nezha.inited");
        }
        if(!StrUtil.isEmpty(inited)&& Integer.parseInt(inited) > 0){
            ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry, true);
            scanner.scan("com.nis.modules","com.nis.common");
        }

    }
}
