package com.nis.modules.terminal.job;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.log.Log;
import com.jcraft.jsch.Session;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.election.dao.LeaderElectionDao;
import com.nis.modules.sys.service.SysConfService;
import com.nis.modules.terminal.backend.TerminalSession;
import org.apache.commons.net.telnet.TelnetClient;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 清理未实际使用的 ssh/telnet 连接
 * 以超过最后数据交互时间 30 分钟为准 （sys_config.session_timeout）
 */
@DisallowConcurrentExecution
public class CleanNoRealUseTerminalConnJob extends QuartzJobBean {

    private final Log log = Log.get();

    @Autowired
    private SysConfService sysConfService;

    @SuppressWarnings("unchecked")
    @Override
    protected void executeInternal(JobExecutionContext arg0) throws JobExecutionException {
        Thread.currentThread().setName("Clean No Real Use Connection Job");
        // 检查 HA_LOCK
        LeaderElectionDao dao = LeaderElectionDao.getInstance();
        boolean isLeader = dao.isLeader(Constant.SYS_HA_LOCK, Constant.SERVER_ID);
        log.info("[CleanNoRealUseTerminalConnJob] [Get HA_LOCK] [isLeader: {}]", isLeader);
        if (BooleanUtil.negate(isLeader)) return;

        log.info("clean no real use ssh/telnet connection job start...");
        StopWatch sw = StopWatch.create("clean no real use ssh/telnet connection job");
        sw.start();
        try {
            // 清理未实际使用的 ssh/telnet 连接
            // 以超过最后数据交互时间 30 分钟为准 （sys_config.session_timeout）
            this.cleanNoRealUseConnection();
        } catch (Exception e) {
            log.error(e, "clean no real use ssh/telnet connection job error");
        } finally {
            sw.stop();
        }
        log.info("clean no real use ssh/telnet connection job Run Time: {}", sw.prettyPrint());
    }

    /**
     * 清理未实际使用的 ssh/telnet 连接
     * 以超过最后数据交互时间 30 分钟为准 （sys_config.session_timeout）
     */
    private void cleanNoRealUseConnection() {
        if (Tool.MapUtil.isEmpty(TerminalSession.TERMINAL_CONNECTED_LAST_ACTIVE_MAP)) {
            log.warn("current no connection info, clean no real use ssh/telnet connection finshed. ");
            return;
        }

        // system session timeout (minute)
        String sessionTimeoutConfStr = sysConfService.getValue(Constant.SYSCONFIG_SESSION_TIMEOUT_KEY);
        Integer sessionTimeout = Integer.valueOf(sessionTimeoutConfStr);

        long currentSeconds = Tool.DateUtil.currentSeconds();

        List<Map.Entry<String, Long>> entries = TerminalSession.TERMINAL_CONNECTED_LAST_ACTIVE_MAP.entrySet()
                .stream()
                .filter(entry -> {
                    Long lastActiveTime = entry.getValue();
                    return currentSeconds - lastActiveTime > (sessionTimeout * 60);
                }).collect(Collectors.toList());

        for (Map.Entry<String, Long> entry : entries) {
            String uuid = entry.getKey();
            Map<Object, Object> connObjMap = TerminalSession.TERMINAL_CONNECTED_CACHE.get(uuid);
            if (Tool.MapUtil.isEmpty(connObjMap)) {
                continue;
            }

            log.info("disconnect no real use ssh/telnet connection. uuid: {}", uuid);
            Object obj = connObjMap.get(TerminalSession.CONNECTION_CONNECTION_ENTITY_KEY);
            try {
                if (obj instanceof Session) {
                    Session session = (Session) obj;
                    session.disconnect();
                }
                if (obj instanceof TelnetClient) {
                    TelnetClient telnetClient = (TelnetClient) obj;
                    telnetClient.disconnect();
                }
            } catch (IOException e) {
                log.error(e, "disconnect no real use ssh/telnet connection error. uuid: {}", uuid);
            } finally {
                // remove cache
                TerminalSession.TERMINAL_CONNECTED_CACHE.remove(uuid);
                TerminalSession.TERMINAL_CONNECTED_LAST_ACTIVE_MAP.remove(uuid);

                // monitor
                TerminalSession.TERMINAL_MONITORS_MAPPING.remove(uuid);

                // socket session
                WebSocketSession socketSession = TerminalSession.findSession(uuid);
                if (ToolUtil.isNotEmpty(socketSession)) {
                    TerminalSession.remove(socketSession);
                }

                // session attribute info
                TerminalSession.TERMINAL_ATTRIBUTE_INFO.remove(uuid);

                // session cmd info
                TerminalSession.TERMINAL_CMD_INFO.remove(uuid);
            }
        }
    }

}
