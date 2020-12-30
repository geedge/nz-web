package com.nis.modules.endpoint.service.impl;


import cn.hutool.log.Log;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.endpoint.dao.EndpointStateDao;
import com.nis.modules.endpoint.entity.EndpointState;
import com.nis.modules.endpoint.service.EndpointStateService;
import org.springframework.stereotype.Service;

@Service("endpointSateService")
public class EndpointStateServiceImpl  extends ServiceImpl<EndpointStateDao, EndpointState> implements EndpointStateService {
	
	private static Log logger = Log.get();
	
}
