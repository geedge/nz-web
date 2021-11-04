package com.nis.common.config;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.nis.common.smartvalidate.utils.CommonUtil;
import com.nis.common.utils.Constant;
import com.nis.modules.sys.entity.SysI18nEntity;
import com.nis.modules.sys.service.SysDictService;
import com.nis.modules.sys.service.SysI18nService;

import cn.hutool.log.Log;

@Service("messageSource")
public class I18nConfig extends AbstractMessageSource implements ResourceLoaderAware {

    private ResourceLoader resourceLoader;

    @Autowired
    SysI18nService sysI18nService;
    @Autowired
    SysDictService sysDictService;

    private final Log logger = Log.get();
    
    /**
     * 国际化缓存 
     */
    public static final Map<String, Map<String, String>> I18N_CACHE = new HashMap<>();
    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        this.reload();
    }

    /**
     * 重新将数据库中的国际化配置加载
     */
    public void reload() {
        I18N_CACHE.clear();
    }

    /**
     * 从数据库中获取所有国际化配置
     */
    public void loadAllMessageResourcesFromDB() {
        List<SysI18nEntity> list = sysI18nService.queryAll();
        if (!CommonUtil.isNull(list)) {
			if (CommonUtil.isNull(Constant.langList)) { //语言种类列表
				sysDictService.updateLangCache();
			}
        	try {
				for (Map<String, String> l : Constant.langList) {
					Map<String, String> langMap = I18N_CACHE.get(l.get("value"));
					if (CommonUtil.isNull(langMap)) {
						langMap = new HashMap<String, String>();
						I18N_CACHE.put(l.get("value"), langMap);
					}
					for (SysI18nEntity s : list) {
						if (l.get("value").equals(s.getLang())) {
							langMap.put(s.getCode(), s.getValue());
						}
					}
				}
			} catch (Exception e) {
				logger.error(e);
			}
        }
    }

    /**
     * 从缓存中取出国际化配置对应的数据 或者从父级获取
     *
     * @param code
     * @param locale
     * @return
     */
    public String getSourceFromCache(String code, Locale locale,Object... param) {
        String language = locale.getLanguage();
        if (CommonUtil.isNull(I18N_CACHE)) {
        	loadAllMessageResourcesFromDB();
        }
        Map<String, String> props = I18N_CACHE.get(language);
        if (null != props && props.containsKey(code)) {
        	String msg = props.get(code);
            return MessageFormat.format(msg, param);
        } else {
        	try {
                if (null != this.getParentMessageSource()) {
                    return this.getParentMessageSource().getMessage(code, param, locale);
                }
			} catch (Exception ex) {
				logger.error(ex);
			}
            return code;
        }
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = (resourceLoader == null ? new DefaultResourceLoader() : resourceLoader);
    }

    @Override
    protected MessageFormat resolveCode(String code, Locale locale) {
        String msg = getSourceFromCache(code, locale);
        MessageFormat messageFormat = new MessageFormat(msg, locale);
        return messageFormat;
    }

    @Override
    protected String resolveCodeWithoutArguments(String code, Locale locale) {
        return getSourceFromCache(code, locale);
    }
}