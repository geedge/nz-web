package com.nis.modules.detectSet.dao;

import com.nis.modules.detectSet.entity.DeteSetInfoEntity;
import com.nis.modules.sys.entity.PermissionInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

/**
 * 监测设置信息表
 * 
 * @author nezha
 * @date 2019-07-25 16:35:48
 */
@Mapper
public interface DeteSetInfoDao extends BaseMapper<DeteSetInfoEntity> {
	
	List<DeteSetInfoEntity> queryByPermission(PermissionInfoEntity entity);
}
