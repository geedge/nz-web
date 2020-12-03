package com.nis.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Constant;
import com.nis.modules.sys.dao.SysMenuDao;
import com.nis.modules.sys.entity.SysMenuEntity;
import com.nis.modules.sys.entity.SysRoleMenuEntity;
import com.nis.modules.sys.service.SysMenuService;
import com.nis.modules.sys.service.SysRoleMenuService;
import com.nis.modules.sys.service.SysUserService;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Service("sysMenuService")
public class SysMenuServiceImpl extends ServiceImpl<SysMenuDao, SysMenuEntity> implements SysMenuService {

	@Autowired
	private SysUserService sysUserService;

	@Autowired
	private SysRoleMenuService sysRoleMenuService;

	@Autowired
	private SysMenuDao sysMenuDao;

	@Override
	public List<SysMenuEntity> getMenuList(Integer id) {
		List<SysMenuEntity> menuList = this.list(new LambdaQueryWrapper<SysMenuEntity>().eq(id != null, SysMenuEntity::getId, id));
		if(CollectionUtils.isEmpty(menuList) || menuList.size() == 1)
			return menuList;
		menuList = this.getMenuTreeList(menuList, 0);
		return menuList;
	}


	@Override
	public List<SysMenuEntity> queryListParentId(Long parentId, List<Long> menuIdList) {
		List<SysMenuEntity> menuList = queryListParentId(parentId);
		if(menuIdList == null){
			return menuList;
		}
		
		List<SysMenuEntity> userMenuList = new ArrayList<>();
		for(SysMenuEntity menu : menuList){
			/*if(menuIdList.contains(menu.getMenuId())){
				userMenuList.add(menu);
			}*/
		}
		return userMenuList;
	}

	@Override
	public List<SysMenuEntity> queryListParentId(Long parentId) {
		return baseMapper.queryListParentId(parentId);
	}

	@Override
	public List<SysMenuEntity> queryNotButtonList() {
		return baseMapper.queryNotButtonList();
	}

	@Override
	public List<SysMenuEntity> getUserMenuList(Long userId) {
		//系统管理员，拥有最高权限
		if(userId == Constant.SUPER_ADMIN){
			return getAllMenuList(null);
		}
		
		//用户菜单列表
		List<Long> menuIdList = sysUserService.queryAllMenuId(userId);
		return getAllMenuList(menuIdList);
	}

	@Override
	public void delete(Long menuId){
		//删除菜单
		this.removeById(menuId);
		//删除菜单与角色关联
		sysRoleMenuService.remove(new QueryWrapper<SysRoleMenuEntity>().eq("menu_id", menuId));
	}

	/**
	 * 获取所有菜单列表
	 */
	private List<SysMenuEntity> getAllMenuList(List<Long> menuIdList){
		//查询根菜单列表
		List<SysMenuEntity> menuList = queryListParentId(0L, menuIdList);
		//递归获取子菜单
		// getMenuTreeList(menuList, menuIdList);

		return menuList;
	}

	/**
	 * 根据角色获取 菜单列表
	 * @param roleId
	 * @return
	 */
	@Override
	public List<SysMenuEntity> getRoleMenuList(Integer roleId) {
		List<SysMenuEntity> roleMenuList = sysMenuDao.getRoleMenuList(roleId);
		if(CollectionUtils.isEmpty(roleMenuList))
			return Collections.emptyList();
		// 递归获取树结构
		return this.getMenuTreeList(roleMenuList, 0);
	}

	@Override
	public List<Integer> getSelectedMenuIds(List<SysMenuEntity> roleMenuList) {
		List<SysMenuEntity> allMenuList = new ArrayList<>();
		for (SysMenuEntity entity : roleMenuList) {
			getAllChildrenMenuList(entity, allMenuList);
		}
		List<Integer> allMenuIdList = allMenuList.stream().map(SysMenuEntity::getId).collect(Collectors.toList());
		return allMenuIdList;
	}

	/**
	 * 平铺 获取所有 menu 节点
	 * @param menu
	 * @param menuList
	 */
	public void getAllChildrenMenuList(SysMenuEntity menu, List<SysMenuEntity> menuList) {
		menuList.add(menu);
		if (CollectionUtils.isNotEmpty(menu.getChildren())) {
			for (SysMenuEntity pojo : menu.getChildren()) {
				getAllChildrenMenuList(pojo, menuList);
			}
		}
	}

	/**
	 * 递归获取树结构
	 * @param menuList
	 * @param parentId
	 * @return
	 */
	private List<SysMenuEntity> getMenuTreeList(List<SysMenuEntity> menuList, Integer parentId) {
		List<SysMenuEntity> resultList = new ArrayList<>();
		for (SysMenuEntity menu : menuList) {
			Integer id = menu.getId();
			Integer pid = menu.getParentId();
			if (parentId.equals(pid)) {
				menu.setChildren(this.getMenuTreeList(menuList, id));
				resultList.add(menu);
			}
		}
		return resultList;
	}
}