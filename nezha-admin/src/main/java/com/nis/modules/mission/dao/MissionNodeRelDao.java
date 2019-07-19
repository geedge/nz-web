package com.nis.modules.mission.dao;

import com.nis.modules.mission.entity.MissionNodeRelEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务节点关系表
 * 
 * @author hkx
 * @email ${email}
 * @date 2019-07-18 10:37:19
 */
@Mapper
public interface MissionNodeRelDao extends BaseMapper<MissionNodeRelEntity> {
	
}
