package com.nis.modules.detect.service;

import com.nis.common.utils.PageUtils;
import com.nis.modules.detect.entity.DeteWarnInfoEntity;

import java.util.Map;

public interface DeteWarnInfoService extends DeteCommonService<DeteWarnInfoEntity> {
    void saveEntity(DeteWarnInfoEntity deteWarnInfoEntity);

    void updateEntity(DeteWarnInfoEntity deteWarnInfoEntity);

    void deleteEntity(Long[] ids);

    PageUtils queryPage(Map<String, Object> params);

    DeteWarnInfoEntity queryDetail(Long id);

    void deleteByTypeId(Long typeId);
}
