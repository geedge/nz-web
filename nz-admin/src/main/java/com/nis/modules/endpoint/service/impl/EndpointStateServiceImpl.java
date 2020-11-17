package com.nis.modules.endpoint.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.endpoint.dao.EndpointStateDao;
import com.nis.modules.endpoint.entity.EndpointState;
import com.nis.modules.endpoint.service.EndpointStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service("endpointSateService")
public class EndpointStateServiceImpl  extends ServiceImpl<EndpointStateDao, EndpointState> implements EndpointStateService {
	
	private static Logger logger = LoggerFactory.getLogger(EndpointStateServiceImpl.class);
	
}
