package com.nis.modules.sys.service;

import java.io.IOException;
import java.security.PublicKey;

import com.nis.modules.sys.entity.LicenseEntity;

public interface LicenseService {

	/**
	 * 获取token 首先尝试获取 硬件token 硬件token获取失败从数据库读取 数据库不存在 通过uuid 生成，保存到数据库
	 * 
	 * @param key
	 * @return
	 */
	String getLicenseToken();

	/**
	 * 获取 license
	 * 
	 * @return
	 */
	LicenseEntity getLicenseEntity();

	/**
	 * 更新 license
	 */
	void uploadLicenseEntity(LicenseEntity license);

	/**
	 * 获取系统版本
	 * 
	 * @return
	 */
	String getSystemVersion();

	/**
	 * 验证license 签名，以及 开始时间，结束时间等是否有效
	 * 
	 * @return
	 */
	boolean verify(LicenseEntity license);

	/**
	 * 获取license 缓存，没有从数据库读取
	 * 
	 * @return
	 * @throws IOException
	 */
	LicenseEntity getAndSetLicenseCache();

	String getSignAlgorithm();

	PublicKey getPublicKey();

}
