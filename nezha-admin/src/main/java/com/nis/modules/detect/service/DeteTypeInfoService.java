package com.nis.modules.detect.service;

import com.nis.common.utils.PageUtils;
import com.nis.modules.detect.entity.DeteTypeInfoEntity;
import com.nis.modules.detect.entity.DeteTypeMetaMappingEntity;
import com.nis.modules.detect.entity.DeteTypeMetaMapppingVO;

import java.util.List;
import java.util.Map;

public interface DeteTypeInfoService extends DeteTypeCommonService<DeteTypeInfoEntity>  {
    PageUtils queryPage(Map<String, Object> params);

    DeteTypeInfoEntity queryDetail(Long id);


    void saveEntity(DeteTypeInfoEntity deteTypeInfo);

    void updateEntity(DeteTypeInfoEntity deteTypeInfo);



    boolean checkTablesExists(String tableName);

    void validatePassDate(DeteTypeInfoEntity deteTypeInfo);

    void deleteEntity(DeteTypeInfoEntity id);


}
