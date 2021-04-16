package com.nis.common.interceptor;

import java.security.PublicKey;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.sys.entity.LicenseEntity;
import com.nis.modules.sys.service.LicenseService;
import com.nis.modules.sys.service.impl.LicenseServiceImpl;

import cn.hutool.log.Log;

public class LicenseInterceptor extends HandlerInterceptorAdapter {
	private static final Log log = Log.get();

	@Autowired
	private LicenseService licenseService;


	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		//获取license
		LicenseEntity licenseEntity = licenseService.getAndSetLicenseCache();
		//验证并返回结果
		return licenseService.verify(licenseEntity);
	}

}