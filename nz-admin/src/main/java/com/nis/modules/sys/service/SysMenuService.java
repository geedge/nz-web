package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.sys.entity.SysMenuEntity;

import java.util.List;
import java.util.Map;

public interface SysMenuService extends IService<SysMenuEntity> {

    /**
     * 菜单列表
     * @param id
     * @return
     */
    List<SysMenuEntity> getMenuList(Integer id);

    void saveMenu(SysMenuEntity menu);

    void updateMenu(SysMenuEntity menu);

    void delete(Integer[] ids);

    /**
     * 根据角色获取 菜单列表
     * @param roleId
     * @return
     */
    List<SysMenuEntity> getRoleMenuList(Integer roleId);


    Map<String, Object> getMenuInfoByRoles(List<Integer> roleIds);


    /**
     * 根据叶子节点 得到关联完整节点集合
     * @param leafMenuList
     * @return
     */
    List<SysMenuEntity> getMenuByLeafMenu(List<SysMenuEntity> leafMenuList);

    /**
     * 获取选中 所有 Menu(树结构) 的 ids
     * @param roleMenuList
     * @return
     */
    List<Integer> getSelectedMenuIds(List<SysMenuEntity> roleMenuList);
}