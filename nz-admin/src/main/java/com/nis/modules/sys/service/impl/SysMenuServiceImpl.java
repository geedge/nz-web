package com.nis.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RCode;
import com.nis.modules.sys.dao.SysMenuDao;
import com.nis.modules.sys.entity.SysMenuEntity;
import com.nis.modules.sys.entity.SysRoleMenuEntity;
import com.nis.modules.sys.service.SysMenuService;
import com.nis.modules.sys.service.SysRoleMenuService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service("sysMenuService")
public class SysMenuServiceImpl extends ServiceImpl<SysMenuDao, SysMenuEntity> implements SysMenuService {

	@Autowired
	private SysRoleMenuService sysRoleMenuService;

	@Autowired
	private SysMenuDao sysMenuDao;

	@Override
	public List<SysMenuEntity> getMenuList(Integer id) {
		List<SysMenuEntity> menuList = this.list(new LambdaQueryWrapper<SysMenuEntity>().eq(id != null, SysMenuEntity::getId, id).eq(SysMenuEntity::getState,1).orderByAsc(SysMenuEntity::getOrderNum));
		if (CollectionUtils.isEmpty(menuList) || menuList.size() == 1)
			return menuList;
		menuList = this.getMenuTreeList(menuList, 0);
		return menuList;
	}


	/**
	 * 1. 参数非空
	 * 2. code 唯一
	 * 3. type 有效性
	 * 4. parent 是否存在
	 * 5. button 上级必须是 菜单 类型
	 *
	 * @param menu
	 */
	private void validateMenuInfo(SysMenuEntity menu) {
		ValidateUtils.is(menu.getName()).notNull(RCode.SYS_MENU_NAME_ISNULL)
				.and(menu.getCode()).notNull(RCode.SYS_MENU_CODE_ISNULL)
				.and(menu.getI18n()).notNull(RCode.SYS_MENU_I18N_ISNULL)
				.and(menu.getType()).notNull(RCode.SYS_MENU_TYPE_ISNULL)
				.and(menu.getOrderNum()).notNull(RCode.SYS_MENU_ORDERNUM_ISNULL);

		// code 唯一 校验
		List<SysMenuEntity> list = this.list(new QueryWrapper<SysMenuEntity>().lambda().eq(SysMenuEntity::getCode, menu.getCode()));

		boolean flag = false;
		if ((list == null || list.size() == 0) || (list != null && list.size() == 1 && list.get(0).getId().equals(menu.getId()))) {
			flag = true;
		}
		if (!flag) {
			throw new NZException(RCode.SYS_MENU_CODE_DUPLICATE);
		}

		// type 合法性校验
		boolean typeExist = false;
		for (Constant.MenuType menuType : Constant.MenuType.values()) {
			if(menu.getType().equals(menuType.getValue())){
				typeExist = true;
				break;
			}
		}
		if(!typeExist){
			throw new NZException(RCode.SYS_MENU_TYPE_INVALIDE);
		}

		// 存在上级ID时间,校验上级菜单是否存在
		if (menu.getParentId() != null && menu.getParentId() != 0) {
			SysMenuEntity parent = this.getById(menu.getParentId());
			if (parent == null)
				throw new NZException(RCode.SYS_MENU_ISNULL);

			// 校验 上级菜单类型是否是 菜单 ,按钮不可以做为上级菜单
			if (parent.getType() == Constant.MenuType.BUTTON.getValue())
				throw new NZException(RCode.SYS_MENU_BUTTON_PARENTTYPE_INVALIDE);

			// 校验 上级菜单类型是否是 菜单 ,Tab 不可以做为上级菜单
			if(parent.getType() == Constant.MenuType.TAB.getValue()){
				throw new NZException(RCode.SYS_MENU_TAB_PARENTTYPE_INVALIDE);
			}
		}
	}

	@Override
	public void saveMenu(SysMenuEntity menu) {
		// 参数信息校验
		this.validateMenuInfo(menu);

		if (menu.getParentId() == null)
			menu.setParentId(0);

		this.save(menu);
	}

	@Override
	public void updateMenu(SysMenuEntity menu) {
		// 参数信息校验
		this.validateMenuInfo(menu);

		if (menu.getParentId() == null)
			menu.setParentId(0);

		this.updateById(menu);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delete(Integer[] ids) {
		List<SysMenuEntity> menuList = this.list();
		Set<Integer> parentIds = menuList.stream().map(SysMenuEntity::getParentId).collect(Collectors.toSet());

		// 判断是否有子菜单或按钮
		for (Integer id : ids) {
			if (parentIds.contains(id))
				throw new NZException(RCode.SYS_MENU_DELSUB);
		}

		// 删除菜单
		this.removeByIds(Arrays.asList(ids));

		// 删除与角色关联关系
		sysRoleMenuService.remove(new LambdaQueryWrapper<SysRoleMenuEntity>().in(SysRoleMenuEntity::getMenuId, Arrays.asList(ids)));
	}

	/**
	 * 根据角色获取 菜单列表
	 *
	 * @param roleId
	 * @return
	 */
	@Override
	public List<SysMenuEntity> getRoleMenuList(Integer roleId) {
		List<SysMenuEntity> roleMenuList = sysMenuDao.getRoleMenuList(roleId);
		if (CollectionUtils.isEmpty(roleMenuList))
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
	 *
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
	 *
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

	@Override
	public Map<String, Object> getMenuInfoByRoles(List<Integer> roleIds, Integer menuState) {
		if (CollectionUtils.isEmpty(roleIds))
			return Collections.emptyMap();

		// 根据角色 ID 得到 关联得所有 menuID
		List<SysRoleMenuEntity> sysRoleMenuEntities = sysRoleMenuService.list(new LambdaQueryWrapper<SysRoleMenuEntity>().in(SysRoleMenuEntity::getRoleId, roleIds));
		Set<Integer> menuIds = sysRoleMenuEntities.stream().map(SysRoleMenuEntity::getMenuId).collect(Collectors.toSet());

		Map<String, Object> resultMap = new HashMap<>(4);
		if (CollectionUtils.isEmpty(menuIds)) {
			resultMap.put("menus", Collections.emptyList());
			resultMap.put("buttons", Collections.emptyList());
			return resultMap;
		}

		// 状态 0:停用 1:启用
		menuState = menuState == null ? 1 : menuState;
		// 查到的所有子节点
		List<SysMenuEntity> list = sysMenuDao.selectList(new LambdaQueryWrapper<SysMenuEntity>().in(SysMenuEntity::getId, menuIds).eq(SysMenuEntity::getState, menuState));

		List<SysMenuEntity> menus = this.getMenuByLeafMenu(list, menuState);
		List<SysMenuEntity> menuTreeList = this.getMenuTreeList(menus, 0);

		// 因为按钮不能作为父级节点，所以可以直接进行过滤
		List<SysMenuEntity> buttonList = list.stream().filter(sysMenuEntity -> sysMenuEntity.getType() == Constant.MenuType.BUTTON.getValue()).collect(Collectors.toList());
		List<String> buttonCodes = buttonList.stream().map(SysMenuEntity::getCode).collect(Collectors.toList());
		resultMap.put("menus", menuTreeList);
		resultMap.put("buttons", buttonCodes);
		return resultMap;
	}

	@Override
	public List<SysMenuEntity> getMenuByLeafMenu(List<SysMenuEntity> leafMenuList, Integer menuState) {
		// 状态 0:停用 1:启用
		menuState = menuState == null ? 1 : menuState;
		List<SysMenuEntity> menuList = this.list(new LambdaQueryWrapper<SysMenuEntity>().eq(SysMenuEntity::getState, menuState).orderByAsc(SysMenuEntity::getOrderNum));
		Map<Integer, SysMenuEntity> idAndMenuList = new HashMap<>();
		for (SysMenuEntity entity : menuList) {
			idAndMenuList.put(entity.getId(), entity);
		}

		List<SysMenuEntity> list = new ArrayList<>();
		for (SysMenuEntity pojo : leafMenuList) {
			this.getParentMenu(idAndMenuList, pojo, list);
		}
		list.addAll(leafMenuList);
		list = list.stream().sorted(Comparator.comparing(SysMenuEntity::getOrderNum)).distinct().collect(Collectors.toList());
		return list;
	}

	private void getParentMenu(Map<Integer, SysMenuEntity> idAndMenuList, SysMenuEntity pojo, List<SysMenuEntity> list) {
		SysMenuEntity parentMenu = idAndMenuList.get(pojo.getParentId());
		if (parentMenu != null) {
			list.add(parentMenu);
			this.getParentMenu(idAndMenuList, parentMenu, list);
		}
	}
}
