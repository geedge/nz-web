package com.nis.modules.asset.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.nis.common.utils.DateUtil;
import com.nis.modules.asset.service.AssetService;

@Component
public class AssetConcurrent {
	
	private Logger logger = LoggerFactory.getLogger(AssetConcurrent.class);
	
	@Autowired
	private AssetService assetService;
	
	@Async
	public void execute(Integer assetId) {
		// 异步执行采集资产信息任务
		logger.info("AssetConcurrent start assetId :{} ",assetId);
		
		try {
			assetService.collectData(assetId);
		} catch (Exception e) {
			logger.error("AssetConcurrent fail : ",e);
		}
		
	}
	
}
