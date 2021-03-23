package com.nis.modules.asset.job;

import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.sys.service.SysConfigService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.List;

@DisallowConcurrentExecution
public class AssetFeatureJob extends QuartzJobBean {
	private Log logger= Log.get();
	@Autowired
	private AssetService assetService;
	
	@Autowired
	private AssetConcurrent assetConcurrent;

	@Autowired
	private SysConfigService sysConfigService;
	
	@Override
	protected void executeInternal(JobExecutionContext context)  {

		// 检查 haMode
		boolean haMode = sysConfigService.checkHaMode();
		if (!haMode) return;
		
		logger.info("--------------------AssetFeatureJob starting--------------------");
		// 查询所有asset信息列表
		List<Asset> assets = assetService.list();
		logger.info("AssetFeatureJob asset infos :{}",JSON.toJSON(assets));
	    // 创建多线程处理
		for(Asset asset : assets) {
			try {
				assetConcurrent.execute(asset.getId());
			} catch (RuntimeException e) {
				logger.error(e);
			}
		}
		logger.info("---------------AssetFeatureJob finish-------------------");
	}

}
