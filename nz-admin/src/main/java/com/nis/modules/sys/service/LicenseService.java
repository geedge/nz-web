package com.nis.modules.sys.service;

public interface LicenseService {


	/**
	 * 获取 license
	 * 
	 * @return
	 */
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
}
