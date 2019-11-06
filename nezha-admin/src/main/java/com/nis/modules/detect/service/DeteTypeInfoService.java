package com.nis.modules.detect.service;

import com.nis.common.utils.PageUtils;
import com.nis.modules.detect.entity.DeteTypeInfoEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface DeteTypeInfoService extends DeteCommonService<DeteTypeInfoEntity> {
    PageUtils queryPage(Map<String, Object> params);

    DeteTypeInfoEntity queryDetail(Long id);

    void saveEntity(DeteTypeInfoEntity deteTypeInfo,MultipartFile file);

    void updateEntity(DeteTypeInfoEntity deteTypeInfo,MultipartFile file);

    boolean checkTablesExists(String tableName);

    void validatePassDate(DeteTypeInfoEntity deteTypeInfo, MultipartFile file);

    void deleteEntity(Long... id);

    boolean checkDeteMethodIsValide(DeteTypeInfoEntity deteTypeInfo);

    boolean isBuildIn(DeteTypeInfoEntity deteTypeInfoEntity);

}
