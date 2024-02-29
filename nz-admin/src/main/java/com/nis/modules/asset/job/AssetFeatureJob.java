/*
package com.nis.modules.asset.job;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.nis.common.utils.Constant;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.election.dao.LeaderElectionDao;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.List;

@DisallowConcurrentExecution
public class AssetFeatureJob extends QuartzJobBean {
	private Log logger= Log.get();
	@Autowired
	private AssetAssetService assetAssetService;
	
	@Autowired
	private AssetConcurrent assetConcurrent;

	@Override
	protected void executeInternal(JobExecutionContext context)  {
		Thread.currentThread().setName("AssetFeatureJob");

		// 检查 HA_LOCK
		LeaderElectionDao dao = LeaderElectionDao.getInstance();
		boolean isLeader = dao.isLeader(Constant.SYS_HA_LOCK, Constant.SERVER_ID);
		logger.info("[AssetFeatureJob] [Get HA_LOCK] [isLeader: {}]", isLeader);
		if (BooleanUtil.negate(isLeader)) return;
		
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
*/
