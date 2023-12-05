package com.nis.modules.sys.controller;

import cn.hutool.json.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.sys.service.LicenseManagementService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class LicenseManagementController {

    @Autowired
    private LicenseManagementService licenseManagementService;

    @GetMapping("/license/product")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.LICENSE)
    public R queryLicenseProductList(@RequestParam(value = "saId", required = false) Integer saId,
                                     @RequestParam(value = "islocal", required = false, defaultValue = "1") Integer islocal) {
        ValidateUtils.is(saId).notNull(RCode.SOFTWARE_ASSET_ID_ISNULL);

        List<JSONObject> productList = licenseManagementService.queryLicenseProductList(saId, islocal);

        // value as string
        String jsonString = JSON.toJSONString(productList, JSONWriter.Feature.WriteNonStringValueAsString);
        List<Map> returnData = JSONUtil.toList(jsonString, Map.class);

        return R.ok().putData("list", returnData);
    }

    @GetMapping("/license/downloadC2V")
    @SysLog(operation = OperationEnum.DOWNLOAD, type = TypeEnum.LICENSE)
    public void downloadC2V(@RequestParam(value = "saId", required = false) Integer saId, HttpServletResponse response) throws Exception {
        ValidateUtils.is(saId).notNull(RCode.SOFTWARE_ASSET_ID_ISNULL);

        licenseManagementService.downloadC2V(saId, response);
    }


    @PostMapping("/license/uploadV2C")
    @SysLog(operation = OperationEnum.UPLOAD, type = TypeEnum.LICENSE)
    public R uploadV2C(@RequestParam(value = "file") MultipartFile file,
                       @RequestParam(value = "saId", required = false) Integer saId) throws Exception {

        ValidateUtils.is(saId).notNull(RCode.SOFTWARE_ASSET_ID_ISNULL)
                .and(file).notNull(RCode.LICENSE_UPLOAD_V2CFILE_ISNULL);

        licenseManagementService.uploadV2C(saId, file);
        return R.ok();
    }

}
