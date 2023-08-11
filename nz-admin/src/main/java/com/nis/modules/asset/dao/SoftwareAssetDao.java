package com.nis.modules.asset.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.asset.entity.SoftwareAsset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * Software Asset Dao
 *
 * @author shizhendong
 * @date 2023-08-10
 */
@Mapper
public interface SoftwareAssetDao extends BaseMapper<SoftwareAsset> {

    List<SoftwareAsset> querySoftwareAssetInfoList(IPage page, @Param("params") Map<String, Object> params);

}
