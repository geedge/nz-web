package com.nis.modules.node.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.node.entity.NodeInfoEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 节点信息表
 *
 * @author shizhendong
 * @date 2019-07-18 09:58:18
 */
@Mapper
@Repository
public interface NodeInfoDao extends BaseMapper<NodeInfoEntity> {

    void changeState(@Param("uuids") Long[] uuids, @Param("state") String state);

    void save(NodeInfoEntity nodeInfoEntity);

    // 查询机房下所包含的节点
    List<NodeInfoEntity> selectListByRoomId(IPage<NodeInfoEntity> page, @Param("params") Map<String, Object> params);

    //查询节点组和包含子节点组的节点
    List<NodeInfoEntity> selectListByGroups(IPage<NodeInfoEntity> page, @Param("params") Map<String, Object> params, @Param("groupdIds") Long[] groupdIds);
}
