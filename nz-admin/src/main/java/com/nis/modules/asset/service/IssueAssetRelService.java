package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.IssueAssetRel;

import java.util.List;

/**
 * 资产问题关系 service
 */
public interface IssueAssetRelService extends IService<IssueAssetRel> {

    List<AssetAsset> queryAssetsByIssueId(Integer issueId);

}
