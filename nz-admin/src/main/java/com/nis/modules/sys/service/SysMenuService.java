package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.sys.entity.SysMenuEntity;

import java.util.List;

public interface SysMenuService extends IService<SysMenuEntity> {

	/**
	 * 根据父菜单，查询子菜单
	 * @param parentId 父菜单ID
	 * @param menuIdList  用户菜单ID
	 */
	List<SysMenuEntity> queryListParentId(Long parentId, List<Long> menuIdList);

	/**
	 * 根据父菜单，查询子菜单
	 * @param parentId 父菜单ID
	 */
	List<SysMenuEntity> queryListParentId(Long parentId);
	
	/**
	 * 获取不包含按钮的菜单列表
	 */
	List<SysMenuEntity> queryNotButtonList();
	
	/**
	 * 获取用户菜单列表
	 */
	List<SysMenuEntity> getUserMenuList(Long userId);

	/**
	 * 删除
	 */
	void delete(Long menuId);

	/**
	 * 根据角色获取 菜单列表
	 * @param roleId
	 * @return
	 */
	List<SysMenuEntity> getRoleMenuList(Integer roleId);

	/**
	 * 获取选中 所有 Menu(树结构) 的 ids
	 * @param roleMenuList
	 * @return
	 */
	List<Integer> getSelectedMenuIds(List<SysMenuEntity> roleMenuList);

	/**
	 * 菜单列表
	 * @return
	 * @param id
	 */
	List<SysMenuEntity> getMenuList(Integer id);
}