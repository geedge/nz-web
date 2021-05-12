package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.sys.entity.SysUserRoleEntity;

import java.util.List;

public interface SysUserRoleService extends IService<SysUserRoleEntity> {

	void saveOrUpdate(Long userId, String roleIds);
	
	/**
	 * 根据用户ID，获取角色ID列表
	 */
	List<Integer> queryRoleIdList(Integer userId);

	/**
	 * 根据角色ID数组，批量删除
	 */
	int deleteBatch(Integer[] roleIds);
}
