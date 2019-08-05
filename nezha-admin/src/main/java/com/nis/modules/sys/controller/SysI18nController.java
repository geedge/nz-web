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
import com.nis.modules.sys.service.SysI18nService;
import com.nis.modules.sys.shiro.ShiroUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import com.nis.common.smartvalidate.ValidateUtils;
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

    /**
     * 列表
     */
    @RequestMapping(value = "list", method = RequestMethod.GET)
    @ApiOperation(httpMethod = "GET", value = "国际化信息列表")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = sysI18nService.queryPage(params);

        return R.ok().put("page", page);
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
    	boolean flag = sysI18nService.check(sysI18n); //检查是否重复，true为重复
    	if (flag) {
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
    	boolean flag = sysI18nService.check(sysI18n); //检查是否重复，true为重复
    	if (flag) {
    		return R.error(RCode.SYS_I18N_DUPLICATE);
    	} else {
	    	sysI18n.setOperator(ShiroUtils.getUserId());
	    	sysI18n.setOpTime(new Date());
			sysI18nService.updateById(sysI18n);
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
		sysI18nService.delete(Arrays.asList(ids));
		return R.ok();
    }

}
