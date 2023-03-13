package com.nis.modules.endpoint.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.endpoint.entity.MonitorEndpointState;

import java.util.List;

public interface MonitorEndpointStateService extends IService<MonitorEndpointState> {
	
	void modifyBatchEndpointState(List<MonitorEndpointState> datas,String type);
}
