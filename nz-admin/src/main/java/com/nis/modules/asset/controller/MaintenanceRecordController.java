package com.nis.modules.asset.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.MaintenanceRecord;
import com.nis.modules.asset.service.MaintenanceRecordService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


/**
 * 维修记录  controller
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@RestController
@RequestMapping("/asset/maintenance")
public class MaintenanceRecordController {

    @Autowired
    private MaintenanceRecordService maintenanceRecordService;

    /**
     * 维修记录列表查询
     *
     * @param params
     * @return
     */
    @GetMapping
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ASSET_MAINTENANCE)
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = maintenanceRecordService.queryPage(params);
        return R.ok(page);
    }

    /**
     * 保存
     *
     * @param maintenance
     * @return
     */
    @PostMapping
    @RequiresPermissions({"asset:maintenance:save"})
    @SysLog(operation = OperationEnum.ADD, type = TypeEnum.ASSET_MAINTENANCE)
    public R save(@RequestBody MaintenanceRecord maintenance) {
        ValidateUtils.is(maintenance.getAssetId()).notNull(RCode.ASSET_ID_ISNULL)
                .and(maintenance.getContent()).notNull(RCode.ASSET_MAINTENANCE_CONTENT_ISNULL);

        maintenanceRecordService.saveOrUpdateMaintenance(maintenance);
        return R.ok();
    }

    @PutMapping
    @RequiresPermissions({"asset:maintenance:update"})
    @SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.ASSET_MAINTENANCE)
    public R update(@RequestBody MaintenanceRecord maintenance) {
        ValidateUtils.is(maintenance.getId()).notNull(RCode.ASSET_MAINTENANCEID_ISNULL)
                .and(maintenance.getAssetId()).notNull(RCode.ASSET_ID_ISNULL)
                .and(maintenance.getContent()).notNull(RCode.ASSET_MAINTENANCE_CONTENT_ISNULL);

        maintenanceRecordService.saveOrUpdateMaintenance(maintenance);
        return R.ok();
    }
}
