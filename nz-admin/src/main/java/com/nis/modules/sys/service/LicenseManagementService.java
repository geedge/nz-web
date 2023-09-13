package com.nis.modules.sys.service;

import cn.hutool.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface LicenseManagementService {

	List<JSONObject> queryLicenseProductList(Integer saId);

	void downloadC2V(Integer saId, String haspid, HttpServletResponse response) throws Exception;

	void uploadV2C(Integer saId, MultipartFile file) throws Exception;

}
