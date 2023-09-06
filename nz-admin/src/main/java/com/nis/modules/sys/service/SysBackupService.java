package com.nis.modules.sys.service;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface SysBackupService {

	Object queryInfo();

	void save(Map<String, Object> params);

	List<Object> queryBackupList();

	void backupData(String remark) throws Exception;

	void removeBackFileByFilename(String filename);

	void downloadBackFileByFilename(HttpServletResponse response, String filename) throws IOException;

	void unpackBackupFile(MultipartFile file, HttpServletResponse response);

	void syncBackupFile(String serverid, String type, String filename, HttpServletResponse response);

	void restore(String filename, String pin);

}
