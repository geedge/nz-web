package com.nis.modules.detectType.service.impl;

import com.nis.modules.detectType.dao.DeteTypeParamDao;
import com.nis.modules.detectType.entity.DeteTypeParamEntity;
import com.nis.modules.detectType.service.DeteTypeCommonService;
import com.nis.modules.detectType.service.DeteTypeParamService;
import org.springframework.stereotype.Service;

@Service("deteTypeParamService")
public class DeteTypeParamServiceImpl extends DeteTypeCommonServiceImpl<DeteTypeParamDao, DeteTypeParamEntity> implements DeteTypeParamService  {


}
