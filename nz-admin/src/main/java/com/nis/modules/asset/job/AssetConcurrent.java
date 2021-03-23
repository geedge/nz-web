package com.nis.modules.asset.job;

import cn.hutool.log.Log;
import com.nis.common.utils.DateUtil;
import com.nis.modules.asset.service.AssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class AssetConcurrent {
	
	private Log logger = Log.get();
	
	@Autowired
	private AssetService assetService;
	
	@Async
	public void execute(Integer assetId) {
		// 异步执行采集资产信息任务
		logger.info("AssetConcurrent start assetId :{} ",assetId);
		
		try {
			assetService.collectData(assetId);
			logger.info("AssetConcurrent task finished time ："+DateUtil.getTime());
		} catch (RuntimeException e) {
			logger.error("AssetConcurrent fail assetId : {} info : {} ",assetId, e);
		}
		
	}
	
}
