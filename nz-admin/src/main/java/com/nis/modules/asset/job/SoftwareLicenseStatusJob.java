package com.nis.modules.asset.job;

import cn.hutool.log.Log;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.asset.service.SoftwareAssetService;
import com.nis.modules.election.dao.LeaderElectionDao;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

@DisallowConcurrentExecution
public class SoftwareLicenseStatusJob extends QuartzJobBean {

    private static final Log log = Log.get();

    @Autowired
    private SoftwareAssetService softwareAssetService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Thread.currentThread().setName("SoftwareLicenseStatusJob");

        LeaderElectionDao dao = LeaderElectionDao.getInstance();
        boolean isLeader = dao.isLeader(Constant.SYS_HA_LOCK, Constant.SERVER_ID);
        log.info("[SoftwareLicenseStatusJob] [Get HA_LOCK] [isLeader: {}]", isLeader);
        if (Tool.BooleanUtil.negate(isLeader)) return;

        log.info("[SoftwareLicenseStatusJob] [begin]");

        StopWatch sw = new StopWatch();
        sw.start();
        try {
            // update license status
            softwareAssetService.updateLicenseStatus(null);
        } catch (Exception e) {
            log.error(e, "[SoftwareLicenseStatusJob] [error]");
        } finally {
            sw.stop();
        }

        log.info("[SoftwareLicenseStatusJob] [finshed] [Run Time: {}]", sw.toString());
    }
}
