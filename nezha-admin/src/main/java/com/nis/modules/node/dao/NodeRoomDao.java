package com.nis.modules.node.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.node.entity.NodeRoomEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 机房信息表
 * 
 * @author shizhendong
 * @date 2019-07-18 09:58:18
 */
@Mapper
@Repository
public interface NodeRoomDao extends BaseMapper<NodeRoomEntity> {
	
}
