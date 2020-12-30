package com.nis.common.config;

import cn.hutool.log.Log;
import com.nis.common.smartvalidate.utils.CommonUtil;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

@Configuration
@EnableAutoConfiguration
@ComponentScan
public class LocaleConfig implements WebMvcConfigurer {

    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver slr = new SessionLocaleResolver();
        // 默认语言
        slr.setDefaultLocale(new Locale("en"));
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
	private Log log = Log.get();
	private boolean validateHttpMethod(String currentMethod) {
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
			if (validateHttpMethod(request.getMethod())) {
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
						log.debug("忽略无效的locale值 [" + newLocale + "]: " + ex);
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