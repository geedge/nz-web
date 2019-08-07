package com.nis.modules.detect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.detect.entity.DeteSetInfoEntity;
import com.nis.modules.sys.entity.PermissionInfoEntity;

import java.util.List;
import java.util.Map;

/**
 * 监测设置信息表
 *
 * @author nezha
 * @email 
 * @date 2019-07-25 16:35:48
 */
public interface DeteSetInfoService extends IService<DeteSetInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);
    
    List<DeteSetInfoEntity> queryByPermission(PermissionInfoEntity entity);

    boolean checkNameExists(String name, Long setId);

    void saveEntity(DeteSetInfoEntity deteSetInfo);

    void updateEntity(DeteSetInfoEntity deteSetInfo);

    void changeStateByIds(Long[] ids, String state);

    void deleteByTypeId(Long typeId);

    DeteSetInfoEntity queryDetail(Long id);
}

