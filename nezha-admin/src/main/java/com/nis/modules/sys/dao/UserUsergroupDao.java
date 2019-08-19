package com.nis.modules.sys.dao;

import com.nis.modules.sys.entity.UserUsergroupEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 
 * 
 * @author song
 * @email 
 * @date 2019-08-01 14:56:46
 */
@Mapper
public interface UserUsergroupDao extends BaseMapper<UserUsergroupEntity> {
	//验证用户和用户组信息是否一致
	Integer checkUserGroupCorrect(@Param("userId")Long userId,@Param("userGroupIds")Long[] userGroupIds);

}
