package com.nis.modules.detect.service.impl;

import com.nis.modules.detect.dao.DeteTypeMetaDao;
import com.nis.modules.detect.entity.DeteTypeMetaEntity;
import com.nis.modules.detect.service.DeteTypeMetaService;
import org.springframework.stereotype.Service;

@Service("deteTypeMetaService")
public class DeteTypeMetaServiceImpl extends DeteTypeCommonServiceImpl<DeteTypeMetaDao, DeteTypeMetaEntity> implements DeteTypeMetaService{
}
