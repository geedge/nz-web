package com.nis.common.config;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import com.nis.common.smartvalidate.utils.CommonUtil;

@Configuration
@EnableAutoConfiguration
@ComponentScan
public class LocaleConfig implements WebMvcConfigurer {

	@Autowired
	private Environment env;
	
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver slr = new SessionLocaleResolver();
        // 默认语言
        String defaultLang = env.getProperty("lang.default", "en_US");
        slr.setDefaultLocale(new Locale(defaultLang));
        return slr;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
    	MyI18nInterceptor lci = new MyI18nInterceptor();
        // 参数名
        lci.setParamName("_lang");
        return lci;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}

class MyI18nInterceptor extends LocaleChangeInterceptor {
	
	private boolean checkHttpMethod(String currentMethod) {
		String[] configuredMethods = getHttpMethods();
		if (ObjectUtils.isEmpty(configuredMethods)) {
			return true;
		}
		for (String configuredMethod : configuredMethods) {
			if (configuredMethod.equalsIgnoreCase(currentMethod)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		String newLocale = request.getParameter(getParamName());
		if (newLocale != null) {
			if (checkHttpMethod(request.getMethod())) {
				LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
				if (localeResolver == null) {
					throw new IllegalStateException("找不到LocaleResolver");
				}
				try {
					localeResolver.setLocale(request, response, parseLocaleValue(newLocale));
					// 若语言和session中记录的值不同了，刷新session中的值
					String currLang = (String) request.getSession().getAttribute(getParamName());
					if (CommonUtil.isNull(currLang) || !currLang.equals(newLocale)) {
						request.getSession().setAttribute(getParamName(), newLocale);
					}
				}
				catch (IllegalArgumentException ex) {
					if (isIgnoreInvalidLocale()) {
						logger.debug("忽略无效的locale值 [" + newLocale + "]: " + ex.getMessage());
					}
					else {
						throw ex;
					}
				}
			}
		}
		return true;
	}
}