/**

 *

 *

 */

package com.nis.modules.sys.controller;


import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.common.validator.ValidatorUtils;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYS_CONF)
	public R list(@RequestParam Map<String, Object> params){
		PageUtils page = sysConfigService.queryPage(params);

		return R.ok().put("page", page);
	}


	/**
	 * 配置信息
	 */
	@RequestMapping("/info/{id}")
	@ResponseBody
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYS_CONF)
	public R info(@PathVariable("id") Long id){
		SysConfigEntity config = sysConfigService.getById(id);
		return R.ok().put("config", config);
	}

	/**
	 * 保存配置
	 */
	@RequestMapping("/save")
	@SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.SYS_CONF)
	public R save(@RequestBody SysConfigEntity config){
		ValidatorUtils.validateEntity(config);

		sysConfigService.saveConfig(config);

		return R.ok();
	}

	/**
	 * 修改配置
	 */
	@RequestMapping("/update")
	@SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.SYS_CONF)
	public R update(@RequestBody SysConfigEntity config){
		ValidatorUtils.validateEntity(config);

		sysConfigService.update(config);

		return R.ok();
	}

	/**
	 * 删除配置
	 */
	@SysLog(operation = OperationEnum.DELETE, type = TypeEnum.SYS_CONF)
	@RequestMapping("/delete")
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
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.SYS_CONF)
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

        // 返回前端界面结果中 去除ldap密码
        Map ldaps = (Map) resultData.get("ldap");
        if(ObjectUtils.isNotEmpty(ldaps)) {
        	ldaps.remove("ldap_password");
        }

        return R.ok(resultData);
    }


    /**
     *
     * 保存配置
     * @param param
     * @return
     */
    @PutMapping
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.SYS_CONF)
    @RequiresPermissions({"sys:config:update"})
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
	 * @param param
	 * @return
	 */
	@PutMapping("/reset")
	@SysLog(operation = OperationEnum.RESET,type = TypeEnum.SYS_CONF)
	@RequiresPermissions({"sys:config:reset"})
	public R resetConfig(@RequestBody Map<String, Object> param) throws IOException,RuntimeException{
		try {
			sysConfigService.resetConfig(param);
		}catch (IOException | RuntimeException e){
			throw e;
		}
    	return R.ok();
	}
}
