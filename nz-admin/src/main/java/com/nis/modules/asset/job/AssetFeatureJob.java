package com.nis.modules.asset.job;

import java.util.List;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.alibaba.fastjson.JSON;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.service.AssetService;

public class AssetFeatureJob extends QuartzJobBean {
	private Logger logger= LoggerFactory.getLogger(AssetFeatureJob.class);
	@Autowired
	private AssetService assetService;
	
	@Autowired
	private AssetConcurrent assetConcurrent;
	
	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		
		logger.info("--------------------AssetFeatureJob starting--------------------");
		// 查询所有asset信息列表
		List<Asset> assets = assetService.list();
		logger.info("AssetFeatureJob asset infos :{}",JSON.toJSON(assets));
	    // 创建多线程处理
		for(Asset asset : assets) {
			try {
				assetConcurrent.execute(asset.getId());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		logger.info("---------------AssetFeatureJob finish-------------------");
	}

}
