package com.nis.modules.endpoint.controller;

import java.util.Arrays;
import java.util.Map;

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
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.endpoint.service.EndpointService;
import com.nis.modules.sys.controller.AbstractController;

@RestController
@RequestMapping("/endpoint")
public class EndpointController extends AbstractController {
	
	@Autowired
	private EndpointService endpointService;
	
	@GetMapping
	public R list(@RequestParam Map<String, Object> params){
		PageUtils page = endpointService.queryPage(params);
		return R.ok().put("page", page);
	}
	
	@PostMapping
	@SysLog("save endpoint")
	public R save(@RequestBody Endpoint endpointEntity) {
		endpointService.validateParam(endpointEntity);
		endpointService.save(endpointEntity);
		return R.ok();
	}
	
	@PutMapping
	@SysLog("update endpoint")
	public R modify(@RequestBody Endpoint endpointEntity) {
		ValidateUtils.is(endpointEntity.getId()).notNull(RCode.ENDPOINT_ID_ISNULL);
		endpointService.validateParam(endpointEntity);
		endpointService.updateById(endpointEntity);
		return R.ok();
	}
	
	@DeleteMapping
	@SysLog("delete endpoint")
	public R remove(@RequestBody Long[] ids) {
		ValidateUtils.is(ids).notNull(RCode.ENDPOINT_ID_ISNULL);
		endpointService.removeByIds(Arrays.asList(ids));
		return R.ok();
	}
}

