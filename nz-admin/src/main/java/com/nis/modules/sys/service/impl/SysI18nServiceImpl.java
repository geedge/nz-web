package com.nis.modules.sys.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.config.I18nConfig;
import com.nis.common.smartvalidate.utils.CommonUtil;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.sys.dao.SysI18nDao;
import com.nis.modules.sys.entity.SysI18nEntity;
import com.nis.modules.sys.service.SysDictService;
import com.nis.modules.sys.service.SysI18nService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;


@Service("sysI18nService")
public class SysI18nServiceImpl extends ServiceImpl<SysI18nDao, SysI18nEntity> implements SysI18nService {

	@Autowired
	private Environment env;
	@Autowired
    private SysDictService dictService;
	@Autowired
    private I18nConfig i18nConfig;
	
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SysI18nEntity> page = this.page(
                new Query<SysI18nEntity>().getPage(params),
                new QueryWrapper<SysI18nEntity>()
                	.like(!StringUtils.isEmpty((String)params.get("code")), "code", (String)params.get("code"))
                	.eq(!StringUtils.isEmpty((String)params.get("lang")), "lang", (String)params.get("lang"))
                	.like(!StringUtils.isEmpty((String)params.get("value")), "value", (String)params.get("value"))
        );

        return new PageUtils(page);
    }

    public List<SysI18nEntity> queryAll() {
    	QueryWrapper<SysI18nEntity> qw = new QueryWrapper<>();
    	List<SysI18nEntity> list = list(qw);
    	return list;
    }
    
    public Locale getLocale() {
    	HttpServletRequest request = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
    	//从session中获取lang
    	Locale locale = (Locale) WebUtils.getSessionAttribute(request, SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME);
    	if (locale == null) {
    		String defaultLang = env.getProperty("default.locale", "en_US");
            String[] split = defaultLang.split("_");
            locale = new Locale(split[0], split[1]);
    	}
    	return locale;
    }
    
    /** 
     * 根据code查询国际化值，自动获取当前语言
     */
    public String queryValue(String code) {
    	return queryValue(code, getLocale());
    }
    
    public String queryValue(String code, String lang) {
    	return queryValue(code, new Locale(lang));
    }
    
    public String queryValue(String code, Locale locale) {
    	return i18nConfig.getSourceFromCache(code, locale);
    }
    
    public Map<String, String> getCurrLang() {
    	String lang = getLocale().getLanguage();
    	if (CommonUtil.isNull(Constant.langList)) {
			dictService.updateLangCache();
		}
    	for (Map<String, String> l : Constant.langList){
    		if (l.get("value").equals(lang)) {
    			return l;
    		}
    	}
    	return null;
    }
    
	@Override
	public boolean check(SysI18nEntity sysI18n) {
		boolean dumpFlag = true; //记录是否重复(code+lang)，true为重复
		List<SysI18nEntity> list = list(new QueryWrapper<SysI18nEntity>()
									.eq(true, "code", sysI18n.getCode())
									. eq(true, "lang", sysI18n.getLang())
									. eq(true, "del_flag", Constant.DELFLAG_FALSE));
		//当只有一个结果并且这个结果是自身时，或没有结果时，说明不重复
		if ((list == null || list.size() == 0) ||
				(list != null && list.size() == 1 && list.get(0).getId().longValue() == sysI18n.getId().longValue())) {
			dumpFlag = false;
		}
		return dumpFlag;
	}
}