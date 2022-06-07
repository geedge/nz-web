package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.sys.entity.SysApiKey;

import java.util.Map;

public interface SysApiKeyService extends IService<SysApiKey> {

    PageUtils queryPage(Map<String, Object> params);

    Integer addSysApiKey(SysApiKey sysApiKey);

    void removeSysApiKeys(String ids);

    PageUtils querySelfApiKeys(Map<String, Object> params);

    Map<String, Object> addSelfApiKey(SysApiKey sysApiKey);

    void removeSelfApiKeys(String ids);

    void updateRoleIdByUser(Integer userId, Integer roleId);
}
