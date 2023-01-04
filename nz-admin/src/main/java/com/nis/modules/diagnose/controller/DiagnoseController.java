package com.nis.modules.diagnose.controller;

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.diagnose.service.DiagnoseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * DiagnoseController 故障排查
 *
 * @author shizhendong
 * @date 2023/1/4
 */
@RestController
public class DiagnoseController {

    @Autowired
    private DiagnoseService diagnoseService;

    @GetMapping("/asset/diagnose/{id}")
    public R assetDiagnose(@PathVariable(value = "id", required = false) Integer assetId,
                           @RequestHeader(required = false, defaultValue = "en") String language) {
        ValidateUtils.is(assetId).notNull(RCode.ASSET_ID_ISNULL);

        List<Map<Object, Object>> diagnoseMsgList = diagnoseService.assetErrorDiagnosis(assetId, language);
        return R.ok().putData("list", diagnoseMsgList);
    }

}
