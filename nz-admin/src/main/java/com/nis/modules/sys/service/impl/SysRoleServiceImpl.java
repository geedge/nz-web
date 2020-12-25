package com.nis.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.sys.dao.SysMenuDao;
import com.nis.modules.sys.dao.SysRoleDao;
import com.nis.modules.sys.entity.SysMenuEntity;
import com.nis.modules.sys.entity.SysRoleEntity;
import com.nis.modules.sys.entity.SysRoleMenuEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.*;
import com.nis.modules.sys.shiro.ShiroUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service("sysRoleService")
public class SysRoleServiceImpl extends ServiceImpl<SysRoleDao, SysRoleEntity> implements SysRoleService {

    @Autowired
    private SysRoleMenuService sysRoleMenuService;

    @Autowired
    private SysUserRoleService sysUserRoleService;

    @Autowired
    private SysMenuService sysMenuService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysMenuDao sysMenuDao;

    @Autowired
    private RedisTemplate redisTemplate;

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
        ValidateUtils.is(name).notNull(RCode.SYS_ROLE_NAME_ISNULL);

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

        // 处理角色菜单关系
        this.handleRoleMenuRelation(role.getId(), role.getMenuIds());
    }

    /**
     * 处理 角色 和 菜单 关联关系
     * @param roleId
     * @param menuIds
     */
    private void handleRoleMenuRelation(Integer roleId, List<Integer> menuIds) {
        // 当前角色关联的菜单
        List<SysRoleMenuEntity> menus = sysRoleMenuService.list(new LambdaQueryWrapper<SysRoleMenuEntity>().eq(SysRoleMenuEntity::getRoleId, roleId));
        List<Integer> menuIdList = menus.stream().map(SysRoleMenuEntity::getMenuId).distinct().collect(Collectors.toList());

        // menu 信息未改变 不进行处理
        if (CommonUtils.checkIdListDiffrent(menuIdList, menuIds))
            return;

        // 更新角色与菜单关系
        sysRoleMenuService.saveOrUpdate(roleId, menuIds);

        // 角色关联 用户
        List<SysUserEntity> userList = sysUserService.getUserListByRoleId(roleId);
        if (CollectionUtils.isEmpty(userList))
            return;

        List<Long> userIds = userList.stream().map(SysUserEntity::getUserId).distinct().collect(Collectors.toList());

        // 删除 redis 中的 token
        List<String> removeKeys = new ArrayList<>();
        try {
            Map<String, SysUserEntity> shiroSessionInfoMap = ShiroUtils.getShiroSessionInfoMap();
            for (Map.Entry<String, SysUserEntity> entry : shiroSessionInfoMap.entrySet()) {
                SysUserEntity userEntity = entry.getValue();
                if (userIds.contains(userEntity.getUserId())) {
                    removeKeys.add(entry.getKey());
                }
            }
        } catch (Exception e) {
            log.error(String.format("从redis中获取用户登录信息失败,错误信息是：%s,角色Id是：%s", e.getMessage(), roleId), e);
        } finally {
            // 删除 关联key
            if (CollectionUtils.isNotEmpty(removeKeys)) {
                redisTemplate.delete(removeKeys);
            }
        }
    }

    @Override
    public Map getMenuListByRoleId(Integer roleId) {
        // 校验 role 是否存在
        this.validateRoleExist(roleId);

        Map resultData = new HashMap(4);
        List<SysMenuEntity> menuList = sysMenuService.getMenuList(null);

       /* List<SysMenuEntity> roleMenuList = sysMenuService.getRoleMenuList(roleId);
        List<Integer> selectedIds = Collections.emptyList();
        if(CollectionUtils.isNotEmpty(roleMenuList))
            selectedIds = sysMenuService.getSelectedMenuIds(roleMenuList);*/
        List<SysMenuEntity> roleMenuList = sysMenuDao.getRoleMenuList(roleId);
        Set<Integer> selectedIds = roleMenuList.stream().map(SysMenuEntity::getId).collect(Collectors.toSet());

        resultData.put("menus", menuList);
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