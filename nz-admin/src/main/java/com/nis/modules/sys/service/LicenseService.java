package com.nis.modules.sys.service;

import java.util.Map;

import org.dom4j.DocumentException;
import org.springframework.web.multipart.MultipartFile;

public interface LicenseService {


	/**
	 * 获取 license
	 * 
	 * @return
	 */
	Map<String,Object> getLicenseDetail();

	String getLicense();
	
	/**
	 * 获取系统版本
	 * 
	 * @return
	 */
	String getSystemVersion();


	void uploadLicense(String v2c);

	
	boolean installLicense();

	/**
	 * 证书校验
	 * @return
	 */
	Integer verify();

	String getLicenseC2V();
	
	void validateDigestValue(String v2c) throws DocumentException ;
	
	/**
	 * 生成v2c文件
	 */
	String gen(MultipartFile[] files,Map<String,Object> params);
}
