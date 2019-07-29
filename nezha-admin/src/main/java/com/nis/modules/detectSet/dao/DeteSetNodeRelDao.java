package com.nis.modules.detectSet.dao;

import com.nis.modules.detectSet.entity.DeteSetNodeRelEntity;
import com.nis.modules.sys.entity.PermissionInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

/**
 * 监测信息节点关系表
 * 
 * @author nezha
 * @email 
 * @date 2019-07-25 16:35:48
 */
@Mapper
public interface DeteSetNodeRelDao extends BaseMapper<DeteSetNodeRelEntity> {
	List<DeteSetNodeRelEntity> queryBySystemId(PermissionInfoEntity entity);
}
