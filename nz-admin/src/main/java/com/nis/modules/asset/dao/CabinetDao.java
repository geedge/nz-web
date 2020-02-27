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

    List<Cabinet> queryPage(IPage<Cabinet> page, @Param("params") Map<String, Object> params);
}
