package com.nis.modules.node.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.node.entity.NodeGroupInfoEntity;
import com.nis.modules.node.entity.NodeInfoEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 节点组信息表
 * 
 * @author shizhendong
 * @date 2019-07-18 09:58:18
 */
@Mapper
@Repository
public interface NodeGroupInfoDao extends BaseMapper<NodeGroupInfoEntity> {

    List<NodeInfoEntity> assList(IPage<NodeInfoEntity> page, @Param("params") Map<String, Object> params);

    // 节点组详情查询
    NodeGroupInfoEntity detail(Long id);
}
