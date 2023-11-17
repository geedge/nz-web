package com.nis.modules.election.job;

import cn.hutool.log.Log;
import com.nis.common.utils.Constant;
import com.nis.modules.election.dao.LeaderElectionDao;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.concurrent.TimeUnit;

@DisallowConcurrentExecution
public class HaJob extends QuartzJobBean {

    private static final Log log = Log.get();

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Thread.currentThread().setName("HaJob");
        try {
            LeaderElectionDao dao = LeaderElectionDao.getInstance();
            // ha lock
            dao.attemptLeadership(Constant.SYS_HA_LOCK, Constant.SERVER_ID, TimeUnit.SECONDS.toMillis(10));

            if (log.isDebugEnabled()) {
                log.debug("[HaJob] [current server] [isLeader: {}]", dao.isLeader(Constant.SYS_HA_LOCK, Constant.SERVER_ID));
            }
        } catch (Exception e) {
            log.error(e, "[HaJob] [error]");
        }
    }

}
