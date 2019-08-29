package com.nis.modules.sys.controller;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nis.modules.sys.entity.SysI18nEntity;
import com.nis.modules.sys.service.SysDictService;
import com.nis.modules.sys.service.SysI18nService;
import com.nis.modules.sys.shiro.ShiroUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.smartvalidate.utils.CommonUtil;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;



/**
 * i18n
 * @author song
 * @date 2019-08-05 14:35:21
 */
@Api(tags="国际化")
@RestController
@RequestMapping("sys/i18n")
public class SysI18nController {
    @Autowired
    private SysI18nService sysI18nService;
    @Autowired
    private SysDictService dictService;
    
    /**
     * 列表
     */
    @RequestMapping(value = "list", method = RequestMethod.GET)
    @ApiOperation(httpMethod = "GET", value = "国际化信息列表")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = sysI18nService.queryPage(params);

        return R.ok(page);
    }

    @RequestMapping(value = "detail", method = RequestMethod.GET)
    @ApiOperation(httpMethod = "GET", value = "国际化详情")
    public R detail(Long id) {
    	ValidateUtils.is(id).notNull();
    	SysI18nEntity byId = sysI18nService.getById(id);
    	return R.ok(byId);
    }
    
    /**
     * 保存
     */
    @RequestMapping(value = "save", method = RequestMethod.POST)
    @ApiOperation(httpMethod = "POST", value = "国际化信息新增")
    public R save(@ApiParam @RequestBody SysI18nEntity sysI18n){
    	ValidateUtils.is(sysI18n.getCode()).notNull()
    				 .and(sysI18n.getLang()).notNull()
    				 .and(sysI18n.getValue()).notNull();
    	if (!checkLang(sysI18n.getLang())) { //检查lang是否合法
    		return R.error(RCode.SYS_I18N_NOSUCHLANG);
    	}
    	
    	if (sysI18nService.check(sysI18n)) { //检查是否重复，true为重复
    		return R.error(RCode.SYS_I18N_DUPLICATE);
    	} else {
    		sysI18n.setOperator(ShiroUtils.getUserId());
    		sysI18n.setOpTime(new Date());
    		sysI18nService.save(sysI18n);
    		return R.ok();
    	}
    }

	/**
     * 修改
     */
    @RequestMapping(value = "update", method = RequestMethod.PUT)
    @ApiOperation(httpMethod = "PUT", value = "国际化信息修改")
    public R update(@RequestBody SysI18nEntity sysI18n){
    	ValidateUtils.is(sysI18n.getCode()).notNull()
					 .and(sysI18n.getLang()).notNull()
					 .and(sysI18n.getValue()).notNull()
					 .and(sysI18n.getId()).notNull();
    	if (!checkLang(sysI18n.getLang())) { //检查lang是否合法
    		return R.error(RCode.SYS_I18N_NOSUCHLANG);
    	}
    	if (sysI18nService.check(sysI18n)) { //检查是否重复，true为重复
    		return R.error(RCode.SYS_I18N_DUPLICATE);
    	} else {
	    	sysI18n.setOperator(ShiroUtils.getUserId());
	    	sysI18n.setOpTime(new Date());
			sysI18nService.updateById(sysI18n);
			// TODO 刷新缓存
			
			return R.ok();
    	}
    }

    /**
     * 删除
     */
    @RequestMapping(value = "delete", method = RequestMethod.DELETE)
	@ApiOperation(httpMethod = "DELETE", response = R.class, value = "删除国际化信息")
    public R delete(@ApiParam @RequestBody Long[] ids){
    	ValidateUtils.is(ids).notNull();
    	sysI18nService.removeByIds(Arrays.asList(ids));
		return R.ok();
    }

    /**
     * 重复校验，lang+code的组合不能重复
     * @param params
     * @return
     */
    @RequestMapping(value = "check", method = RequestMethod.GET)
    @ApiOperation(httpMethod = "GET", response = R.class, value = "校验重复记录")
    public R check(@RequestBody SysI18nEntity sysI18n) {
    	ValidateUtils.is(sysI18n.getCode()).notNull()
    				.and(sysI18n.getLang()).notNull();
    	if (!checkLang(sysI18n.getLang())) { //检查lang是否合法
    		return R.error(RCode.SYS_I18N_NOSUCHLANG);
    	}
    	if (sysI18nService.check(sysI18n)) {
    		return R.error(RCode.SYS_I18N_DUPLICATE);
    	} else {
    		return R.ok();
    	}
    }
    
    @RequestMapping(value = "getLangList", method = RequestMethod.GET)
    @ApiOperation(httpMethod = "GET", response = R.class, value = "获取语言列表")
    public R getLangList() {
    	if (CommonUtil.isNull(Constant.langList)) {
			dictService.updateLangCache();
		}
    	return R.ok(Constant.langList);
    }
    
    /**
     * 校验lang是否合法
     * @param lang
     * @return
     */
    private boolean checkLang(String lang) {
		if (CommonUtil.isNull(Constant.langList)) {
			dictService.updateLangCache();
		}
		boolean contains = Constant.langList.contains(lang);
		return contains;
	}
    
    public static void main(String[] args) {
    	String lang = "rzhu";
		String reg = "^[z][h]$|^[c][n]$|^[r][u]$";
		ValidateUtils.is(lang).regex(reg);
		
				
    }
}
