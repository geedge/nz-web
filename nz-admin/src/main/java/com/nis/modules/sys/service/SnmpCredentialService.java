package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.sys.entity.SnmpCredential;

import java.util.List;
import java.util.Map;

public interface SnmpCredentialService extends IService<SnmpCredential>{

	PageUtils queryPage(Map<String,Object> params);
	
	Integer saveSnmpCredential(SnmpCredential snmpCredential);
	
	Integer modifySnmpCredential(SnmpCredential snmpCredential);
	
	void validateSnmpCredential(SnmpCredential snmpCredential);
	
	void removeSnmpCredential(List<String> ids);
}
