package com.nis.modules.sys.job;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONPath;
import com.nis.common.utils.Constant;
import com.nis.modules.report.job.ReportJobManager;
import com.nis.modules.sys.service.SysBackupService;
import com.nis.modules.sys.service.SysConfService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Component
@DisallowConcurrentExecution
public class SysBackJob extends QuartzJobBean {

    private static final Log log = Log.get();

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SysBackupService sysBackupService;

    @Autowired
    private SysConfService sysConfService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        // 检查 haMode
        String serverId = (String) redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
        if (!StrUtil.equals(serverId, Constant.SERVER_ID)) return;

        JobKey key = context.getJobDetail().getKey();
        log.info(String.format("Job name: %s, Job group: %s", key.getName(), key.getGroup()));

        // JOB 每天都执行，根据匹配规则，检查本次是否执行
        String configJsonStr = sysConfService.getValue("backup_config");
        String scheduleType = JSONPath.read(configJsonStr, "schedule.type").toString();
        String stime = JSONPath.read(configJsonStr, "schedule.stime").toString();
        String repeatStr = JSONPath.read(configJsonStr, "schedule.repeat").toString();
        String nums = JSONPath.read(configJsonStr, "schedule.nums").toString().replaceAll("\\[|\\]", "");

        Boolean isRun = ReportJobManager.checkJobIsRun(scheduleType, stime, repeatStr, nums);
        if (isRun) {
            log.info("System data backup begin ...");
            try {
                // 该接口内实现了 retention 保存文件个数
                sysBackupService.backupData("System scheduled backup");
            } catch (Exception e) {
                log.error("System data backup error", e);
            }
            log.info("System data backup finished.");
        } else {
            log.info("System data backup is not executed, this time does't match the config schedule");
        }
    }

}
