package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.asset.entity.AssetTag;

import java.util.List;

/**
 * 资产标签
 *
 * @author shizhendong
 * @date 2020-06-11
 */
public interface AssetTagService extends IService<AssetTag> {

    void saveTagByAssetId(Integer assetId, List<AssetTag> tags);
}

