package com.nis.modules.asset.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.asset.entity.Model;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 资产型号    dao
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Mapper
@Repository
public interface ModelDao extends BaseMapper<Model> {

    List<Model> queryList(IPage<Model> page, Map<String, Object> params);
}
