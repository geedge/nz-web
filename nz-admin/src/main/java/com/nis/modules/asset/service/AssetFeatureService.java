package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.asset.entity.AssetFeature;

public interface AssetFeatureService extends IService<AssetFeature> {

    void collectAssetBasicInfo(Integer assetId);

}
