package com.nis.modules.detect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.detect.entity.DeteWarnInfoEntity;

import java.util.Map;

public interface DeteWarnInfoService extends IService<DeteWarnInfoEntity> {
    void saveEntity(DeteWarnInfoEntity deteWarnInfoEntity);

    void updateEntity(DeteWarnInfoEntity deteWarnInfoEntity);

    void deleteEntity(Long[] ids);

    boolean checkNameExists(String name, Long id);

    PageUtils queryPage(Map<String, Object> params);

    DeteWarnInfoEntity queryDetail(Long id);

    void deleteByTypeId(Long typeId);
}
