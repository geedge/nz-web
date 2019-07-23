package com.nis.modules.detectType.service.impl;

import com.nis.modules.detectType.dao.DeteTypeMetaDao;
import com.nis.modules.detectType.entity.DeteTypeMetaEntity;
import com.nis.modules.detectType.service.DeteTypeCommonService;
import com.nis.modules.detectType.service.DeteTypeMetaService;
import org.springframework.stereotype.Service;

@Service("deteTypeMetaService")
public class DeteTypeMetaServiceImpl extends DeteTypeCommonServiceImpl<DeteTypeMetaDao, DeteTypeMetaEntity> implements DeteTypeMetaService{
}
