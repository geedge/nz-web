package com.nis.modules.detectSet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.detectSet.entity.DeteSetNodeRelEntity;
import com.nis.modules.sys.entity.PermissionInfoEntity;

import java.util.List;
import java.util.Map;

/**
 * 监测信息节点关系表
 *
 * @author nezha
 * @email 
 * @date 2019-07-25 16:35:48
 */
public interface DeteSetNodeRelService extends IService<DeteSetNodeRelEntity> {

    PageUtils queryPage(Map<String, Object> params);
    
    List<DeteSetNodeRelEntity> queryBySystemId(PermissionInfoEntity entity);

    void deleteBySetId(Long id);
}

