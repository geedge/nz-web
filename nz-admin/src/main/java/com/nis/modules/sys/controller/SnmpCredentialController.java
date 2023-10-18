package com.nis.modules.sys.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.sys.entity.SnmpCredential;
import com.nis.modules.sys.service.SnmpCredentialService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/snmp/credential")
public class SnmpCredentialController {
	
	@Autowired
	private SnmpCredentialService snmpCredentialService;
	
	@GetMapping("/{id}")
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.SNMP_CREDENTIAL)
	public R queryCredentialById(@PathVariable Integer id) {
		ValidateUtils.is(id).notNull(RCode.SNMP_CREDENTIAL_ID_ISNULL);
		SnmpCredential result = snmpCredentialService.getById(id);
		return R.ok(result);
	}
	
	@GetMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.SNMP_CREDENTIAL)
	public R queryCredentialList(@RequestParam Map<String,Object> params) {
		PageUtils page = snmpCredentialService.queryPage(params);
		return R.ok(page);
	}
	
	@PutMapping
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.SNMP_CREDENTIAL)
	@RequiresPermissions({"snmp:credential:update"})
	public R modifyCredential(@RequestBody SnmpCredential snmpCredential) {
		Integer id = snmpCredentialService.modifySnmpCredential(snmpCredential);
		return R.ok().putData("id", id);
	}
	
	@PostMapping
	@SysLog(operation = OperationEnum.ADD,type = TypeEnum.SNMP_CREDENTIAL)
	@RequiresPermissions({"snmp:credential:save"})
	public R saveCredential(@RequestBody SnmpCredential snmpCredential) {
		Integer id = snmpCredentialService.saveSnmpCredential(snmpCredential);
		return R.ok().putData("id", id);
	}
	
	@DeleteMapping
	@SysLog(operation = OperationEnum.DELETE,type = TypeEnum.SNMP_CREDENTIAL)
	@RequiresPermissions({"snmp:credential:delete"})
	public R deleteCredential(@RequestParam String ids) {
		ValidateUtils.is(ids).notNull(RCode.SNMP_CREDENTIAL_ID_ISNULL);
		snmpCredentialService.removeSnmpCredential(Arrays.asList(ids.split(",")));
		return R.ok();
	}
}
