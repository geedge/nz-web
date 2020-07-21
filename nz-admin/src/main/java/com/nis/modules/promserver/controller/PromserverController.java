package com.nis.modules.promserver.controller;

import java.util.Arrays;
import java.util.Map;

import com.nis.common.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.sys.controller.AbstractController;

@RestController
@RequestMapping("/promServer")
public class PromserverController extends AbstractController {
	
	@Autowired
	private PromserverService promserverService;
	
	@GetMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.PROMSERVER)
	public R list(@RequestParam Map<String, Object> params){
		PageUtils page = promserverService.queryPage(params);
		return R.ok(page);
	}
	
	@PostMapping
	@SysLog(operation = OperationEnum.ADD,type = TypeEnum.PROMSERVER)
	public R save(@RequestBody Promserver promserverEntity) {

		promserverService.saveOrUpdatePromserver(promserverEntity,false);
		return R.ok();
	}
	
	@PutMapping
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.PROMSERVER)
	public R modify(@RequestBody Promserver promserverEntity) {
		ValidateUtils.is(promserverEntity.getId()).notNull(RCode.PROMSERVER_ID_ISNULL);

		promserverService.saveOrUpdatePromserver(promserverEntity,true);
		return R.ok();
	}
	
	@DeleteMapping
	@SysLog(operation = OperationEnum.DELETE,type = TypeEnum.PROMSERVER)
	public R remove(String ids) {
		ValidateUtils.is(ids).notNull(RCode.PROMSERVER_ID_ISNULL);
		String[] split = ids.split(",");
		promserverService.removeByIds(Arrays.asList(split));
		return R.ok();
	}
}
