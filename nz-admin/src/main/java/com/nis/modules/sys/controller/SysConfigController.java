/**

 *

 *

 */

package com.nis.modules.sys.controller;


import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.validator.ValidatorUtils;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * 系统配置信息
 *

 */
@RestController
@RequestMapping("/sysConfig")
public class SysConfigController extends AbstractController {
	@Autowired
	private SysConfigService sysConfigService;
	
	/**
	 * 所有配置列表
	 */
	@RequestMapping("/list")
	@RequiresPermissions("sys:config:list")
	public R list(@RequestParam Map<String, Object> params){
		PageUtils page = sysConfigService.queryPage(params);

		return R.ok().put("page", page);
	}
	
	
	/**
	 * 配置信息
	 */
	@RequestMapping("/info/{id}")
	@RequiresPermissions("sys:config:info")
	@ResponseBody
	public R info(@PathVariable("id") Long id){
		SysConfigEntity config = sysConfigService.getById(id);
		
		return R.ok().put("config", config);
	}
	
	/**
	 * 保存配置
	 */
	@SysLog("保存配置")
	@RequestMapping("/save")
	@RequiresPermissions("sys:config:save")
	public R save(@RequestBody SysConfigEntity config){
		ValidatorUtils.validateEntity(config);

		sysConfigService.saveConfig(config);
		
		return R.ok();
	}
	
	/**
	 * 修改配置
	 */
	@SysLog("修改配置")
	@RequestMapping("/update")
	@RequiresPermissions("sys:config:update")
	public R update(@RequestBody SysConfigEntity config){
		ValidatorUtils.validateEntity(config);
		
		sysConfigService.update(config);
		
		return R.ok();
	}
	
	/**
	 * 删除配置
	 */
	@SysLog("删除配置")
	@RequestMapping("/delete")
	@RequiresPermissions("sys:config:delete")
	public R delete(@RequestBody Long[] ids){
		sysConfigService.deleteBatch(ids);
		
		return R.ok();
	}

    /**
     * 根据 系统设置类别分类获取包含的设置数据
     * @param type
     * @param paramKey
     * @return
     */
    @GetMapping
    public R getConfigList(String[] type, String[] paramKey) {
    	// 查询参数不能同时为空
		if((type == null || type.length == 0) && (paramKey == null || paramKey.length == 0)){
			throw new NZException(RCode.SYS_CONFIG_QUERYPARAMS_ISNULL);
		}

		if(type != null && type.length > 0){
			// 校验type值是否在枚举值内
			Set<String> configTypes = Constant.SYSCONFIG_TYPEWITHKEYS_MAP.keySet();
			for (String t : type) {
				if (!configTypes.contains(t)) {
					throw new NZException(RCode.SYS_CONFIG_TYPE_INVALIDE);
				}
			}
		}

        Map resultData = sysConfigService.getConfigListByTypes(type, paramKey);

        return R.ok(resultData);
    }


    /**
     *
     * 保存配置
     * @param param
     * @return
     */
    @PutMapping
    public R updateConfig(@RequestBody Map<String, Object> param) {
        ValidateUtils.is(param).notNull(RCode.SYS_CONFIG_SAVEENTITY_ISNULL);

        // 校验type值是否在枚举值内
        Set<String> configTypes = Constant.SYSCONFIG_TYPEWITHKEYS_MAP.keySet();

        Set<String> types = param.keySet();
        for (String type : types) {
            if (!configTypes.contains(type)) {
                throw new NZException(RCode.SYS_CONFIG_TYPE_INVALIDE);
            }
        }

		R result = sysConfigService.updateConfigs(param);

		return result;
    }

	/**
	 * 系统重置
	 * @param password
	 * @param type
	 * @return
	 */
	@DeleteMapping("/reset")
	public R resetConfig(String password,String ...type) throws Exception{
		try {
			sysConfigService.resetConfig(password,type);
		}catch (Exception e){
			throw e;
		}
    	return R.ok();
	}
}
