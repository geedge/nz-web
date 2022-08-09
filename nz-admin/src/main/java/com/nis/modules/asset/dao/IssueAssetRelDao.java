package com.nis.modules.asset.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.IssueAssetRel;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 问题管理    dao
 */
@Mapper
@Repository
public interface IssueAssetRelDao extends BaseMapper<IssueAssetRel> {

    List<AssetAsset> queryAssetList(Integer issueId);
}
