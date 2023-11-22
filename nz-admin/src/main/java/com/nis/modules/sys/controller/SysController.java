package com.nis.modules.sys.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONPath;
import com.nis.common.utils.Constant;
import com.nis.common.utils.R;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.backend.SNMPTrapReceiver;
import com.nis.modules.report.job.ReportJobManager;
import com.nis.modules.sys.job.SysBackJobManager;
import com.nis.modules.sys.service.SysConfService;
import com.nis.modules.sys.service.SysService;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.Charset;
import java.util.Map;

@RestController
@RequestMapping("/sys")
public class SysController {

    private static final Log log = Log.get();

    @Autowired
    private SysService sysService;

    @Autowired
    private SNMPTrapReceiver trapReceiver;

    @Autowired
    private SysConfService sysConfService;

    @Autowired
    private ReportJobManager reportJobManager;

    @Autowired
    private SysBackJobManager sysBackJobManager;

    @GetMapping("/appearance")
    public R queryAppearance() {
        Map<String, String> result = sysService.queryAppearance();
        return R.ok(result);
    }


    /**
     * 消息通知接口
     *
     * @param params
     * @return
     */
    @PostMapping("/component/message")
    public R processSystemMessage(@RequestBody Map<String, Object> params) {
        String topicName = Tool.MapUtil.getStr(params, "topic");
        Object messageObj = Tool.MapUtil.get(params, "message", Object.class, "");
        log.info("[processSystemMessage] [topicName: {}]", topicName);

        Constant.SysTopicNameEnum instance = Constant.SysTopicNameEnum.getInstanceByName(topicName);
        if (ObjectUtil.isNull(instance)) {
            log.warn("[processSystemMessage] [topicName is invalid] [topicName: {}]", topicName);
        } else {
            switch (instance) {
                case SYS_RESTORE -> {
                    String serverId = StrUtil.str(messageObj, Charset.forName("UTF-8"));
                    sysService.processSysRestore(serverId);
                }
                case SNMP_TRAP_PORT -> {
                    // 监听到 snmp 端口变化
                    try {
                        String snmpPort = StrUtil.str(messageObj, Charset.forName("UTF-8"));
                        trapReceiver.restart(Integer.valueOf(snmpPort));
                    } catch (Exception e) {
                        log.error(e, "snmp trap receiver restart error");
                    }
                }
                case DASHBOARD_REPORT -> {
                    log.info("Report consumer message, report begin ...");
                    log.info("consumer message :{}", messageObj);

                    String dashboardReportConfJsonStr = StrUtil.str(messageObj, Charset.forName("UTF-8"));

                    String reportEnable = JSONPath.read(dashboardReportConfJsonStr, "report.enable").toString();
                    String dashboardId = JSONPath.read(dashboardReportConfJsonStr, "dashboardId").toString();
                    if (Tool.StrUtil.equalsIgnoreCase("true", StrUtil.nullToDefault(reportEnable, "false"))) {
                        try {
                            reportJobManager.delJobByName(dashboardId);
                            reportJobManager.createReportJob(dashboardReportConfJsonStr, dashboardId);
                            log.info("createReportJob success.");
                        } catch (SchedulerException e) {
                            log.error(e, "report job create or update error,dashboardId: {}", dashboardId);
                        }
                    } else {
                        try {
                            reportJobManager.delJobByName(dashboardId);
                        } catch (SchedulerException e) {
                            log.error(e, "delete report job error, dashboardId:{}", dashboardId);
                        }
                    }
                    log.info("Report consumer message, report finished");
                }
                case SYS_BACKUP -> {
                    log.info("Sys-backup consumer message begin ...");
                    log.info("consumer message :{}", messageObj);

                    String sysBackupJobConfJsonStr = StrUtil.str(messageObj, Charset.forName("UTF-8"));

                    // 1 开启 0 关闭
                    String state = JSONPath.read(sysBackupJobConfJsonStr, "state").toString();

                    if (Tool.StrUtil.equalsIgnoreCase("1", StrUtil.nullToDefault(state, "1"))) {
                        try {
                            sysBackJobManager.delJob();
                            sysBackJobManager.createSysBackJob(sysBackupJobConfJsonStr);
                        } catch (SchedulerException e) {
                            log.error(e, "report job create or update error");
                        }
                    } else {
                        try {
                            sysBackJobManager.delJob();
                        } catch (SchedulerException e) {
                            log.error(e, "delete sys-backup job error");
                        }
                    }
                    log.info("Sys-backup consumer message, finished");
                }
                case SYS_CONFIG_RELOAD -> sysConfService.reloadSysConfigToCache();
            }
        }
        return R.ok();
    }

}
