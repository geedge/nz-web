package com.nis.modules.asset.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.DateUtil;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.asset.dao.MaintenanceRecordDao;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.MaintenanceRecord;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.asset.service.MaintenanceRecordService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 数据中心    serviceImpl
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Service("maintenance")
public class MaintenanceRecordServiceImpl extends ServiceImpl<MaintenanceRecordDao, MaintenanceRecord> implements MaintenanceRecordService {

    // 资产 service
    @Autowired
    private AssetService assetService;

    // 资产维修 dao
    @Autowired
    private MaintenanceRecordDao maintenanceRecordDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MaintenanceRecord> page = new Query<MaintenanceRecord>().getPage(params);

        List<MaintenanceRecord> maintenanceRecords = maintenanceRecordDao.queryList(page, params);
        page.setRecords(maintenanceRecords);

        return new PageUtils(page);
    }

    @Override
    public void saveOrUpdateMaintenance(MaintenanceRecord maintenance) {

        // 1. 校验资产ID是否存在
        Asset asset = assetService.getById(maintenance.getAssetId());
        if (asset == null) {
            // 资产不存在
            throw new NZException(RCode.ASSET_NOT_EXIST);
        }

        Date now = new Date();
        Long userId = ShiroUtils.getUserId();

        // 2. 添加的时候维护创建人
        if (maintenance.getId() == null) {
            maintenance.setCreateAt(now);
            maintenance.setCreateBy(userId);
        }

        // 添加 修改都更新信息
        maintenance.setUpdateAt(now);
        maintenance.setUpdateBy(userId);

        // 2. 入库
        this.saveOrUpdate(maintenance);
    }

    @Override
    public void delete(String ids) {
        // 1.类型转换
        String[] idArr = ids.split(",");
        List<Integer> delIds = new ArrayList<>();

        for (String id : idArr) {
            try {
                delIds.add(Integer.parseInt(id));
            } catch (NumberFormatException e) {
                // ID转换异常
                throw new NZException(RCode.ASSET_ID_FORMAT);
            }
        }

        // 2.删除
        this.removeByIds(delIds);
    }
}
