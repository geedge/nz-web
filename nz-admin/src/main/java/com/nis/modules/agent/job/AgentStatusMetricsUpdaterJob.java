package com.nis.modules.agent.job;

import cn.hutool.log.Log;
import com.nis.common.protobuf.Types;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.AgentService;
import com.nis.modules.alert.service.RecordRuleService;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.election.dao.LeaderElectionDao;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * nz_agent_status metrics 定时更新任务
 */
@DisallowConcurrentExecution
public class AgentStatusMetricsUpdaterJob extends QuartzJobBean {

    private static final Log log = Log.get();

    @Autowired
    private DcService dcService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private RecordRuleService recordRuleService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Thread.currentThread().setName("AgentStatusMetricsUpdaterJob");

        LeaderElectionDao leaderElectionDao = LeaderElectionDao.getInstance();
        boolean isLeader = leaderElectionDao.isLeader(Constant.SYS_HA_LOCK, Constant.SERVER_ID);
        log.info("[AgentStatusMetricsUpdaterJob] [Get HA_LOCK] [isLeader: {}]", isLeader);
        if (Tool.BooleanUtil.negate(isLeader)) return;

        log.info("[AgentStatusMetricsUpdaterJob] [begin]");
        StopWatch sw = new StopWatch();
        sw.start();
        try {
            this.agentStatusMetricsUpdater();
        } catch (Exception e) {
            log.error(e, "[AgentStatusMetricsUpdaterJob] [error]");
        } finally {
            sw.stop();
        }

        log.info("[AgentStatusMetricsUpdaterJob] [finshed] [Run Time: {}]", sw.toString());
    }


    /**
     * agentStatusMetricsUpdater
     */
    private void agentStatusMetricsUpdater() {
        /**
         * nz_agent_status
         */
        List<Agent> agentList = agentService.list();
        List<Dc> dcList = dcService.list();
        Map<Integer, String> dcIdAndNameMap = dcList.stream().collect(Collectors.toMap(Dc::getId, Dc::getName));

        List<Types.TimeSeries> result = Tool.ListUtil.list(false);
        for (int i = 0; i < agentList.size(); i++) {
            Agent agent = agentList.get(i);
            String dcName = Tool.MapUtil.getStr(dcIdAndNameMap, agent.getDcId(), "null");
            Types.TimeSeries timeSeries = this.buildTimeSeries(agent, dcName);
            result.add(timeSeries);
        }

        log.info("[agentStatusMetricsUpdater] [nz_agent_status TimeSeries] [size: {}]", Tool.CollUtil.size(result));
        recordRuleService.rightPushRecordToCache(result);
    }

    /**
     * build TimeSeries
     *
     * @param agent
     * @param dcName
     * @return
     */
    private Types.TimeSeries buildTimeSeries(Agent agent, String dcName) {
        List<Types.Label> labelList = Tool.ListUtil.list(true);

        labelList.add(Types.Label.newBuilder().setName("datacenter").setValue(dcName).build());
        labelList.add(Types.Label.newBuilder().setName("type").setValue(Constant.AgentType.getAgentType(agent.getType()).getName()).build());
        labelList.add(Types.Label.newBuilder().setName("host").setValue(agent.getHost()).build());
        labelList.add(Types.Label.newBuilder().setName("port").setValue(agent.getPort() + "").build());
        labelList.add(Types.Label.newBuilder().setName("agent_name").setValue(agent.getName()).build());
        labelList.add(Types.Label.newBuilder().setName("application").setValue("nz-web").build());

        // __name__ nz_agent_status
        labelList.add(Types.Label.newBuilder().setName("__name__").setValue("nz_agent_status").build());

        Types.Sample sample = Types.Sample.newBuilder()
                .setTimestamp(System.currentTimeMillis())
                .setValue(agent.getStatus())
                .build();

        Types.TimeSeries timeSeries = Types.TimeSeries.newBuilder().addAllLabels(labelList).addSamples(sample).build();
        return timeSeries;
    }
}
