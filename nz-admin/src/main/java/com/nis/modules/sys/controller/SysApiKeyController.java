package com.nis.modules.sys.controller;

import java.util.Arrays;
import java.util.Map;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.sys.entity.SysApiKey;
import com.nis.modules.sys.service.SysApiKeyService;

@RestController
@RequestMapping("/sys/apiKey")
public class SysApiKeyController {
	
	@Autowired
	private SysApiKeyService sysApiKeyService;
	
	@GetMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.SYSTEMAPIKEY)
	@RequiresPermissions({"sys:apiKey:list"})
	public R queryCredentialList(@RequestParam Map<String,Object> params) {
		PageUtils page = sysApiKeyService.queryPage(params);
		return R.ok(page);
	}
	
	@PostMapping
	@SysLog(operation = OperationEnum.ADD,type = TypeEnum.SYSTEMAPIKEY)
	@RequiresPermissions({"sys:apiKey:add"})
	public R addSysApiKey(@RequestBody SysApiKey sysApiKey) {
		ValidateUtils.is(sysApiKey.getName()).notNull(RCode.SYSAPIKEY_NAME_ISNULL)
		.and(sysApiKey.getRoleId()).notNull(RCode.SYSAPIKEY_ROLEID_ISNULL);
		
		Integer id = sysApiKeyService.addSysApiKey(sysApiKey);
		return R.ok().putData("id", id);
	}
	
	@DeleteMapping
	@SysLog(operation = OperationEnum.DELETE,type = TypeEnum.SYSTEMAPIKEY)
	@RequiresPermissions({"sys:apiKey:delete"})
	public R removeSysApiKey(@RequestParam String ids) {
		ValidateUtils.is(ids).notNull(RCode.SYSAPIKEY_ID_ISNULL);
		
		sysApiKeyService.removeByIds(Arrays.asList(ids.split(",")));
		return R.ok();
	}
	
}
