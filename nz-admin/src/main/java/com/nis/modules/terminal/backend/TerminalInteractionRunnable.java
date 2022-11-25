package com.nis.modules.terminal.backend;

import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.DigestUtils;
import com.nis.common.utils.Tool;
import com.nis.modules.terminal.entity.TerminalRecord;
import com.nis.modules.terminal.entity.TerminalSessionEntity;
import com.nis.modules.terminal.service.TerminalRecordService;
import com.nis.modules.terminal.service.TerminalSessionService;
import com.nis.modules.terminal.utils.TerminalConstant;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;

public class TerminalInteractionRunnable extends Thread {

    private static final Log log = Log.get();

    private TerminalClient client;

    public TerminalInteractionRunnable(TerminalClient client) {
        this.client = client;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024 * 4];
        String clientId = client.getClientId();
        WebSocketSession webSocketSession = client.getWebSocketSession();
        try {
            while (webSocketSession != null && webSocketSession.isOpen()) {
                int size = client.getInputStream().read(buffer);
                if (size == Tool.StrUtil.INDEX_NOT_FOUND) {
                    log.warn("no more data because the end of the stream has been reached. clientid: {}", clientId);
                    return;
                }
                // update last active time
                // connected last active time
                TerminalSession.TERMINAL_CONNECTED_LAST_ACTIVE_MAP.put(clientId, Tool.DateUtil.currentSeconds());

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < size; i++) {
                    char chr = (char) (buffer[i] & 0xff);
                    sb.append(chr);
                }
                // 取到 linux 远程机器输出的信息发送给前端
                String message = sb.toString();
                message = Tool.StrUtil.str(message.getBytes(DigestUtils.getEncoding(message)), "UTF-8");

                // send Message To Monitor Terminal
                client.sendMessageToMonitorTerminal(clientId, message);

                // save Or Append Terminal Record Message
                File tempFile = Tool.FileUtil.file(Tool.StrUtil.concat(true, Constant.TEMP_PATH, File.separator, clientId, ".log"));
                client.saveOrAppendTerminalRecordMessage(tempFile, message);

                // 处理服务器方向键以及 tab 操作返回的指令信息 便于做日志记录
                client.processCommand(message);

                if (Tool.StrUtil.equals("logout", message.replaceAll("\r\n", ""))) {
                    // logout prompt information
                    String unloginInfo = this.getLogoutPromptByExitStatus(clientId, TerminalConstant.SessionStatus.OVER.getValue());
                    webSocketSession.sendMessage(new TextMessage(unloginInfo));

                    // save Terminal Record When Logout
                    this.saveTerminalRecordWhenLogout(clientId, tempFile);
                } else {
                    // send message to terminal
                    webSocketSession.sendMessage(new TextMessage(message));
                }

                // after enter
                if (client.getSendEnter()) {
                    String trimMessage = message.replaceAll("\r\n", "");
                    if (Tool.StrUtil.isNotEmpty(trimMessage)) {
                        // 重置下标
                        StringBuffer sendBuffer = client.getSendBuffer();
                        if (Tool.BooleanUtil.and(
                                Tool.StrUtil.equals("exit", Tool.StrUtil.trim(sendBuffer)),
                                Tool.StrUtil.equals("logout", trimMessage))) {
                            // disconnect
                            client.disconnect();
                        }
                    }
                    // 重置下标
                    client.getSendBuffer().setLength(0);
                }
            }
        } catch (IOException e) {
            log.error(e, "Terminal error.");
            TerminalSession.remove(webSocketSession);
        }
    }

    /**
     * save Terminal Record When Logout
     *
     * @param clientId
     * @param tempFile
     * @throws IOException
     */
    private void saveTerminalRecordWhenLogout(String clientId, File tempFile) {
        try {
            TerminalRecordService terminalRecordService = (TerminalRecordService) TerminalSession.CLIENT_ATTRIBUTE_INFO.get("terminalRecordService");

            TerminalRecord terminalRecord = new TerminalRecord();
            terminalRecord.setUuid(clientId);
            terminalRecord.setContent(Files.readAllBytes(tempFile.toPath()));
            // last Record
            TerminalRecord lastRecord = terminalRecordService.getOne(new LambdaQueryWrapper<TerminalRecord>().eq(TerminalRecord::getUuid, clientId).orderByDesc(TerminalRecord::getId).last("limit 0,1"));
            // first time to save db
            if (lastRecord == null) {
                terminalRecord.setStartTime(0);
            } else {
                // start time == end of the last time
                terminalRecord.setStartTime(lastRecord.getEndTime());
            }
            long terminalSessionStartTime = (long) TerminalSession.getTerminalAttributeInfo(clientId).get("terminalSessionStartTime");
            terminalRecord.setEndTime((int) (System.currentTimeMillis() - terminalSessionStartTime));
            terminalRecordService.save(terminalRecord);
        } catch (Exception e) {
            log.error(e, "Terminal connection has been closed, Terminal Record file content record failed. clientid: {}, File info: {}", clientId, tempFile.toString());
        } finally {
            Tool.FileUtil.del(tempFile);
        }
    }


    /**
     * logout prompt information
     *
     * @param clientId
     * @param exitStatus
     * @return
     */
    public String getLogoutPromptByExitStatus(String clientId, Integer exitStatus) {
        /**
         * session 已经结束，更新状态，设置结束时间
         */
        TerminalSessionEntity sessionEntity = (TerminalSessionEntity) TerminalSession.getTerminalAttributeInfo(clientId).get("terminalSessionEntity");
        if (sessionEntity.getStartTime() == null) {
            sessionEntity.setStartTime(new Date());
        }
        sessionEntity.setStatus(exitStatus);
        sessionEntity.setEndTime(new Date());
        TerminalSessionService terminalSessionService = (TerminalSessionService) TerminalSession.CLIENT_ATTRIBUTE_INFO.get("terminalSessionService");
        terminalSessionService.saveOrUpdate(sessionEntity);

        String logoutPromptStr = Tool.StrUtil.concat(true,
                "\r\nlogout\r\n",
                "Connection closed \r\n");
        return logoutPromptStr;
    }
}
