package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.sys.entity.SysConfigEntity;

import java.util.Map;

public interface SysAppearanceService extends IService<SysConfigEntity> {
    Map<String,String> queryAppearance();
}
