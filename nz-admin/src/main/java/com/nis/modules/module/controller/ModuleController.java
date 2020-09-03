package com.nis.modules.module.controller;

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
		moduleService.saveModule(moduleEntity);
		return R.ok();
	}
	
	@PutMapping
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.MODULE)
	public R modify(@RequestBody Module moduleEntity) {
		ValidateUtils.is(moduleEntity.getId()).notNull(RCode.MODULE_ID_ISNULL);

		moduleService.updateModule(moduleEntity);
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
