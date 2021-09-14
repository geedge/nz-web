package com.nis.modules.sys.job;

import cn.hutool.core.date.DateTime;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.entity.AlertRuleEvalLog;
import com.nis.modules.alert.entity.AlertRuleSchedLog;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.alert.service.AlertRuleEvalLogService;
import com.nis.modules.alert.service.AlertRuleSchedLogService;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.terminal.service.TerminalSessionService;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * 系统清理任务
 * 1. terminal log，默认保留 365天.
 * 2. alert_rule_eval_log，默认保留 30天
 * 3. alert_rule_sched_log，默认保留 30天
 * 4. alert_message_history ，默认不清理
 */
@DisallowConcurrentExecution
public class CleanJob extends QuartzJobBean {

    private Log log = Log.get();

    @Autowired
    private TerminalSessionService terminalSessionService;

    @Autowired
    private AlertRuleEvalLogService alertRuleEvalLogService;

    @Autowired
    private AlertRuleSchedLogService alertRuleSchedLogService;

    @Autowired
    private AlertMessageService alertMessageService;

    @Autowired
    private SysConfigService sysConfigService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        DateTime now = Tool.DateUtil.date();
        StopWatch sw = new StopWatch();

        //  terminal_*
        String terminalRecordLocalRetention = sysConfigService.getValue("terminal_record_local_retention");
        terminalRecordLocalRetention = Tool.StrUtil.emptyToDefault(terminalRecordLocalRetention, "365");
        DateTime startCleanTerminalTime = Tool.DateUtil.offsetDay(now, -Integer.valueOf(terminalRecordLocalRetention));
        sw.start();
        try {
            log.info("start clean terminal data");
            terminalSessionService.cleanExpiredDataByTime(startCleanTerminalTime);
        } catch (Exception e) {
            log.error("clean terminal data error", e);
        } finally {
            sw.stop();
        }
        log.info("clean terminal data Run Time: {}", sw.toString());

        // alert_rule_eval_log
        String ruleEvalLogLocalRetention = sysConfigService.getValue("alert_rule_eval_log_local_retention");
        ruleEvalLogLocalRetention = Tool.StrUtil.emptyToDefault(ruleEvalLogLocalRetention, "30");
        DateTime startCleanRuleEvalLogTime = Tool.DateUtil.offsetDay(now, -Integer.valueOf(ruleEvalLogLocalRetention));
        sw.reset();
        sw.start();
        try {
            log.info("start clean alert rule eval log data");
            alertRuleEvalLogService.remove(new LambdaQueryWrapper<AlertRuleEvalLog>().le(AlertRuleEvalLog::getStts, startCleanRuleEvalLogTime.getTime()));
        } catch (Exception e) {
            log.error("clean alert rule eval log data error", e);
        } finally {
            sw.stop();
        }
        log.info("clean alert rule eval log data Run Time: {}", sw.toString());

        // alert_rule_sched_log
        String ruleSchedLogLocalRetention = sysConfigService.getValue("alert_rule_sched_log_local_retention");
        ruleSchedLogLocalRetention = Tool.StrUtil.emptyToDefault(ruleSchedLogLocalRetention, "30");
        DateTime startCleanRuleSchedLogTime = Tool.DateUtil.offsetDay(now, -Integer.valueOf(ruleSchedLogLocalRetention));
        sw.reset();
        sw.start();
        try {
            log.info("start clean alert rule sched log data");
            alertRuleSchedLogService.remove(new LambdaQueryWrapper<AlertRuleSchedLog>().le(AlertRuleSchedLog::getTs, startCleanRuleSchedLogTime.getTime()));
        } catch (Exception e) {
            log.error("clean alert rule sched log data error", e);
        } finally {
            sw.stop();
        }
        log.info("clean alert rule sched log data Run Time: {}", sw.toString());

        // 告警消息清理开关
        String cleanAlertMessageEnable = sysConfigService.getValue("clean_alert_message_enable");
        cleanAlertMessageEnable = Tool.StrUtil.emptyToDefault(cleanAlertMessageEnable, "0");
        if (Tool.StrUtil.equals("1", cleanAlertMessageEnable)) {
            String alertMessageLocalRetention = sysConfigService.getValue("alert_message_local_retention");
            alertMessageLocalRetention = Tool.StrUtil.emptyToDefault(alertMessageLocalRetention, "365");
            DateTime startCleanAlertMessageTime = Tool.DateUtil.offsetDay(now, -Integer.valueOf(alertMessageLocalRetention));
            sw.reset();
            sw.start();
            try {
                log.info("start clean alert message data");
                alertMessageService.cleanExpiredDataByTime(startCleanAlertMessageTime);
            } catch (Exception e) {
                log.error("clean alert message data error", e);
            } finally {
                sw.stop();
            }
            log.info("clean alert message data Run Time: {}", sw.toString());
        }
    }
}
