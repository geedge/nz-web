package com.nis.modules.sys.controller;

import java.util.Arrays;
import java.util.Map;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.SnmpUtil;
import com.nis.common.utils.ToolUtil;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.asset.entity.AssetFieldGroup;
import com.nis.modules.sys.entity.SnmpCredential;
import com.nis.modules.sys.service.SnmpCredentialService;

@RestController
@RequestMapping("/snmp/credential")
public class SnmpCredentialController {
	
	@Autowired
	private SnmpCredentialService snmpCredentialService;
	
	@GetMapping("/{id}")
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.SNMPCREDENTIAL)
	public R queryCredentialById(@PathVariable Integer id) {
		ValidateUtils.is(id).notNull(RCode.SNMP_CREDENTIAL_ID_ISNULL);
		SnmpCredential result = snmpCredentialService.getById(id);
		return R.ok(result);
	}
	
	@GetMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.SNMPCREDENTIAL)
	public R queryCredentialList(@RequestParam Map<String,Object> params) {
		PageUtils page = snmpCredentialService.queryPage(params);
		return R.ok(page);
	}
	
	@PutMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.SNMPCREDENTIAL)
	@RequiresPermissions({"snmp:credential:update"})
	public R modifyCredential(@RequestBody SnmpCredential snmpCredential) {
		Integer id = snmpCredentialService.modifySnmpCredential(snmpCredential);
		return R.ok().putData("id", id);
	}
	
	@PostMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.SNMPCREDENTIAL)
	@RequiresPermissions({"snmp:credential:save"})
	public R saveCredential(@RequestBody SnmpCredential snmpCredential) {
		Integer id = snmpCredentialService.saveSnmpCredential(snmpCredential);
		return R.ok().putData("id", id);
	}
	
	@DeleteMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.SNMPCREDENTIAL)
	@RequiresPermissions({"snmp:credential:delete"})
	public R deleteCredential(@RequestParam String ids) {
		ValidateUtils.is(ids).notNull(RCode.SNMP_CREDENTIAL_ID_ISNULL);
		snmpCredentialService.removeSnmpCredential(Arrays.asList(ids.split(",")));
		return R.ok();
	}
}
