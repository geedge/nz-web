package com.nis.modules.detect.service;

import com.nis.common.utils.PageUtils;
import com.nis.modules.detect.entity.DeteSetNodeRelEntity;
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
public interface DeteSetNodeRelService extends DeteCommonService<DeteSetNodeRelEntity> {

    PageUtils queryPage(Map<String, Object> params);
    
    List<DeteSetNodeRelEntity> queryBySystemId(PermissionInfoEntity entity);

    void deleteBySetId(Long id);
}

