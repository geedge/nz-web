package com.nis.modules.sys.controller;

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.modules.sys.entity.SysI18nEntity;
import com.nis.modules.sys.service.SysI18nService;

import io.swagger.annotations.ApiParam;


/**
 * i18n
 */
@RestController
@RequestMapping("sys/i18n")
public class SysI18nController {
	
    @Autowired
    private SysI18nService sysI18nService;
    
    /**
     * 列表
     */
    @GetMapping
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = sysI18nService.queryPage(params);
        return R.ok(page);
    }

    @GetMapping("/{id}")
    public R detail(@PathVariable("id")Long id) {
    	ValidateUtils.is(id).notNull();
    	SysI18nEntity byId = sysI18nService.queryInfo(id);
    	return R.ok(byId);
    }
    
    /**
     * 保存
     */
    @PostMapping
    public R save(@ApiParam @RequestBody SysI18nEntity sysI18n){
    	sysI18nService.saveI18n(sysI18n);
    	return R.ok();
    }

	/**
     * 修改
     */
    @PutMapping
    public R update(@RequestBody SysI18nEntity sysI18n){
		sysI18nService.updateI18n(sysI18n);
		return R.ok();
    }

    /**
     * 删除
     */
    @DeleteMapping
    public R delete(@RequestParam String ids){
    	ValidateUtils.is(ids).notNull();
    	sysI18nService.removeByIds(Arrays.asList(ids.split(",")));
		return R.ok();
    }
    
    @GetMapping("/lang")
    public R lang(@RequestParam String l) {
    	ValidateUtils.is(l).notNull();
    	Map result = sysI18nService.selectLang(l);
    	return R.ok(result);
    }
}
