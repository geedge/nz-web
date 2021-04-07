package com.nis.modules.promserver.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.sys.controller.AbstractController;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/agent")
public class PromserverController extends AbstractController {
	
	@Autowired
	private PromserverService promserverService;

	/**
	 * 详情
	 * @param id
	 * @return
	 */
	@GetMapping("/{id}")
	public R promServerDetails(@PathVariable("id") Integer id) {
		return R.ok(promserverService.queryPromServerDetails(id));
	}
	
	@GetMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.PROMSERVER)
	@RequiresPermissions({"promserver:list"})
	public R list(@RequestParam Map<String, Object> params){
		PageUtils page = promserverService.queryPage(params);
		return R.ok(page);
	}
	
	@PostMapping
	@SysLog(operation = OperationEnum.ADD,type = TypeEnum.PROMSERVER)
	@RequiresPermissions({"promserver:save"})
	public R save(@RequestBody Promserver promserverEntity) {

		Map map = new HashMap(2);
		map.put("id", promserverService.saveOrUpdatePromserver(promserverEntity, false));
		return R.ok(map);
	}
	
	@PutMapping
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.PROMSERVER)
	@RequiresPermissions({"promserver:update"})
	public R modify(@RequestBody Promserver promserverEntity) {
		ValidateUtils.is(promserverEntity.getId()).notNull(RCode.PROMSERVER_ID_ISNULL);

		Map map = new HashMap(2);
		map.put("id", promserverService.saveOrUpdatePromserver(promserverEntity, true));
		return R.ok(map);
	}
	
	@DeleteMapping
	@SysLog(operation = OperationEnum.DELETE,type = TypeEnum.PROMSERVER)
	@RequiresPermissions({"promserver:delete"})
	public R remove(String ids) {
		ValidateUtils.is(ids).notNull(RCode.PROMSERVER_ID_ISNULL);
		String[] split = ids.split(",");
		promserverService.removeByIds(Arrays.asList(split));
		return R.ok();
	}
}
