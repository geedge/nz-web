package com.nis.modules.election.job;

import cn.hutool.log.Log;
import com.nis.modules.election.dao.LeaderElectionDao;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

@DisallowConcurrentExecution
public class HaJob extends QuartzJobBean {

    private static final Log log = Log.get();

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Thread.currentThread().setName("HaJob");
        try {
            LeaderElectionDao dao = LeaderElectionDao.getInstance();
            dao.attemptLeadership();

            if (log.isDebugEnabled()) {
                log.debug("[HaJob] [current server] [isLeader: {}]", dao.isLeader());
            }

        } catch (Exception e) {
            log.error(e, "[HaJob] [error]");
        }
    }

}
