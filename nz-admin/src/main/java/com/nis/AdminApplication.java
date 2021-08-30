
package com.nis;

import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.nis.AdminApplication.SetupAutoConfiguration;
import com.nis.modules.terminal.config.TerminalHandler;

import cn.hutool.core.util.StrUtil;
import io.micrometer.core.instrument.MeterRegistry;

@SpringBootApplication(scanBasePackages = { "com.nis.setup" })
@Import(SetupAutoConfiguration.class)
@PropertySource(value = { "file:./config/nezha.properties","classpath:./config/nezha.properties" }, encoding = "utf-8", ignoreResourceNotFound = true)
public class AdminApplication {

	public static void main(String[] args) {
		// 默认为 UTC 时区
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		ConfigurableApplicationContext run = SpringApplication.run(AdminApplication.class, args);
		TerminalHandler.setApplicationContext(run);
	}

	/*@Bean
	MeterRegistryCustomizer<MeterRegistry> configurer(@Value("Nezha") String applicationName) {
		return (registry) -> registry.config().commonTags("application", applicationName);
	}*/

	static class SetupAutoConfiguration implements ImportBeanDefinitionRegistrar, EnvironmentAware {
		private Environment environment;
		
		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			String inited = environment.getProperty("nezha.inited");
			if (!StrUtil.isEmpty(inited) && Integer.parseInt(inited) > 0) {
				ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry, true);
				scanner.scan("com.nis.modules", "com.nis.common");
			}
		}
		
		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}
	}

	@Bean
	public FilterRegistrationBean<CorsFilter> corsFilter() {
		final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		final CorsConfiguration config = new CorsConfiguration();
		// 允许cookies跨域
		config.setAllowCredentials(true);
		// #允许向该服务器提交请求的URI，*表示全部允许，在SpringMVC中，如果设成*，会自动转成当前请求头中的Origin
		config.addAllowedOrigin("*");
		// #允许访问的头信息,*表示全部
		config.addAllowedHeader("*");
		// 预检请求的缓存时间（秒），即在这个时间段里，对于相同的跨域请求不会再预检了
		config.setMaxAge(18000L);
		// 允许提交请求的方法，*表示全部允许
		config.addAllowedMethod("OPTIONS");
		config.addAllowedMethod("HEAD");
		config.addAllowedMethod("GET");
		config.addAllowedMethod("PUT");
		config.addAllowedMethod("POST");
		config.addAllowedMethod("DELETE");
		config.addAllowedMethod("PATCH");
		source.registerCorsConfiguration("/**", config);
		FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<CorsFilter>(new CorsFilter(source));
		// 设置监听器的优先级
		bean.setOrder(0);
		return bean;
	}
}
