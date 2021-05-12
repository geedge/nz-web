package com.nis.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.StringUtils;
import com.nis.modules.sys.dao.SysUserRoleDao;
import com.nis.modules.sys.entity.SysUserRoleEntity;
import com.nis.modules.sys.service.SysUserRoleService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Service("sysUserRoleService")
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleDao, SysUserRoleEntity> implements SysUserRoleService {

	@Override
	public void saveOrUpdate(Long userId, String roleIds) {
		// 先删除用户与角色关系
		this.remove(new LambdaQueryWrapper<SysUserRoleEntity>().eq(SysUserRoleEntity::getUserId, userId));

		if (StringUtils.isEmpty(roleIds)) return;

		// 保存用户与角色关系
		List<SysUserRoleEntity> saveList = new ArrayList<>();
		SysUserRoleEntity entity;
		for (String roleId : Arrays.asList(roleIds.split(","))) {
			entity = new SysUserRoleEntity();
			entity.setUserId(userId.intValue());
			entity.setRoleId(Integer.valueOf(roleId));
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
