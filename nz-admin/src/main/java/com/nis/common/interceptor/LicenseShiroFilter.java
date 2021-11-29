package com.nis.common.interceptor;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.shiro.web.filter.AccessControlFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nis.common.utils.Tool;
import com.nis.modules.sys.entity.LicenseEntity;
import com.nis.modules.sys.service.LicenseService;

@Component
public class LicenseShiroFilter extends AccessControlFilter {

	@Autowired
	private LicenseService licenseService;
	
	@Value("${compileType:release}")
	private String comileType;
	
	
	@Override
	protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue)
			throws Exception {
		// debug 模式不校验 license
		if(Tool.StrUtil.equalsIgnoreCase(comileType, "debug")) {
			return true;
		}
		//获取license
		LicenseEntity licenseEntity = licenseService.getAndSetLicenseCache();
		//验证并返回结果
		return licenseService.verify(licenseEntity);
	}

	@Override
	protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
		return false;
	}

}