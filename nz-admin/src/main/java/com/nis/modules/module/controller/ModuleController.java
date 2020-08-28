package com.nis.modules.module.controller;

import com.alibaba.fastjson.JSONObject;
import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
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
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.MODULE)
	public R list(@RequestParam Map<String, Object> params){
		PageUtils page = moduleService.queryPage(params);
		return R.ok(page);
	}
	
	@PostMapping
	@SysLog(operation = OperationEnum.ADD,type = TypeEnum.MODULE)
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
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.MODULE)
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
	@SysLog(operation = OperationEnum.DELETE,type = TypeEnum.MODULE)
	public R remove(@RequestParam Integer ... ids) {
		ValidateUtils.is(ids).notNull(RCode.MODULE_ID_ISNULL);
		moduleService.validateBulitinRule(Arrays.asList(ids));
		moduleService.removeHandle(Arrays.asList(ids));
		return R.ok();
	}

	/**
	 * 根据 moduleId 查询 asset endpoint alert 统计信息
	 *
	 * @param id
	 * @return
	 */
	@GetMapping("/stat")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.MODULE)
	public R getModuleStatById(Integer id) {
		ValidateUtils.is(id).notNull(RCode.MODULE_ID_ISNULL);
		return R.ok(moduleService.getModuleStatById(id));
	}
}
