package com.nis.modules.detect.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.detect.entity.DeteSetNodeRelEntity;
import com.nis.modules.sys.entity.PermissionInfoEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

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
