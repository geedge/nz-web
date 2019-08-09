package com.nis.modules.node.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.node.entity.NodeRoomEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 机房信息表
 * 
 * @author shizhendong
 * @date 2019-07-18 09:58:18
 */
@Mapper
@Repository
public interface NodeRoomDao extends BaseMapper<NodeRoomEntity> {

    /**
     * 查询存在关联节点的机房 包含有节点的机柜和节点信息
     * @return
     */
    List<NodeRoomEntity> queryRoomsNodes();


    /**
     * 查询所有存在机柜的机房列表 并含有机柜列表
     * @return
     */
    List<NodeRoomEntity> queryRoomCabinets();
	
}
