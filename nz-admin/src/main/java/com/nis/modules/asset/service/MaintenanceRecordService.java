package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.MaintenanceRecord;

import java.util.Map;

/**
 * 维修记录 service
 *
 * @author shizhendong
 * @date 2019-11-21
 */
public interface MaintenanceRecordService extends IService<MaintenanceRecord> {

    PageUtils queryPage(Map<String, Object> params);

    void saveOrUpdateMaintenance(MaintenanceRecord maintenance);

    void delete(String ids);
}