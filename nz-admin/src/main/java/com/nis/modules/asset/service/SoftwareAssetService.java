package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.modules.asset.entity.SoftwareAsset;

import java.util.List;
import java.util.Map;

/**
 * Software Asset Service
 *
 * @author shizhendong
 * @date 2023-08-10
 */
public interface SoftwareAssetService extends IService<SoftwareAsset> {


    SoftwareAsset querySoftwareAssetInfo(Integer id);

    PageUtils querySoftwareAssetPage(Map<String, Object> params);

    Integer saveOrUpdateSoftwareAsset(SoftwareAsset softwareAsset);

    R deleteSoftwareAsset(Integer force, List<Integer> idList);

    List<SoftwareAsset> getSoftwareAssetListByTypeName(String... nameArr);

    List<SoftwareAsset> getSoftwareAssetListByTypeId(Integer... idArr);

    void updateLicenseStatus(String saIds);
}
