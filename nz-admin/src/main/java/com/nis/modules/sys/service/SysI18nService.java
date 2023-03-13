package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.sys.entity.SysI18nEntity;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * i18n信息表
 *
 */
public interface SysI18nService extends IService<SysI18nEntity> {

	PageUtils queryPage(Map<String, Object> params);

	SysI18nEntity queryInfo(Long id);
	
	void saveI18n(SysI18nEntity sysI18nEntity);
	
	void updateI18n(SysI18nEntity sysI18nEntity);
	
	void validateData(SysI18nEntity sysI18nEntity);
	
	Map selectLang(String l);

	boolean check(SysI18nEntity sysI18n);

	String queryValue(String code);
	
	String queryValue(String code, Object...param);
	
	public String queryValue(String code, String lang);
	
	String queryValue(String code, Locale locale,Object...param);
	
	List<SysI18nEntity> queryAll();
	
	Map<String, String> getCurrLang();
	
	Locale getLocale();
	
	void clearCache();

	String queryValueByName(String name);
}

