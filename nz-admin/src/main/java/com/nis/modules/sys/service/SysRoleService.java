package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.modules.sys.entity.SysRoleEntity;

import java.util.Map;

public interface SysRoleService extends IService<SysRoleEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveRole(SysRoleEntity role);

    void update(SysRoleEntity role);

    /**
     * role 删除方法
     * @param force 0：存在关联数据时，删除失败 1：存在关联数据时，同时删除关联数据 默认：0
     * @param ids
     * @return
     */
    R deleteBatch(Integer force, Integer... ids);

    Map getMenuListByRoleId(Integer roleId);
}
