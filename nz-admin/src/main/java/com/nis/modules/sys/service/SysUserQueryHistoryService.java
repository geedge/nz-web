package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.sys.entity.SysUserQueryHistoryEntity;

import java.util.List;
import java.util.Map;


public interface SysUserQueryHistoryService extends IService<SysUserQueryHistoryEntity> {

    PageUtils queryHistory(Map<String, Object> params);

    void saveQueryHistory(List<SysUserQueryHistoryEntity> queryHistoryList);

    void deleteQueryHistory(Map params);
}
