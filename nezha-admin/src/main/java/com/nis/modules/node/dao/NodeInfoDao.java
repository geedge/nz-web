package com.nis.modules.node.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.node.entity.NodeInfoEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

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
}
