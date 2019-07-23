package com.nis.modules.dashboard.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.dashboard.entity.DashboardEntity;

@Mapper
public interface DashboardDao extends BaseMapper<DashboardEntity>{
	
	List<DashboardEntity> selectAllDashboards(@Param("systemId")Long systemId);

}
