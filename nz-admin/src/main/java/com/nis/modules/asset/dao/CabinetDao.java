package com.nis.modules.asset.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.asset.entity.Cabinet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 机柜    dao
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Mapper
@Repository
public interface CabinetDao extends BaseMapper<Cabinet> {

    List<Cabinet> queryPage(IPage page, @Param("params") Map<String, Object> params);

    // 根据 数据中心id 获取包含机柜最大行列值
    Map<String,Integer> getCabinetMaxXY(@Param("id") Integer id);

    // 返回机柜信息中包含所属的资产 起始位置 数据
    List<Cabinet> getCabinetWithAssetPositionInfo();
}
