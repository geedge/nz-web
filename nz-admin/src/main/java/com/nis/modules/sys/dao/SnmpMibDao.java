package com.nis.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.sys.entity.SnmpMib;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SnmpMibDao extends BaseMapper<SnmpMib> {

    List<SnmpMib> queryList(IPage page, @Param("params") Map<String, Object> params);

    List<SnmpMib> getMibTreesByIdsAndModels(@Param("ids")String[] ids, @Param("models")String[] models);

    List<SnmpMib> queryByModelIds(@Param("ids") List<Integer> asList);
}
