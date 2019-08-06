package com.nis.modules.mission.dao;

import com.nis.modules.mission.entity.MissionResultEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 任务结果表
 * 
 * @author th
 * @email ${email}
 * @date 2019-07-18 10:37:19
 */
@Mapper
public interface MissionResultDao extends BaseMapper<MissionResultEntity> {
	// 查询当前用户可查看任务结果详情列表
	Page<MissionResultEntity> selectMissionResultInfoList(Page page,@Param("id") Integer missionId);
}
