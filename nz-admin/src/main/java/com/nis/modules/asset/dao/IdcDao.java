package com.nis.modules.asset.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.asset.entity.Idc;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 数据中心    dao
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Mapper
@Repository
public interface IdcDao extends BaseMapper<Idc> {
	
	List<Idc> queryPage(IPage<Idc> page,@Param("params") Map<String, Object> params);

	List<Idc> queryListByIds(@Param("idcIds") List<Integer> idcIds);
}
