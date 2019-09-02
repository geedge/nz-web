package com.nis.modules.detect.service;

import com.nis.modules.detect.entity.DeteTypeMetaEntity;

public interface DeteMetaService extends DeteCommonService<DeteTypeMetaEntity> {
    boolean checkFieldTypeIsValide(DeteTypeMetaEntity deteTypeMetaEntity);
    boolean checkDataTypeIsValide(DeteTypeMetaEntity deteTypeMetaEntity);
    boolean checkFieldNameExists(DeteTypeMetaEntity deteTypeMetaEntity);
    boolean checkNameExists(DeteTypeMetaEntity deteTypeMetaEntity);
}
