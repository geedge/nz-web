package com.nis.modules.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.asset.dao.MaintenanceRecordDao;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.MaintenanceRecord;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.asset.service.MaintenanceRecordService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

/**
 * 数据中心    serviceImpl
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Service("maintenance")
public class MaintenanceRecordServiceImpl extends ServiceImpl<MaintenanceRecordDao, MaintenanceRecord> implements MaintenanceRecordService {

    @Autowired
    private AssetService assetService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String id = null, assetId = null, content = null;

        if (params != null && params.size() > 0) {
            id = (String) params.get("id");
            assetId = (String) params.get("assetId");
            content = (String) params.get("content");
        }

        IPage<MaintenanceRecord> page = this.page(
                new Query<MaintenanceRecord>().getPage(params),
                new QueryWrapper<MaintenanceRecord>()
                        .eq(StringUtils.isNotEmpty(id), "id", id)
                        .eq(StringUtils.isNotEmpty(assetId), "asset_id", assetId)
                        .like(StringUtils.isNotEmpty(content), "content", content)
        );

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
        maintenance.setCreateAt(now);
        maintenance.setCreateBy(ShiroUtils.getUserId());

        // 2. 入库
        this.saveOrUpdate(maintenance);
    }

    @Override
    public void delete(Long[] ids) {
        this.removeByIds(Arrays.asList(ids));
    }
}