package com.nis.modules.detect.service;

import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.modules.detect.entity.DeteTypeMetaMappingEntity;
import com.nis.modules.detect.entity.DeteTypeMetaMapppingVO;

import java.util.List;
import java.util.Map;

public interface DeteTypeMetaMappingService extends DeteTypeCommonService<DeteTypeMetaMappingEntity> {
    PageUtils queryMetaMappingPage(Map<String, Object> params);

    void saveDicts(List<DeteTypeMetaMappingEntity> mappings);

    void updateDict(DeteTypeMetaMapppingVO deteTypeMetaMapppingVO);

    R deleteDic(String ... types);

    boolean checkMetaMappingTypeExists(String type);
}
