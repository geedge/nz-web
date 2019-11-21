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
 * @author song
 * @email 
 * @date 2019-08-05 14:35:21
 */
public interface SysI18nService extends IService<SysI18nEntity> {

    PageUtils queryPage(Map<String, Object> params);

	boolean check(SysI18nEntity sysI18n);

	String queryValue(String code);
	
	public String queryValue(String code, String lang);
	
	String queryValue(String code, Locale locale);
	
	List<SysI18nEntity> queryAll();
	
	Map<String, String> getCurrLang();
	
	Locale getLocale();
}

