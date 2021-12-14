package com.nis.modules.endpoint.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.endpoint.entity.MonitorEndpointState;

public interface MonitorEndpointStateService extends IService<MonitorEndpointState> {
	
	void modifyBatchEndpointState(List<MonitorEndpointState> datas,String type);
}
