package com.nis.modules.node.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.node.entity.NodeCabinetEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 机柜信息表
 *
 * @author shizhendong
 * @date 2019-07-18 09:58:18
 */
@Mapper
@Repository
public interface NodeCabinetDao extends BaseMapper<NodeCabinetEntity> {

    // 根据 机房 ID  得到包含机柜中的最大行 列 号
    Map<String,Integer> getCabMaxRowCol(@Param("roomId") Long id);

    // 根据机房行得到列的集合
    List<Integer> getColsByRow(@Param("roomId") Long roomId, @Param("roomRow") int roomRow);
}
