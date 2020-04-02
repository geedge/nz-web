package com.nis.modules.asset.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.asset.entity.Model;
import com.nis.modules.asset.entity.ModelWithMib;
import io.swagger.models.auth.In;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
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

    List<Model> queryList(IPage<Model> page, @Param("params") Map<String, Object> params);

    List<ModelWithMib> selectByModels(@Param("models") int[] models);

    List<Integer> selectIdByDictIds(@Param("vendorIds") String[] vendorIds,@Param("typeIds") String[] typeIds);
}
