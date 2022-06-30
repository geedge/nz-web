package com.nis.modules.report.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONPath;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.report.job.ReportJobManager;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Report Consumer
 */
public class ReportConsumerListener implements MessageListener {

    private static Log log = Log.get();

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ReportJobManager reportJobManager;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        doBusiness(message);
    }

    /**
     * Consumer message
     *
     * @param message
     */
    public void doBusiness(Message message) {
        // 检查 haMode
        String serverId = (String) redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
        if (!StrUtil.equals(serverId, Constant.SERVER_ID)) return;

        log.info("Report consumer message, report  begin ...");

        Object consumerMsg = redisTemplate.getValueSerializer().deserialize(message.getBody());
        log.info("consumer message :{}", consumerMsg.toString());

        String consumerMsgJsonStr = consumerMsg.toString();
        String reportEnable = JSONPath.read(consumerMsgJsonStr, "report.enable").toString();
        String panelId = JSONPath.read(consumerMsgJsonStr, "panelId").toString();

        if (Tool.StrUtil.equalsIgnoreCase("true", StrUtil.nullToDefault(reportEnable, "false"))) {
            try {
                //  先删后增
                reportJobManager.findJobAndDelByGroup(panelId);

                // 创建任务
                reportJobManager.createReportJob(consumerMsgJsonStr, panelId);
                log.info("createReportJob success.");
            } catch (SchedulerException e) {
                log.error("report job create or update error,panelId:{}", e, panelId);
            }
        } else {
            try {
                //  先删后增
                reportJobManager.findJobAndDelByGroup(panelId);
            } catch (SchedulerException e) {
                log.error("delete report job error, panelId:{}", panelId, e);
            }
        }
        log.info("Report consumer message, report finished");
    }

}
