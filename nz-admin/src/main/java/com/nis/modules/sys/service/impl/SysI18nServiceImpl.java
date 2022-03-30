package com.nis.modules.sys.service.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.util.WebUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.config.I18nConfig;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.smartvalidate.utils.CommonUtil;
import com.nis.modules.sys.dao.SysI18nDao;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.entity.SysI18nEntity;
import com.nis.modules.sys.service.SysDictService;
import com.nis.modules.sys.service.SysI18nService;
import com.nis.modules.sys.service.SysUserService;
import com.nis.modules.sys.shiro.ShiroUtils;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

@Service("sysI18nService")
public class SysI18nServiceImpl extends ServiceImpl<SysI18nDao, SysI18nEntity> implements SysI18nService {

	@Autowired
	private Environment env;
	@Autowired
	private SysDictService dictService;
	@Autowired
	private I18nConfig i18nConfig;
	@Autowired
	private SysUserService sysUserService;
	@Autowired
	private SysDictService sysDictService;

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		String ids = (String) params.get("ids");
		if (StrUtil.isNotBlank(ids)) {
			List<String> listIds = Arrays.asList(ids.split(","));
			params.put("ids", listIds);
		}
		IPage<SysI18nEntity> page=this.page(new Query(SysI18nEntity.class).getPage(params));

		List<SysI18nEntity> list=this.baseMapper.selectSysI18nListWithPage(page,params);

		page.setRecords(list);
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
		return queryValue(code,getLocale());
	}
	
	@Override
	public String queryValue(String code, Object... param) {
		return queryValue(code,getLocale(),param);
	}
			
	public String queryValue(String code, String lang) {
		return queryValue(code,new Locale(lang));
	}
	
	public String queryValue(String code,Locale locale,Object...param) {
		return i18nConfig.getSourceFromCache(code, locale,param);
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
		// 根据sysdict判断是否支持当前lang类型
		List<SysDictEntity> langList = sysDictService.list(new QueryWrapper<SysDictEntity>().lambda().eq(SysDictEntity::getType, "lang"));
		if(ToolUtil.isEmpty(langList)) {
			throw new NZException(RCode.SYS_I18N_NOSUCHLANG);
		}else {
			List<String> collect = langList.stream().map(SysDictEntity::getValue).collect(Collectors.toList());
			if(!collect.contains(sysI18n.getLang())) {
				throw new NZException(RCode.SYS_I18N_NOSUCHLANG);
			}
		}
		
		boolean dumpFlag = true; //记录是否重复(code+lang)，true为重复
		List<SysI18nEntity> list = list(new QueryWrapper<SysI18nEntity>().lambda()
					.eq(true, SysI18nEntity::getCode, sysI18n.getCode())
					. eq(true, SysI18nEntity::getLang, sysI18n.getLang()).ne(ObjectUtil.isNotEmpty(sysI18n.getId()),SysI18nEntity::getId, sysI18n.getId()));
		//当只有一个结果并且这个结果是自身时，或没有结果时，说明不重复
		if ((list == null || list.size() == 0)) {
			dumpFlag = false;
		}
		return dumpFlag;
	}

	@Override
	public SysI18nEntity queryInfo(Long id) {
		SysI18nEntity sysI18n = this.baseMapper.selectById(id);
		if (ToolUtil.isNotEmpty(sysI18n)) {
			sysI18n.setUuser(sysUserService.getById(sysI18n.getUby()));
		}
		return sysI18n;
	}

	@Override
	public void saveI18n(SysI18nEntity sysI18nEntity) {
		this.validateData(sysI18nEntity);
		sysI18nEntity.setUtime(new Date());
		sysI18nEntity.setUby(ShiroUtils.getUserId());
		this.baseMapper.insert(sysI18nEntity);
	}

	@Override
	public void updateI18n(SysI18nEntity sysI18nEntity) {
		ValidateUtils.is(sysI18nEntity.getId()).notNull();
		this.validateData(sysI18nEntity);
		sysI18nEntity.setUtime(new Date());
		sysI18nEntity.setUby(ShiroUtils.getUserId());
		this.baseMapper.updateById(sysI18nEntity);
	}

	@Override
	public void validateData(SysI18nEntity sysI18nEntity) {
		ValidateUtils.is(sysI18nEntity.getCode()).notNull()
		 .and(sysI18nEntity.getLang()).notNull()
		 .and(sysI18nEntity.getValue()).notNull()
		 .and(sysI18nEntity.getName()).notNull();
		if(this.check(sysI18nEntity)) { //检查是否重复，true为重复
			throw new NZException(RCode.SYS_I18N_DUPLICATE);
    	}
	}
	
	@Override
	public Map selectLang(String l) {
		List<String> langs = null;
		if(ToolUtil.isEmpty(l)) {
			List<SysDictEntity> list = sysDictService.list(new QueryWrapper<SysDictEntity>().lambda().eq(SysDictEntity::getType, "lang"));
			if(ToolUtil.isNotEmpty(list)) {
				langs = list.stream().map(SysDictEntity::getValue).collect(Collectors.toList());
			}
		}else {
			langs = StrUtil.split(l, StrUtil.C_COMMA);
		}
		Map result =new HashMap();
		if(ToolUtil.isNotEmpty(langs)) {
			for(String lang:langs) {
				List<SysI18nEntity> datas = this.list(new QueryWrapper<SysI18nEntity>().lambda().eq(SysI18nEntity::getLang, lang));
				Map<String, String> collect = datas.stream().collect(Collectors.toMap(SysI18nEntity::getCode, SysI18nEntity::getValue));
				result.put(lang, collect);
			}
		}
		return result;
	}

	@Override
	public void clearCache() {
		i18nConfig.reload();
	}

	@Override
	public String queryValueByName(String name) {
		String language = getLocale().getLanguage();
		List<SysI18nEntity> list = this.list(new LambdaQueryWrapper<SysI18nEntity>().eq(SysI18nEntity::getName, name).eq(SysI18nEntity::getLang, language));
		if (Tool.CollectionUtil.isEmpty(list) || list.size() > 1) {
			return StrUtil.EMPTY;
		}
		SysI18nEntity sysI18nEntity = list.stream().findFirst().get();
		return sysI18nEntity.getValue();
	}
}
