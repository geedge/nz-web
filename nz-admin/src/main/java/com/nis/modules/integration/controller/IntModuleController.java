package com.nis.modules.integration.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.integration.entity.IntModuleEntity;
import com.nis.modules.integration.service.IntModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/integration")
public class IntModuleController {

    @Autowired
    private IntModuleService moduleService;

    @GetMapping("/module/{id}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.INTEGRATION)
    public R queryIntegrationModuleInfo(@PathVariable Integer id) {
        ValidateUtils.is(id).notNull(RCode.INT_MODULE_ID_ISNULL);

        IntModuleEntity moduleEntity = moduleService.queryIntModuleInfo(id);

        return R.ok(moduleEntity);
    }

    @GetMapping("/module")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.INTEGRATION)
    public R queryModulePage(@RequestParam Map param) {

        PageUtils page = moduleService.queryModulePage(param);

        return R.ok(page);
    }

    @GetMapping("/metadata")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.INTEGRATION)
    public R queryMetadataPage(@RequestParam Map params) {
        PageUtils page = moduleService.queryMetadataPage(params);

        return R.ok(page);
    }

    @GetMapping("/alertRule")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.INTEGRATION)
    public R queryAlertRulePage(@RequestParam Map params) {
        PageUtils page = moduleService.queryAlertRulePage(params);

        return R.ok(page);
    }
}
