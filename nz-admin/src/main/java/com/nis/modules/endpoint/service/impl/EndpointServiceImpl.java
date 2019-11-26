package com.nis.modules.endpoint.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.endpoint.dao.EndpointDao;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.endpoint.service.EndpointService;
import com.nis.modules.module.dao.ModuleDao;
import com.nis.modules.module.entity.Module;
import com.nis.modules.module.service.ModuleService;

@Service("endpointService")
public class EndpointServiceImpl  extends ServiceImpl<EndpointDao, Endpoint> implements EndpointService {
	
	@Autowired
	private EndpointDao endpointDao;
	
	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		IPage<Endpoint> page = new Query<Endpoint>().getPage(params);
		List<Endpoint> datas = endpointDao.queryPage(page, params);
        page.setRecords(datas);
        return new PageUtils(page);
	}

	@Override
	public void validateParam(Endpoint endpointEntity) {
		ValidateUtils.is(endpointEntity.getModuleId()).notNull(RCode.ENDPOINT_MODULEID_ISNULL);
		ValidateUtils.is(endpointEntity.getAssetId()).notNull(RCode.ENDPOINT_ASSETID_ISNULL);
		ValidateUtils.is(endpointEntity.getPort()).notNull(RCode.ENDPOINT_PORT_ISNULL);
		ValidateUtils.is(endpointEntity.getPath()).notNull(RCode.ENDPOINT_PATH_ISNULL);
	}

}
