package com.nis.modules.mission.dao;

import com.nis.modules.mission.entity.MissionNodeRelEntity;
import com.nis.modules.node.entity.NodeGroupInfoEntity;
import com.nis.modules.node.entity.NodeInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 任务节点关系表
 * 
 * @author th
 * @email ${email}
 * @date 2019-07-18 10:37:19
 */
@Mapper
public interface MissionNodeRelDao extends BaseMapper<MissionNodeRelEntity> {
	
	//根据任务id查询该任务下所有节点组信息详情
	List<NodeGroupInfoEntity> selectNodeGroupEntityByMissionid(@Param("missionId")Long missionId);
	
	//根据任务id查询该任务下所有节点组信息详情
	List<NodeInfoEntity> selectNodeEntityByMissionid(@Param("missionId")Long missionId);
}
