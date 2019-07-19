package com.nis.modules.mission.dao;

import com.nis.modules.mission.entity.MissionInfoEntity;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

/**
 * 任务信息表
 * 
 * @author hkx
 * @email ${email}
 * @date 2019-07-18 10:37:19
 */
@Mapper
public interface MissionInfoDao extends BaseMapper<MissionInfoEntity> {
	
	IPage<MissionInfoEntity> queryPageMission(IPage<MissionInfoEntity> page,Map<String, Object> params);
	//List<MissionInfoEntity> queryPageMission(Page page,Map<String, Object> params);
	
	IPage<MissionInfoEntity> queryAllMission(@Param("ew") Wrapper<MissionInfoEntity> userWrapper,IPage<MissionInfoEntity> page);
	
	//根据id获取任务信息详情
	//MissionInfoEntity getMissionById(Integer id);
	
}
