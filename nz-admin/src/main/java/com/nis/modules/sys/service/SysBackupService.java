package com.nis.modules.sys.service;

import java.util.List;
import java.util.Map;

public interface SysBackupService {

	Object queryInfo();

	void save(Map<String, Object> params);

	List<Object> queryBackupList();
	
}