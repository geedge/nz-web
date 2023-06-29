/**
 * Copyright (c) 2015 The Nezha Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.nis.modules.terminal.backend;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jcraft.jsch.Session;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysConfService;
import com.nis.modules.sys.shiro.ShiroUtils;
import com.nis.modules.terminal.entity.TerminalCmd;
import com.nis.modules.terminal.entity.TerminalRecord;
import com.nis.modules.terminal.entity.TerminalSessionEntity;
import com.nis.modules.terminal.service.TerminalCmdService;
import com.nis.modules.terminal.service.TerminalRecordService;
import com.nis.modules.terminal.service.TerminalSessionService;
import com.nis.modules.terminal.utils.TerminalConstant;
import org.apache.commons.net.telnet.TelnetClient;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.File;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.nis.common.utils.CommonUtils.toInt;

@Component
public class TerminalHandler extends TextWebSocketHandler {

    private static final Log log = Log.get();

    /**
     * nz login system token
     */
    private String token;

    /**
     * uuid token
     * connection information key (get SSH | TELNET session)
     */
    private String uuid;

    /**
     * windows fields
     */
    private Integer cols;
    private Integer rows;
    private Integer width;
    private Integer height;

    private Long userId;

    private SysConfService sysConfService;

    private AssetAssetService assetAssetService;

    private TerminalCmdService terminalCmdService;

    private TerminalRecordService terminalRecordService;

    private TerminalSessionService terminalSessionService;

    /**
     * init Field Val
     *
     * @param session
     */
    private void initFieldVal(WebSocketSession session) {
        this.uuid = (String) session.getAttributes().get("uuid");
        this.token = (String) session.getAttributes().get("terminalToken");

        cols = toInt(session.getAttributes().get("cols").toString());
        rows = toInt(session.getAttributes().get("rows").toString());
        width = toInt(session.getAttributes().get("width").toString());
        height = toInt(session.getAttributes().get("height").toString());

        // 创建登录服务器日志记录相关内容
        terminalSessionService = (TerminalSessionService) SpringContextUtils.getBean("terminalSessionService");
        terminalCmdService = (TerminalCmdService) SpringContextUtils.getBean("terminalCmdService");
        terminalRecordService = (TerminalRecordService) SpringContextUtils.getBean("terminalRecordService");
        assetAssetService = SpringContextUtils.getBean(AssetAssetService.class);
        sysConfService = SpringContextUtils.getBean(SysConfService.class);

        TerminalSession.CLIENT_ATTRIBUTE_INFO.put("terminalSessionService", terminalSessionService);
        TerminalSession.CLIENT_ATTRIBUTE_INFO.put("terminalCmdService", terminalCmdService);
        TerminalSession.CLIENT_ATTRIBUTE_INFO.put("terminalRecordService", terminalRecordService);
        TerminalSession.CLIENT_ATTRIBUTE_INFO.put("assetAssetService", assetAssetService);
        TerminalSession.CLIENT_ATTRIBUTE_INFO.put("sysConfService", sysConfService);
    }

    /**
     * inti Terminal Client
     *
     * @param session
     * @param asset
     * @return
     */
    private TerminalClient initTerminalClient(WebSocketSession session, AssetAsset asset, String token) {
        TerminalClient terminalClient = TerminalSession.TERMINAL_SESSION.get(session);
        if (Tool.ObjectUtil.isNotNull(terminalClient)) {
            return terminalClient;
        }

        // init
        terminalClient = new TerminalClient(session, asset, uuid, token);
        TerminalSession.TERMINAL_SESSION.put(session, terminalClient);
        return terminalClient;
    }


    @Override
    public synchronized void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        // init Field Val
        this.initFieldVal(session);
        log.info("WebSocket connectioned. after connection established open terminal begin...  uuid: {}", uuid);

        SysUserEntity userEntity = Tool.MapUtil.get(ShiroUtils.getShiroSessionInfoMap(), token, SysUserEntity.class);
        if (ObjectUtil.isNull(userEntity)) {
            log.warn("open terminal error. system login token exception. uuid: {}, token: {}", uuid, token);
            session.sendMessage(new TextMessage("Token error, Please log in to the system first"));
            session.close();
            return;
        }
        // user id
        userId = userEntity.getId();

        // get connected obj
        Map<Object, Object> connObjMap = TerminalSession.TERMINAL_CONNECTED_CACHE.get(uuid);
        if (ObjectUtil.isNull(connObjMap)) {
            log.warn("open terminal error. Connection information not found, expired or does not exist, please login again. uuid: {}", uuid);
            session.sendMessage(new TextMessage("Connection information not found, expired or does not exist, please login again."));
            session.close();
            return;
        }

        // terminal Connection Info
        AssetAsset terminalConnInfo = (AssetAsset) connObjMap.get(TerminalSession.CONNECTION_ASSET_KEY);

        // terminal session
        TerminalSessionEntity sessionEntity = new TerminalSessionEntity();
        sessionEntity.setUuid(uuid);
        sessionEntity.setUserId(userId);
        sessionEntity.setHost(terminalConnInfo.getManageIp());
        sessionEntity.setPort(terminalConnInfo.getAuthProtocolPort());
        sessionEntity.setAssetId(terminalConnInfo.getId());

        TerminalConstant.ProtocolType protocolType = TerminalConstant.ProtocolType.getEntityByValue(terminalConnInfo.getAuthProtocol());
        sessionEntity.setProtocol(protocolType.toString());

        TerminalConstant.SshAuthType sshAuthType = TerminalConstant.SshAuthType.getEntityByType(terminalConnInfo.getAuthType());
        sessionEntity.setAuthType(sshAuthType.getType());

        sessionEntity.setLoginUser(terminalConnInfo.getAuthUsername());
        sessionEntity.setRemoteAddr(session.getRemoteAddress().getHostString());
        sessionEntity.setStartTime(new Date());
        sessionEntity.setStatus(TerminalConstant.SessionStatus.CONNECTING.getValue());

        TerminalClient terminalClient = null;
        try {
            String token = (String) connObjMap.get(Constant.AUTH_TOKEN_CODE);
            // init client
            terminalClient = this.initTerminalClient(session, terminalConnInfo, token);

            // asset info
            terminalClient.setAsset(terminalConnInfo);

            TerminalSession.setTerminalAttributeInfo(uuid, "terminalSessionEntity", sessionEntity);
            TerminalSession.setTerminalAttributeInfo(uuid, "terminalSessionStartTime", System.currentTimeMillis());

            // terminal cmd 信息
            TerminalCmd terminalCmd = new TerminalCmd();
            terminalCmd.setUuid(uuid);
            terminalCmd.setTime(0);
            terminalCmd.setCmd("Login");
            terminalCmdService.save(terminalCmd);

            Integer authProtocol = terminalConnInfo.getAuthProtocol();
            // SSH
            if (TerminalConstant.ProtocolType.SSH.getValue().equals(authProtocol)) {
                log.info("open ssh terminal,uuid: {},terminal Connection Info: {}", uuid, JSONUtil.toJsonStr(terminalConnInfo));
                Session jschSession = (Session) connObjMap.get(TerminalSession.CONNECTION_CONNECTION_ENTITY_KEY);
                terminalClient.openSshTerminal(jschSession, cols, rows, width, height);
            }
            // TELNET
            if (TerminalConstant.ProtocolType.TELNET.getValue().equals(authProtocol)) {
                log.info("open telnet terminal,uuid: {},terminal Connection Info: {}", uuid, JSONUtil.toJsonStr(terminalConnInfo));
                TelnetClient telnetClient = (TelnetClient) connObjMap.get(TerminalSession.CONNECTION_CONNECTION_ENTITY_KEY);
                terminalClient.openTelnetTerminal(telnetClient, width, height);
            }
            terminalSessionService.save(sessionEntity);
            log.info("WebSocket connectioned. after connection established open terminal finished. uuid: {}", uuid);
        } catch (Exception e) {
            log.error(e, "WebSocket connectioned. after connection established open terminal error. uuid: {}", uuid);
            // 连接异常，关闭session 记录，更改状态为 连接失败
            sessionEntity.setEndTime(new Date());
            sessionEntity.setLoginUser(StrUtil.emptyToDefault(terminalConnInfo.getAuthUsername(), StrUtil.EMPTY));
            sessionEntity.setRemoteAddr(session.getRemoteAddress().getHostString());
            sessionEntity.setEndTime(new Date());
            sessionEntity.setStatus(TerminalConstant.SessionStatus.CONNECTION_FAILED.getValue());
            terminalSessionService.save(sessionEntity);

            // disconnect
            if (ObjectUtil.isNotNull(terminalClient)) {
                terminalClient.disconnect();
            }
            TerminalSession.remove(session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 每次请求获取对应的 uuid 信息
        Map<String, Object> attributes = session.getAttributes();
        String uuid = (String) attributes.get("uuid");
        super.handleTextMessage(session, message);
        log.info("handleTextMessage meesage info : {}", message.getPayload());

        TerminalClient terminalClient = TerminalSession.TERMINAL_SESSION.get(session);
        try {
            if (terminalClient != null) {
                // 保存当前输入字符信息  以便 client 端获取对应的操作字符
                terminalClient.writeContentToTerminal(message.getPayload());
            }
        } catch (Exception e) {
            // 出现异常将光标信息重置防止下一次无法正常执行命令
            log.error(e, "terminal handle Text Message error.");

            // cmd 信息保存
            TerminalCmd terminalCmd = new TerminalCmd();
            terminalCmd.setCmd("Terminal due to abnormal disconnection");
            terminalCmd.setUuid(uuid);
            long terminalSessionStartTime = (long) TerminalSession.getTerminalAttributeInfo(uuid).get("terminalSessionStartTime");
            terminalCmd.setTime((int) (System.currentTimeMillis() - terminalSessionStartTime));
            terminalCmdService.save(terminalCmd);

            // terminal sesssion 信息保存
            TerminalSessionEntity sessionEntity = (TerminalSessionEntity) TerminalSession.getTerminalAttributeInfo(uuid).get("terminalSessionEntity");
            sessionEntity.setRemoteAddr(session.getRemoteAddress().getHostString());
            sessionEntity.setUuid(uuid);
            sessionEntity.setUserId(userId);
            sessionEntity.setStatus(TerminalConstant.SessionStatus.CONNECTION_FAILED.getValue());
            sessionEntity.setStartTime(new Date());
            sessionEntity.setEndTime(new Date());
            terminalSessionService.save(sessionEntity);

            TerminalSession.removeTerminalAttributeInfo(uuid);
            session.sendMessage(new TextMessage(e.getMessage() + "\r\n"));
            session.sendMessage(new TextMessage("Sorry! Nz Terminal was closed, please try again. \r\n"));
            if (ToolUtil.isNotEmpty(terminalClient)) {
                terminalClient.disconnect();
            }
            TerminalSession.remove(session);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error(exception, "WebSocket handle Transport Error");
        super.handleTransportError(session, exception);
        TerminalHandler.closeTerminalRescourse(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        try {
            log.info("[afterConnectionClosed] [WebSocket connection closed] [websocket uri: {}]", session.getUri());
            super.afterConnectionClosed(session, status);

            TerminalClient terminalClient = TerminalSession.TERMINAL_SESSION.get(session);
            log.info("[afterConnectionClosed] [Close terminal client connection] [clientId: {}]", ObjectUtil.isNull(terminalClient) ? null : terminalClient.getClientId());
            if (ObjectUtil.isNull(terminalClient)) {
                log.warn("[afterConnectionClosed] [failed to get terminal client through websocket connection.] [websocket uri: {}]", session.getUri());
                return;
            }

            // close terminal
            TerminalHandler.closeTerminalRescourse(session);

            // client id
            String clientId = terminalClient.getClientId();

            // update Session Status After Client Closed
            TerminalHandler.updateSessionStatusAfterClientClosed(clientId);

            // save Record Content After Client Closed
            TerminalHandler.saveRecordContentAfterClientClosed(clientId);

            // remove Terminal Attribute Info
            TerminalSession.removeTerminalAttributeInfo(clientId);

            // monitor terminal session
            List<WebSocketSession> monitorWebSocketSessions = TerminalSession.TERMINAL_MONITORS_MAPPING.get(clientId);
            if (Tool.CollUtil.isNotEmpty(monitorWebSocketSessions)) {
                log.info("[afterConnectionClosed] [close monitor windows] [size: {}]", monitorWebSocketSessions.size());
                for (WebSocketSession socketSession : monitorWebSocketSessions) {
                    socketSession.sendMessage(new TextMessage("\u001B[01;31m\u001B[KConnection closed \u001B[m\u001B[K"));
                }
                TerminalSession.TERMINAL_MONITORS_MAPPING.remove(clientId);
            }
            log.info("[afterConnectionClosed] [close resource finshed.] [clientId: {}]", clientId);
        } catch (Exception e) {
            log.error(e, "[afterConnectionClosed] [error] [session: {}] [status: {}]", JSONUtil.toJsonStr(session), JSONUtil.toJsonStr(status));
            throw e;
        }
    }

    /**
     * close Terminal
     *
     * @param session
     */
    public static synchronized void closeTerminalRescourse(WebSocketSession session) {
        log.info("[closeTerminalRescourse] [begin] [websocket uri: {}]", session.getUri());
        TerminalClient terminalClient = TerminalSession.remove(session);
        log.info("[closeTerminalRescourse] [find terminal client] [exist: {}]", Tool.ObjectUtil.isNotNull(terminalClient));

        if (Tool.ObjectUtil.isNotNull(terminalClient)) {
            log.info("[closeTerminalRescourse] [terminal client exist. close rescourse] [client id: {}]", terminalClient.getClientId());

            // disconnect
            terminalClient.disconnect();

            // remove cache
            TerminalSession.TERMINAL_CONNECTED_CACHE.remove(terminalClient.getClientId());
            TerminalSession.TERMINAL_CONNECTED_LAST_ACTIVE_MAP.remove(terminalClient.getClientId());
        } else {
            log.warn("[closeTerminalRescourse] [terminal client not exist] [websocket uri: {}]", session.getUri());
        }

        log.info("[closeTerminalRescourse] [finshed] [websocket uri: {}]", session.getUri());
    }

    /**
     * update Session Status After Client Closed
     *
     * @param clientId
     */
    public static synchronized void updateSessionStatusAfterClientClosed(String clientId) {
        log.info("[updateSessionStatusAfterClientClosed] [begin] [clientId: {}]", clientId);
        TerminalSessionEntity sessionEntity = (TerminalSessionEntity) TerminalSession.getTerminalAttributeInfo(clientId).get("terminalSessionEntity");
        log.info("[updateSessionStatusAfterClientClosed] [update session entity] [clientId: {}] [exist: {}]", clientId, Tool.ObjectUtil.isNotNull(sessionEntity));

        if (Tool.ObjectUtil.isNotNull(sessionEntity)) {
            log.info("[updateSessionStatusAfterClientClosed] [udpate session status] [session id: {}] [status: {}]", sessionEntity.getUuid(), sessionEntity.getStatus());
            if (sessionEntity.getStartTime() == null) {
                sessionEntity.setStartTime(new Date());
            }

            // 正常连接状态 关闭窗口，将状态改为 已结束，其他不做处理
            if (TerminalConstant.SessionStatus.CONNECTING.getValue() == sessionEntity.getStatus()) {
                sessionEntity.setStatus(TerminalConstant.SessionStatus.OVER.getValue());
            }

            sessionEntity.setEndTime(new Date());
            TerminalSessionService terminalSessionService = (TerminalSessionService) TerminalSession.CLIENT_ATTRIBUTE_INFO.get("terminalSessionService");
            terminalSessionService.saveOrUpdate(sessionEntity);
        } else {
            log.warn("[updateSessionStatusAfterClientClosed] [not found TerminalSessionEntity through client id.] [clientId: {}]", clientId);
        }
        log.info("[updateSessionStatusAfterClientClosed] [finshed] [clientId: {}]", clientId);
    }

    /**
     * save Record Content After Client Closed
     *
     * @param uuid
     */
    public static synchronized void saveRecordContentAfterClientClosed(String uuid) {
        log.info("[saveRecordContentAfterClientClosed] [begin] [uuid: {}]", uuid);
        File tempFile = Tool.FileUtil.file(Tool.StrUtil.concat(true, Constant.TEMP_PATH, File.separator, uuid, ".log"));
        log.info("[saveRecordContentAfterClientClosed] [save ending file] [uuid: {}] [exist: {}]", uuid, Tool.FileUtil.exist(tempFile));

        // exist
        if (Tool.FileUtil.exist(tempFile)) {
            try {
                log.info("[saveRecordContentAfterClientClosed] [Terminal is closed. save Record content] [clientid: {}] [file length: {}] [File info: {}]", uuid, tempFile.length(), tempFile.toString());
                TerminalRecordService terminalRecordService = (TerminalRecordService) TerminalSession.CLIENT_ATTRIBUTE_INFO.get("terminalRecordService");
                // 将本次内容保存至数据库
                List<TerminalRecord> list = terminalRecordService.list(new LambdaQueryWrapper<TerminalRecord>().eq(TerminalRecord::getUuid, uuid).orderByAsc(TerminalRecord::getId));
                TerminalRecord terminalRecord = new TerminalRecord();
                terminalRecord.setUuid(uuid);
                // first time, start time = 0
                if (Tool.CollUtil.isEmpty(list)) {
                    terminalRecord.setStartTime(0);
                } else {
                    // start time == end of the last time
                    terminalRecord.setStartTime(list.get(list.size() - 1).getEndTime());
                }
                long terminalSessionStartTime = (long) TerminalSession.getTerminalAttributeInfo(uuid).get("terminalSessionStartTime");
                terminalRecord.setEndTime((int) (System.currentTimeMillis() - terminalSessionStartTime));
                terminalRecord.setContent(Files.readAllBytes(tempFile.toPath()));
                terminalRecordService.saveTerminalRecordContent(terminalRecord);
            } catch (Exception e) {
                log.error(e, "[saveRecordContentAfterClientClosed] [Save terminal record content error] [clientid: {}] [file length: {}] [File info: {}]", uuid, tempFile.length(), tempFile.toString());
            } finally {
                // del file
                FileUtil.del(tempFile);
            }

            // not exsit.
        } else {
            log.info("[saveRecordContentAfterClientClosed] [Terminal is closed. No save record file exsit] [clientId: {}]", uuid);
        }

        log.info("[saveRecordContentAfterClientClosed] [finshed] [uuid: {}]", uuid);
    }
}
