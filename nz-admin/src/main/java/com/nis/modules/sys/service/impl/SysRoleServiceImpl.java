package com.nis.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.sys.dao.SysRoleDao;
import com.nis.modules.sys.entity.SysMenuEntity;
import com.nis.modules.sys.entity.SysRoleEntity;
import com.nis.modules.sys.service.SysMenuService;
import com.nis.modules.sys.service.SysRoleMenuService;
import com.nis.modules.sys.service.SysRoleService;
import com.nis.modules.sys.service.SysUserRoleService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service("sysRoleService")
public class SysRoleServiceImpl extends ServiceImpl<SysRoleDao, SysRoleEntity> implements SysRoleService {

    @Autowired
    private SysRoleMenuService sysRoleMenuService;

    @Autowired
    private SysUserRoleService sysUserRoleService;

    @Autowired
    private SysMenuService sysMenuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SysRoleEntity> page = this.page(
                new Query<SysRoleEntity>().getPage(params),
                new LambdaUpdateWrapper<SysRoleEntity>()
                        .eq(ToolUtil.isNotEmpty(params.get("id")), SysRoleEntity::getId, params.get("id"))
                        .like(ToolUtil.isNotEmpty(params.get("name")), SysRoleEntity::getName, params.get("name"))
        );

        return new PageUtils(page);
    }

    /**
     * 校验 role 是否存在
     *
     * @param roleId
     */
    private void validateRoleExist(Integer roleId) {
        SysRoleEntity role = this.getById(roleId);

        if (role == null)
            throw new NZException(RCode.SYS_ROLE_ISNULL);
    }

    /**
     * 校验 角色 相关信息
     *
     * @param role
     */
    private void validateRoleInfo(SysRoleEntity role) {
        String name = role.getName();
        ValidateUtils.is(name).notNull(RCode.SYS_ROLE_NAME_ISNULL)
                .and(role.getRemark()).notNull(RCode.SYS_ROLE_REMARK_ISNULL);

        // 校验名称重复
        List<SysRoleEntity> list = this.list(new QueryWrapper<SysRoleEntity>().lambda().eq(SysRoleEntity::getName, name));

        boolean flag = false;
        if ((list == null || list.size() == 0) || (list != null && list.size() == 1 && list.get(0).getId().equals(role.getId()))) {
            flag = true;
        }
        if (!flag)
            throw new NZException(RCode.SYS_ROLE_NAME_DUPLICATE);


        List<Integer> menuIds = role.getMenuIds();
        if (CollectionUtils.isEmpty(menuIds))
            throw new NZException(RCode.SYS_ROLE_MENUID_ISNULL);

        List<SysMenuEntity> menuList = sysMenuService.list();
        List<Integer> menuIdList = menuList.stream().map(SysMenuEntity::getId).collect(Collectors.toList());

        for (Integer menuId : menuIds) {
            if (!menuIdList.contains(menuId))
                throw new NZException(RCode.SYS_MENU_ISNULL);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRole(SysRoleEntity role) {
        // 校验 角色 相关信息
        this.validateRoleInfo(role);

        // i18n 默认与 name 保持一致
        if (ToolUtil.isEmpty(role.getI18n())) {
            role.setI18n(role.getName());
        }

        this.save(role);

        sysRoleMenuService.saveOrUpdate(role.getId(), role.getMenuIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysRoleEntity role) {
        // 校验 角色 相关信息
        this.validateRoleInfo(role);

        SysRoleEntity oldRole = this.getById(role.getId());
        if (oldRole == null)
            throw new NZException(RCode.SYS_ROLE_ISNULL);

        // 内置角色 name 不允许修改
        if (oldRole.getBuildIn() == 1) {
            if (!oldRole.getName().equals(role.getName()))
                throw new NZException(RCode.SYS_ROLE_NAME_CANNOT_MODIFY);
        }

        this.updateById(role);

        // 更新角色与菜单关系
        sysRoleMenuService.saveOrUpdate(role.getId(), role.getMenuIds());
    }

    @Override
    public Map getMenuListByRoleId(Integer roleId) {
        // 校验 role 是否存在
        this.validateRoleExist(roleId);

        Map resultData = new HashMap(4);
        List<SysMenuEntity> roleMenuList = sysMenuService.getRoleMenuList(roleId);

        List<Integer> selectedIds = Collections.emptyList();
        if(CollectionUtils.isNotEmpty(roleMenuList))
            selectedIds = sysMenuService.getSelectedMenuIds(roleMenuList);

        resultData.put("menus", roleMenuList);
        resultData.put("selectedIds", selectedIds);

        return resultData;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(Integer[] roleIds) {
        // 内置角色不允许删除
        List<SysRoleEntity> buildInList = this.list(new LambdaQueryWrapper<SysRoleEntity>().eq(SysRoleEntity::getBuildIn, 1));
        List<Integer> buildInRoleIdList = buildInList.stream().map(SysRoleEntity::getId).collect(Collectors.toList());
        for (Integer roleId : roleIds) {
            if (buildInRoleIdList.contains(roleId))
                throw new NZException(RCode.SYS_ROLE_CANNOT_DELETE);
        }

        // 删除角色
        this.removeByIds(Arrays.asList(roleIds));

        // 删除角色与菜单关联
        sysRoleMenuService.deleteBatch(roleIds);

        // 删除角色与用户关联
        sysUserRoleService.deleteBatch(roleIds);
    }
}