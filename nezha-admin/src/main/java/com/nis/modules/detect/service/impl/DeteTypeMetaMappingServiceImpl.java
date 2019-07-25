package com.nis.modules.detect.service.impl;

import com.nis.modules.detect.dao.DeteTypeMetaMappingDao;
import com.nis.modules.detect.entity.DeteTypeMetaMappingEntity;
import com.nis.modules.detect.service.DeteTypeMetaMappingService;
import org.springframework.stereotype.Service;

@Service("deteTypeMetaMappingService")
public class DeteTypeMetaMappingServiceImpl extends DeteTypeCommonServiceImpl<DeteTypeMetaMappingDao, DeteTypeMetaMappingEntity> implements DeteTypeMetaMappingService{
}
