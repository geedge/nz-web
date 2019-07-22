package com.nis.modules.node.dao;

import com.nis.modules.node.entity.NodegroupInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 节点组信息表
 * 
 * @author shizhendong
 * @date 2019-07-18 09:58:18
 */
@Mapper
@Repository
public interface NodegroupInfoDao extends BaseMapper<NodegroupInfoEntity> {

}
