package com.nis.modules.endpoint.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.endpoint.dao.MonitorEndpointStateDao;
import com.nis.modules.endpoint.entity.MonitorEndpointState;
import com.nis.modules.endpoint.service.MonitorEndpointStateService;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class MonitorEndpointServiceStateImpl extends ServiceImpl<MonitorEndpointStateDao, MonitorEndpointState> implements MonitorEndpointStateService {

	@Override
	public void modifyBatchEndpointState(List<MonitorEndpointState> datas, String type) {
		this.baseMapper.updateBatchEndpointState(datas,type);
	}

}
