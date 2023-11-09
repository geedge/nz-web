package com.nis.modules.alert.job;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.dao.AlertMessageHistoryPartitionManagementDao;
import com.nis.modules.election.dao.LeaderElectionDao;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.*;

/**
 * alert_message_history 分区定时任务
 * 1. 首次执行时，创建 未来 20 天得分区
 * 2. 之后每次执行，创建 1 个分区
 * 3. Partition col = start_at
 * 4. Partition name = p20220201 Partition expr = LESS THAN ( todays(`start_at`) )
 * 5. 查询表分区语句 SELECT PARTITION_NAME,TABLE_ROWS FROM INFORMATION_SCHEMA.PARTITIONS  WHERE TABLE_NAME = 'alert_message_history' order by PARTITION_NAME ; | show create table alert_message_history;
 *
 * @author shizhendong
 * @version 3.2
 * @see org.springframework.scheduling.quartz.QuartzJobBean
 */
@DisallowConcurrentExecution
public class AlertMessageHistoryPartitionManagementJob extends QuartzJobBean {

    private static Log log = Log.get();

    public static String ALERT_RTITIONNAME_PREFIX = "p";

    @Autowired
    private AlertMessageHistoryPartitionManagementDao alertMessageHistoryPartitionManagementDao;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Thread.currentThread().setName("AlertMessageHistoryPartitionManagementJob");

        // 检查 HA_LOCK
        LeaderElectionDao dao = LeaderElectionDao.getInstance();
        boolean isLeader = dao.isLeader();
        log.info("[AlertMessageHistoryPartitionManagementJob] [Get HA_LOCK] [isLeader: {}]", isLeader);
        if (BooleanUtil.negate(isLeader)) return;

        StopWatch sw = new StopWatch();
        sw.start();
        try {
            this.partitionManagement();
        } catch (RuntimeException e) {
            log.error("AlertMessageHistoryPartitionManagementJob 异常", e);
        } finally {
            sw.stop();
        }
        log.info("AlertMessageHistoryPartitionManagementJob Run Time: {}", sw.toString());
    }

    private void partitionManagement() throws RuntimeException {
        // 1. 第一次创建 未来 30 天得分区
        // 判断表是否存在分区
        List<String> partNamesForTable = alertMessageHistoryPartitionManagementDao.getPartitionNamesByTabelName("alert_message_history");
        log.info(String.format("AlertMessageHistory Partition current size: %s ,range: [%s]", partNamesForTable.size(), Tool.CollectionUtil.isEmpty(partNamesForTable) ? "" : partNamesForTable.get(0) + " - " + partNamesForTable.get(partNamesForTable.size() - 1)));
        // 第一次提前创建 30 个分区
        int intervals = 30;
        if (Tool.CollectionUtil.isEmpty(partNamesForTable)) {
            log.info("AlertMessageHistory Partition does not exist, create before " + intervals + " day partition");

            List<String> intervalsDaysBeforeList = new ArrayList<>();
            for (int i = 0; i < intervals; i++) {
                intervalsDaysBeforeList.add(getPastOrFutureDays(i, true));
            }

            // 分区规则要按照时间顺序排序
            intervalsDaysBeforeList.sort((t1, t2) -> Tool.DateUtil.parseDate(t1).compareTo(Tool.DateUtil.parseDate(t2)));

            Map<String, String> partitionNameAndExprMap = new LinkedHashMap<>();
            for (String dateExpr : intervalsDaysBeforeList) {
                String partitionName = AlertMessageHistoryPartitionManagementJob.ALERT_RTITIONNAME_PREFIX + dateExpr.replaceAll("-", "");
                partitionNameAndExprMap.put(partitionName, dateExpr);
            }

            // 批量创建表分区
            alertMessageHistoryPartitionManagementDao.createIntervalsPartitions(partitionNameAndExprMap, "alert_message_history", "start_at");
            log.info("AlertMessageHistory Partition create done, partition name and expr info: " + JSONObject.toJSONString(partitionNameAndExprMap));
            return;
        }

        // 2. 以后每天创建一个分区
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) + intervals);
        String partitionExpr = Tool.DateUtil.format(calendar.getTime(), "yyyy-MM-dd");
        String partitionName = AlertMessageHistoryPartitionManagementJob.ALERT_RTITIONNAME_PREFIX + partitionExpr.replaceAll("-", "");
        log.info(String.format("AlertMessageHistory add partition name: %s expression: %s ", partitionName, partitionExpr));

        // 先查询是否存在分区
        List<String> partitionNames = alertMessageHistoryPartitionManagementDao.getPartitionNamesByTabelName("alert_message_history", partitionName);
        if (!partitionNames.contains(partitionName)) {
            alertMessageHistoryPartitionManagementDao.addPartitions("alert_message_history", partitionName, partitionExpr);
            log.info(String.format("AlertMessageHistory add partition success, name: %s expression: %s ", partitionName, partitionExpr));
        } else {
            log.info(String.format("AlertMessageHistory add partition failed, the partition already exists, name: %s expression: %s ", partitionName, partitionExpr));
        }
    }

    /**
     * @param i 第几天
     * @param b true未来 false 过去
     * @return
     */
    private static String getPastOrFutureDays(int i, boolean b) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) + 1);
        if (b) {
            calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) + i);
        } else {
            calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) - i);
        }
        Date today = calendar.getTime();
        String result = Tool.DateUtil.format(today, "yyyy-MM-dd");
        return result;
    }

}
