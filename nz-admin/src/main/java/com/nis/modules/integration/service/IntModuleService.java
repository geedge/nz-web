package com.nis.modules.integration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.integration.entity.IntModuleEntity;

import java.util.Map;

public interface IntModuleService extends IService<IntModuleEntity> {

    IntModuleEntity queryIntModuleInfo(Integer id);

    PageUtils queryModulePage(Map param);

    PageUtils queryMetadataPage(Map params);

    PageUtils queryAlertRulePage(Map params);
}
