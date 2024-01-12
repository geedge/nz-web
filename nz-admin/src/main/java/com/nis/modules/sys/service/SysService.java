package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.sys.entity.SysComponent;
import com.nis.modules.sys.entity.SysConfigEntity;

import java.util.Map;

public interface SysService extends IService<SysConfigEntity> {

    Map<String,String> queryAppearance();

    void notifySysMessageByComponentName(String componentName, String topic, String message);

    void notifySysMessageByComponent(SysComponent component, String topic, String message);

    void processSysRestore(String serverId);

    void syncSystemLoginCache(String sysLoginLockoutJsonStr);
}
