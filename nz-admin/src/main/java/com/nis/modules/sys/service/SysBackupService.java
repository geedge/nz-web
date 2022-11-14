package com.nis.modules.sys.service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface SysBackupService {

	Object queryInfo();

	void save(Map<String, Object> params);

	List<Object> queryBackupList();

	void backupData() throws Exception;

	void removeBackFileByFilename(String filename);

	void downloadBackFileByFilename(HttpServletResponse response, String filename) throws IOException;
}
