package com.nis.modules.asset.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.asset.entity.MaintenanceRecord;
import com.nis.modules.asset.service.MaintenanceRecordService;
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
    @SysLog(operation = OperationEnum.QUERY)
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
    @SysLog(operation = OperationEnum.ADD)
    public R save(@RequestBody MaintenanceRecord maintenance) {
        ValidateUtils.is(maintenance.getAssetId()).notNull(RCode.ASSET_ID_ISNULL)
                .and(maintenance.getContent()).notNull(RCode.ASSET_MAINTENANCE_CONTENT_ISNULL);

        maintenanceRecordService.saveOrUpdateMaintenance(maintenance);
        return R.ok();
    }

    @PutMapping
    @SysLog(operation = OperationEnum.UPDATE)
    public R update(@RequestBody MaintenanceRecord maintenance) {
        ValidateUtils.is(maintenance.getId()).notNull(RCode.ASSET_MAINTENANCEID_ISNULL)
                .and(maintenance.getAssetId()).notNull(RCode.ASSET_ID_ISNULL)
                .and(maintenance.getContent()).notNull(RCode.ASSET_MAINTENANCE_CONTENT_ISNULL);

        maintenanceRecordService.saveOrUpdateMaintenance(maintenance);
        return R.ok();
    }


    /**
     * 删除
     *
     * @param ids
     * @return
     */
    /*@DeleteMapping
    @SysLog("delete maintenanceRecord")
    public R delete(String ids) {
        ValidateUtils.is(ids).notNull(RCode.ASSET_MAINTENANCEID_ISNULL);

        maintenanceRecordService.delete(ids);
        return R.ok();
    }*/
}