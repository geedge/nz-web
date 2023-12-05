package com.nis.modules.sys.service;

import cn.hutool.json.JSONObject;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface LicenseManagementService {

	List<JSONObject> queryLicenseProductList(Integer saId, Integer islocal);

	void downloadC2V(Integer saId, HttpServletResponse response) throws Exception;

	void uploadV2C(Integer saId, MultipartFile file) throws Exception;

}
