package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.sys.entity.SysConfigEntity;

import java.util.Map;

public interface SysService extends IService<SysConfigEntity> {

    Map<String,String> queryAppearance();

    void notifySysMessage(String componentName, String topic, Object message);

    void processSysRestore(String serverId);

}
