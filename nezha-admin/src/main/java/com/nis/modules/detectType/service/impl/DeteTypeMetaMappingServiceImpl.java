package com.nis.modules.detectType.service.impl;

import com.nis.modules.detectType.dao.DeteTypeMetaMappingDao;
import com.nis.modules.detectType.entity.DeteTypeMetaMappingEntity;
import com.nis.modules.detectType.service.DeteTypeCommonService;
import com.nis.modules.detectType.service.DeteTypeMetaMappingService;
import org.springframework.stereotype.Service;

@Service("deteTypeMetaMappingService")
public class DeteTypeMetaMappingServiceImpl extends DeteTypeCommonServiceImpl<DeteTypeMetaMappingDao, DeteTypeMetaMappingEntity> implements DeteTypeMetaMappingService{
}
