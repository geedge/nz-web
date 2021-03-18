package com.nis.setup.register;

import cn.hutool.core.util.StrUtil;
import com.nis.setup.util.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;

import java.io.File;
import java.util.Properties;

public class SetupAutoConfiguration implements ImportBeanDefinitionRegistrar , EnvironmentAware {
    private Environment environment;
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        String inited = environment.getProperty("nezha.inited");

        if(!StrUtil.isEmpty(inited)&& Integer.parseInt(inited) > 0){
            ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry, true);
            scanner.scan("com.nis.modules","com.nis.common");
        }

    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
