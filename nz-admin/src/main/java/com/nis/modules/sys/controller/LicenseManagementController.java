package com.nis.modules.sys.controller;

import cn.hutool.json.JSONObject;
import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.sys.service.LicenseManagementService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping
public class LicenseManagementController {

    @Autowired
    private LicenseManagementService licenseManagementService;

    @GetMapping("/license/product")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.LICENSE)
    public R queryLicenseProductList(@RequestParam(value = "saId", required = false) Integer saId) {
        ValidateUtils.is(saId).notNull(RCode.SOFTWARE_ASSET_ID_ISNULL);

        List<JSONObject> productList = licenseManagementService.queryLicenseProductList(saId);
        return R.ok().putData("list", productList);
    }

    @GetMapping("/license/downloadC2V")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.LICENSE)
    public void downloadC2V(@RequestParam(value = "saId", required = false) Integer saId,
                            @RequestParam(value = "haspid", required = false) String haspid,
                            HttpServletResponse response)  throws Exception {

        ValidateUtils.is(saId).notNull(RCode.SOFTWARE_ASSET_ID_ISNULL)
                        .and(haspid).notNull(RCode.LICENSE_DOWNLOAD_C2VFILE_HASPID_ISNULL);

        licenseManagementService.downloadC2V(saId, haspid, response);
    }


    @PostMapping("/license/uploadV2C")
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.LICENSE)
    public R uploadV2C(@RequestParam(value = "file") MultipartFile file,
                       @RequestParam(value = "saId", required = false) Integer saId) throws Exception {

        ValidateUtils.is(saId).notNull(RCode.SOFTWARE_ASSET_ID_ISNULL)
                .and(file).notNull(RCode.LICENSE_UPLOAD_V2CFILE_ISNULL);

        licenseManagementService.uploadV2C(saId, file);
        return R.ok();
    }

}
