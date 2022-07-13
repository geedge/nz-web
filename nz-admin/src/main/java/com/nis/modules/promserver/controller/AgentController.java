package com.nis.modules.promserver.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.promserver.entity.Agent;
import com.nis.modules.promserver.service.AgentService;
import com.nis.modules.sys.controller.AbstractController;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/agent")
public class AgentController extends AbstractController {
	
	@Autowired
	private AgentService agentService;

	/**
	 * 详情
	 * @param id
	 * @return
	 */
	@GetMapping("/{id}")
	public R agentDetails(@PathVariable("id") Integer id) {
		return R.ok(agentService.queryAgentDetails(id));
	}
	
	@GetMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.PROMSERVER)
	public R list(@RequestParam Map<String, Object> params){
		PageUtils page = agentService.queryPage(params);
		return R.ok(page);
	}
	
	@PostMapping
	@SysLog(operation = OperationEnum.ADD,type = TypeEnum.PROMSERVER)
	@RequiresPermissions({"agent:save"})
	public R save(@RequestBody Agent agent) {

		Map map = new HashMap(2);
		map.put("id", agentService.saveOrUpdateAgent(agent, false));
		return R.ok(map);
	}
	
	@PutMapping
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.PROMSERVER)
	@RequiresPermissions({"agent:update"})
	public R modify(@RequestBody Agent agent) {
		ValidateUtils.is(agent.getId()).notNull(RCode.AGENT_ID_ISNULL);

		Map map = new HashMap(2);
		map.put("id", agentService.saveOrUpdateAgent(agent, true));
		return R.ok(map);
	}
	
	@DeleteMapping
	@SysLog(operation = OperationEnum.DELETE,type = TypeEnum.PROMSERVER)
	@RequiresPermissions({"agent:delete"})
	public R remove(String ids) {
		ValidateUtils.is(ids).notNull(RCode.AGENT_ID_ISNULL);
		String[] split = ids.split(",");
		agentService.removeByIds(Arrays.asList(split));
		return R.ok();
	}
}
