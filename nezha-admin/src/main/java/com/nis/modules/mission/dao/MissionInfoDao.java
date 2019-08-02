package com.nis.modules.mission.dao;

import com.nis.modules.mission.entity.MissionInfoEntity;
import com.nis.modules.sys.entity.PermissionInfoEntity;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 任务信息表
 * 
 * @author th
 * @email ${email}
 * @date 2019-07-18 10:37:19
 */
@Mapper
public interface MissionInfoDao extends BaseMapper<MissionInfoEntity> {
	
	// 查询当前用户可查看任务详情列表
	Page<MissionInfoEntity> selectMissionInfoList(Page page,@Param("missionInfo") MissionInfoEntity missionInfoEntity);
	
	List<MissionInfoEntity> queryByPermission(PermissionInfoEntity entity);
	
}
