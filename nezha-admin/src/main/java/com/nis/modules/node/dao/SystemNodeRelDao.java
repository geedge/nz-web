package com.nis.modules.node.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.node.entity.NodeInfoEntity;
import com.nis.modules.node.entity.SystemNodeRelEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 业务系统 节点 关联表
 *
 * @author shizhendong
 * @date 2019-07-18 09:58:18
 */
@Mapper
@Repository
public interface SystemNodeRelDao extends BaseMapper<SystemNodeRelEntity> {

}
