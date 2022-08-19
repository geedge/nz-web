package com.nis.modules.sys.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
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
import com.nis.modules.sys.entity.*;
import com.nis.modules.sys.service.*;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service("sysRoleService")
public class SysRoleServiceImpl extends ServiceImpl<SysRoleDao, SysRoleEntity> implements SysRoleService {

    private static Log log = Log.get();

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
                new Query(SysRoleEntity.class).getPage(params),
                new LambdaUpdateWrapper<SysRoleEntity>()
                        .eq(ToolUtil.isNotEmpty(params.get("id")), SysRoleEntity::getId, params.get("id"))
                        .like(ToolUtil.isNotEmpty(params.get("name")), SysRoleEntity::getName, Tool.StrUtil.likeEscape(Tool.MapUtil.getStr(params,"name")))
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
        ValidateUtils.is(name).notNull(RCode.SYS_ROLE_NAME_ISNULL).maxLength(64)
        .and(role.getRemark()).maxLength(256);

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
            if(!StrUtil.equals(oldRole.getName(),role.getName()))
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

        List<Long> userIds = userList.stream().map(SysUserEntity::getId).distinct().collect(Collectors.toList());

        // 删除 redis 中的 token
        List<String> removeKeys = new ArrayList<>();
        try {
            Map<String, SysUserEntity> shiroSessionInfoMap = ShiroUtils.getShiroSessionInfoMap();
            for (Map.Entry<String, SysUserEntity> entry : shiroSessionInfoMap.entrySet()) {
                SysUserEntity userEntity = entry.getValue();
                if (userIds.contains(userEntity.getId())) {
                    removeKeys.add(entry.getKey());
                }
            }
        } catch (RuntimeException e) {
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
        Set<Integer> selectedIds = roleMenuList.stream().filter(Objects::nonNull).map(SysMenuEntity::getId).collect(Collectors.toSet());

        resultData.put("menus", menuList);
        resultData.put("selectedIds", selectedIds);

        return resultData;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Map> deleteBatch(Integer force, Integer... roleIds) {
        List<Map> delRelObjectMsgList = Tool.ListUtil.list(true);

        List<Integer> idList = new ArrayList<>(Arrays.asList(roleIds));
        List<SysRoleEntity> delRoles = this.list(new LambdaQueryWrapper<SysRoleEntity>().in(SysRoleEntity::getId, idList));
        if (Tool.CollUtil.isEmpty(delRoles)) {
            return delRelObjectMsgList;
        }

        // 内置 role 不能删除
        List<Integer> buildInIdList = delRoles.stream().filter(role -> role.getBuildIn().equals(1)).map(SysRoleEntity::getId).collect(Collectors.toList());
        if (Tool.CollUtil.isNotEmpty(buildInIdList)) {
            throw new NZException(RCode.SYS_ROLE_CANNOT_DELETE);
        }


        delRoles.forEach(roleEntity -> {
            Map<Object, Object> relObjectInfoMap = this.getRoleDelRelObjectInfoMap(roleEntity);
            if (Tool.CollUtil.isNotEmpty(relObjectInfoMap)) {
                delRelObjectMsgList.add(relObjectInfoMap);
            }
        });

        force = Tool.ObjectUtil.defaultIfNull(force, 0);
        switch (force) {
            case 0: {// 0：存在关联数据时，删除失败
                if (Tool.CollUtil.isNotEmpty(delRelObjectMsgList)) {
                    return delRelObjectMsgList;
                }
            }
            case 1:  // 1：存在关联数据时，同时删除关联数据
                break;
            default:
                throw new NZException(RCode.SYS_ROLE_RELOBJ_CAN_NOT_REMOVE);
        }

        // 删除自身和关联内容
        // role
        this.removeByIds(idList);

        // user
        List<SysUserRoleEntity> userRoleRelList = sysUserRoleService.list(new LambdaQueryWrapper<SysUserRoleEntity>().in(SysUserRoleEntity::getRoleId, idList));
        if (Tool.CollUtil.isNotEmpty(userRoleRelList)) {
            String delUserIds = userRoleRelList.stream().map(SysUserRoleEntity::getUserId).map(String::valueOf).collect(Collectors.joining(","));
            sysUserService.deleteUsers(delUserIds);
        }

        // role_menu
        sysRoleMenuService.deleteBatch(roleIds);

        // role_user
        sysUserRoleService.deleteBatch(roleIds);
        return delRelObjectMsgList;
    }


    /**
     * 获取 role 删除提示关联信息：
     * user
     *
     * @return
     */
    private Map<Object, Object> getRoleDelRelObjectInfoMap(SysRoleEntity role) {
        List<Map> relatesList = Tool.ListUtil.list(true);

        // user
        List<SysUserEntity> sysUserList = sysUserService.getUserListByRoleId(role.getId());
        if (Tool.CollUtil.isNotEmpty(sysUserList)) {
            for (SysUserEntity user : sysUserList) {
                Map<Object, Object> map = Tool.MapUtil.builder()
                        .put("id", user.getId())
                        .put("name", user.getName())
                        .put("type", "user")
                        .build();
                relatesList.add(map);
            }
        }

        if (Tool.CollUtil.isNotEmpty(relatesList)) {
            Map<Object, Object> delRelObjectMap = Tool.MapUtil.builder()
                    .put("id", role.getId())
                    .put("name", role.getName())
                    .put("type", "role")
                    .put("relates", relatesList)
                    .build();

            return delRelObjectMap;
        }
        return Tool.MapUtil.empty();
    }
}
