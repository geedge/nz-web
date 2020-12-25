/**

 *

 *

 */

package com.nis.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.sys.entity.SysUserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 系统用户
 *

 */
@Mapper
public interface SysUserDao extends BaseMapper<SysUserEntity> {
	
	/**
	 * 查询用户的所有菜单ID
	 */
	List<Long> queryAllMenuId(Long userId);


	List<SysUserEntity> queryList(IPage<SysUserEntity> page, @Param("params") Map<String, Object> params);

	// 根据角色ID 获取所属用户
	List<SysUserEntity> getUserListByRoleId(@Param("roleId")  Integer roleId);
}
