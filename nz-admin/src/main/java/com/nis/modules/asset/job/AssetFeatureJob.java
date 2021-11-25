package com.nis.modules.asset.job;

import java.util.List;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.alibaba.fastjson.JSON;
import com.nis.common.utils.Constant;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.service.AssetAssetService;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;

@DisallowConcurrentExecution
public class AssetFeatureJob extends QuartzJobBean {
	private Log logger= Log.get();
	@Autowired
	private AssetAssetService assetAssetService;
	
	@Autowired
	private AssetConcurrent assetConcurrent;

	@Autowired
	private RedisTemplate redisTemplate;
	
	@Override
	protected void executeInternal(JobExecutionContext context)  {
		Thread.currentThread().setName("AssetFeatureJob");
		// 检查 haMode
    	String serverId = (String) redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
        if(!StrUtil.equals(serverId, Constant.SERVER_ID)) return;
		
		logger.info("--------------------AssetFeatureJob starting--------------------");
		// 查询所有asset信息列表
		List<AssetAsset> assets = assetAssetService.list();
		logger.info("AssetFeatureJob asset infos :{}",JSON.toJSON(assets));
	    // 创建多线程处理
		for(AssetAsset asset : assets) {
			try {
				assetConcurrent.execute(asset.getId());
			} catch (RuntimeException e) {
				logger.error(e);
			}
		}
		logger.info("---------------AssetFeatureJob finish-------------------");
	}

}
