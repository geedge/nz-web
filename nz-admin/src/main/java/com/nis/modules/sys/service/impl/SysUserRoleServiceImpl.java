package com.nis.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.sys.dao.SysUserRoleDao;
import com.nis.modules.sys.entity.SysUserRoleEntity;
import com.nis.modules.sys.service.SysUserRoleService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service("sysUserRoleService")
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleDao, SysUserRoleEntity> implements SysUserRoleService {

	@Override
	public void saveOrUpdate(Long userId, List<Integer> roleIdList) {
		// 先删除用户与角色关系
		this.remove(new LambdaQueryWrapper<SysUserRoleEntity>().eq(SysUserRoleEntity::getUserId, userId));

		if (CollectionUtils.isEmpty(roleIdList)) return;

		// 保存用户与角色关系
		List<SysUserRoleEntity> saveList = new ArrayList<>(roleIdList.size());
		SysUserRoleEntity entity;
		for (Integer roleId : roleIdList) {
			entity = new SysUserRoleEntity();
			entity.setUserId(userId.intValue());
			entity.setRoleId(roleId);
			saveList.add(entity);
		}

		this.saveBatch(saveList);
	}

	@Override
	public List<Integer> queryRoleIdList(Integer userId) {
		return baseMapper.queryRoleIdList(userId);
	}

	@Override
	public int deleteBatch(Integer[] roleIds){
		return baseMapper.deleteBatch(roleIds);
	}
}
