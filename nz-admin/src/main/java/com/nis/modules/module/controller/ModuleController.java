package com.nis.modules.module.controller;

import com.alibaba.fastjson.JSONObject;
import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.StringUtils;
import com.nis.modules.module.entity.Module;
import com.nis.modules.module.service.ModuleService;
import com.nis.modules.sys.controller.AbstractController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/module")
public class ModuleController extends AbstractController {
	
	@Autowired
	private ModuleService moduleService;
	
	@GetMapping
	public R list(@RequestParam Map<String, Object> params){
		PageUtils page = moduleService.queryPage(params);
		return R.ok(page);
	}
	
	@PostMapping
	@SysLog("save module")
	public R save(@RequestBody Module moduleEntity) {
		moduleService.validateParam(moduleEntity);

		// 处理snmYml字段
		String snmpParam = moduleEntity.getSnmpParam();
		if(StringUtils.isNotEmpty(snmpParam)){
			String snmpYml = moduleService.generateYmlFromSnmpParam(JSONObject.parseObject(snmpParam, Map.class), moduleEntity);
			moduleEntity.setSnmpYml(snmpYml);
		}
		moduleService.save(moduleEntity);
		return R.ok();
	}
	
	@PutMapping
	@SysLog("update module")
	public R modify(@RequestBody Module moduleEntity) {
		ValidateUtils.is(moduleEntity.getId()).notNull(RCode.MODULE_ID_ISNULL);
		moduleService.validateBulitinRule(Arrays.asList(moduleEntity.getId()));
		moduleService.validateParam(moduleEntity);

		// 处理snmYml字段 , 如果module名称发生改动 并且 snmpParam不为空则重新生成snmpYml字段
		Module module = moduleService.getById(moduleEntity.getId());
		String snmpParam = moduleEntity.getSnmpParam();
		if(StringUtils.isNotEmpty(snmpParam) || !moduleEntity.getName().equals(module.getName())){
			String snmpYml = moduleService.generateYmlFromSnmpParam(JSONObject.parseObject(snmpParam, Map.class), moduleEntity);
			moduleEntity.setSnmpYml(snmpYml);
		}
		moduleService.updateById(moduleEntity);
		return R.ok();
	}
	
	@DeleteMapping
	@SysLog("delete module")
	public R remove(@RequestParam Integer ... ids) {
		ValidateUtils.is(ids).notNull(RCode.MODULE_ID_ISNULL);
		moduleService.validateBulitinRule(Arrays.asList(ids));
		moduleService.removeHandle(Arrays.asList(ids));
		return R.ok();
	}
}
