package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.sys.entity.SysUserStarredEntity;

import java.util.List;
import java.util.Map;

public interface SysUserStarredService extends IService<SysUserStarredEntity> {

    List<SysUserStarredEntity> queryStarred(String type);

    void addStarred(SysUserStarredEntity sysUserStarredEntity);

    void deleteStarred(Map<String, Object> params);
}
