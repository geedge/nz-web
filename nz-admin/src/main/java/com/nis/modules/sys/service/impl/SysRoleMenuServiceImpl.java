package com.nis.modules.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.sys.dao.SysRoleMenuDao;
import com.nis.modules.sys.entity.SysRoleMenuEntity;
import com.nis.modules.sys.service.SysRoleMenuService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service("sysRoleMenuService")
public class SysRoleMenuServiceImpl extends ServiceImpl<SysRoleMenuDao, SysRoleMenuEntity> implements SysRoleMenuService {

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveOrUpdate(Integer roleId, List<Integer> menuIdList) {
		// 先删除角色与菜单关系
		this.deleteBatch(new Integer[]{roleId});

		if (CollectionUtils.isEmpty(menuIdList)) return;

		List<SysRoleMenuEntity> saveList = new ArrayList<>(menuIdList.size());
		SysRoleMenuEntity sysRoleMenuEntity;
		for (Integer menuId : menuIdList) {
			sysRoleMenuEntity = new SysRoleMenuEntity(null, roleId, menuId);
			saveList.add(sysRoleMenuEntity);
		}

		this.saveBatch(saveList);
	}

	@Override
	public List<Integer> queryMenuIdList(Integer roleId) {
		return baseMapper.queryMenuIdList(roleId);
	}

	@Override
	public int deleteBatch(Integer[] roleIds){
		return baseMapper.deleteBatch(roleIds);
	}
}