package com.nis.modules.sys.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.sys.entity.SysI18nEntity;
import com.nis.modules.sys.service.SysI18nService;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;


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
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.I18N)
    public R save(@ApiParam @RequestBody SysI18nEntity sysI18n){
    	sysI18nService.saveI18n(sysI18n);
    	return R.ok();
    }

	/**
     * 修改
     */
    @PutMapping
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.I18N)
    public R update(@RequestBody SysI18nEntity sysI18n){
		sysI18nService.updateI18n(sysI18n);
		return R.ok();
    }

    /**
     * 删除
     */
    @DeleteMapping
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.I18N)
    public R delete(@RequestParam String ids){
    	ValidateUtils.is(ids).notNull();
    	sysI18nService.removeByIds(Arrays.asList(ids.split(",")));
		return R.ok();
    }
    
    @GetMapping("/lang")
    public R lang(@RequestParam(required = false) String l) {
    	Map result = sysI18nService.selectLang(l);
    	return R.ok(result);
    }
    
    /**
     * 清除缓存
     */
    @PutMapping("/clearCache")
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.I18N)
    public R clearCache(){
		sysI18nService.clearCache();
		return R.ok();
    }
    
}
