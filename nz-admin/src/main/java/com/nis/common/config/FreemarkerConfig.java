/**

 *

 *

 */

package com.nis.common.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import com.nis.common.utils.Tool;
import com.nis.modules.sys.shiro.ShiroTag;

import cn.hutool.log.Log;

/**
 * Freemarker配置
 *
 * 
 */
@Configuration
public class FreemarkerConfig {
	private static Log log = Log.get();

	@Value("${nezha.ui.path:static}")
	private String uiPath;

	@Bean
	public FreeMarkerConfigurer freeMarkerConfigurer(ShiroTag shiroTag) {
		FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
		boolean exist = Tool.FileUtil.exist(uiPath);
		uiPath = exist ? uiPath : "classpath:" + uiPath;
		log.info("static file path : {} ", uiPath);
		configurer.setTemplateLoaderPath(uiPath);
		Map<String, Object> variables = new HashMap<>(1);
		variables.put("shiro", shiroTag);
		configurer.setFreemarkerVariables(variables);

		Properties settings = new Properties();
		settings.setProperty("default_encoding", "utf-8");
		settings.setProperty("number_format", "0.##");
		configurer.setFreemarkerSettings(settings);
		return configurer;
	}

}
