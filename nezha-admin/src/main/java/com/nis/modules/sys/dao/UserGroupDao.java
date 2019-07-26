package com.nis.modules.sys.dao;

import com.nis.modules.sys.entity.UserGroupEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

/**
 * 用户组信息表
 * 
 * @author song
 * @email 
 * @date 2019-07-26 11:22:14
 */
@Mapper
public interface UserGroupDao extends BaseMapper<UserGroupEntity> {
	
	List<UserGroupEntity> queryByUser(Long userId);
}
