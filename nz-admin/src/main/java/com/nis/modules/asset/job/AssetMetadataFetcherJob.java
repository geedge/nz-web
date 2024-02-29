package com.nis.modules.asset.job;

import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetFeatureService;
import com.nis.modules.election.dao.LeaderElectionDao;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.List;

@DisallowConcurrentExecution
public class AssetMetadataFetcherJob extends QuartzJobBean {

    private static Log log = Log.get();

    @Autowired
    private AssetAssetService assetService;

    @Autowired
    private AssetFeatureService assetFeatureService;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        Thread.currentThread().setName("AssetMetadataFetcherJob");

        LeaderElectionDao dao = LeaderElectionDao.getInstance();
        boolean isLeader = dao.isLeader(Constant.SYS_HA_LOCK, Constant.SERVER_ID);
        log.info("[AssetMetadataFetcherJob] [Get HA_LOCK] [isLeader: {}]", isLeader);
        if (Tool.BooleanUtil.negate(isLeader)) return;

        log.info("[AssetMetadataFetcherJob] [begin]");

        StopWatch sw = new StopWatch();
        sw.start();
        try {
            List<AssetAsset> assetList = assetService.list(new LambdaQueryWrapper<AssetAsset>().in(AssetAsset::getId, Tool.ListUtil.of(3107,123)));
            assetList.forEach(asset -> {
                Thread.ofVirtual().start(() -> {
                    assetFeatureService.collectAssetBasicInfo(asset.getId());
                });
            });
        } catch (Exception e) {
            log.error(e, "[AssetMetadataFetcherJob] [error]");
        } finally {
            sw.stop();
        }

        log.info("[AssetMetadataFetcherJob] [finshed] [Run Time: {}]", sw.toString());
    }
}
