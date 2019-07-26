package com.nis.modules.topo.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.topo.entity.TopoInfoEntity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 拓扑图 Dao
 *

 */
@Mapper
public interface TopoInfoDao extends BaseMapper<TopoInfoEntity> {
	/**
	 * 查询拓扑图列表信息
	 */
	List<TopoInfoEntity> queryTopoInfoList(@Param("id") Long id,@Param("systemId")Long systemId);
	
	/**
	 * 查询拓扑图详情
	 */
	TopoInfoEntity queryTopoInfoById(@Param("id") Long id);
	
}
