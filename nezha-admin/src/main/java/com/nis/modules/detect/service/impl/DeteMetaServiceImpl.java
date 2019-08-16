package com.nis.modules.detect.service.impl;

import com.nis.modules.detect.dao.DeteTypeMetaDao;
import com.nis.modules.detect.entity.DeteTypeMetaEntity;
import com.nis.modules.detect.service.DeteMetaService;
import org.springframework.stereotype.Service;

@Service("deteTypeMetaService")
public class DeteMetaServiceImpl extends DeteCommonServiceImpl<DeteTypeMetaDao, DeteTypeMetaEntity> implements DeteMetaService {
}
