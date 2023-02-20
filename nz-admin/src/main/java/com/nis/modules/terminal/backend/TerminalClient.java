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
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.nis.common.utils.JSONUtil;
import com.nis.common.utils.Tool;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.terminal.entity.TerminalCmd;
import com.nis.modules.terminal.entity.TerminalRecord;
import com.nis.modules.terminal.service.TerminalCmdService;
import com.nis.modules.terminal.service.TerminalRecordService;
import com.nis.modules.terminal.utils.TerminalConstant;
import lombok.Data;
import org.apache.commons.net.telnet.TelnetClient;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class TerminalClient {

    private static final Log log = Log.get();

    /**
     * uuid token
     */
    private String clientId;

    private AssetAsset asset;

    /**
     * conn object
     */
    private Session session;
    private ChannelShell shellChannel;
    private WebSocketSession webSocketSession;

    /**
     * stream
     */
    private InputStream inputStream;
    private OutputStream outputStream;
    private BufferedWriter writer;

    public TerminalClient(WebSocketSession webSocketSession, AssetAsset asset, String clientId) {
        log.info("init TerminalClient, uuid: {}, terminal Connection Info: {}", clientId, JSONUtil.toJsonStr(asset));
        this.webSocketSession = webSocketSession;
        this.asset = asset;
        this.clientId = clientId;
    }

    /**
     * Ssh Terminal
     *
     * @param jschSession
     * @param cols
     * @param rows
     * @param width
     * @param height
     * @throws JSchException
     * @throws IOException
     */
    public void openSshTerminal(Session jschSession, final int cols, int rows, int width, int height) throws JSchException, IOException {
        this.session = jschSession;
        this.shellChannel = (ChannelShell) session.openChannel("shell");
        this.shellChannel.setPtyType("xterm", cols, rows, width, height);
        this.inputStream = this.shellChannel.getInputStream();
        this.outputStream = this.shellChannel.getOutputStream();
        this.writer = new BufferedWriter(new OutputStreamWriter(this.outputStream, "UTF-8"));
        this.shellChannel.connect();

        // send login Success Prompt
        String loginSuccessPromptStr = StrUtil.concat(true,
                "\r\nConnecting to " + asset.getManageIp() + ":" + asset.getAuthProtocolPort() + "\r\n",
                "Connection established. \r\n",
                "Login success，use user " + asset.getAuthUsername() + " \r\n");
        webSocketSession.sendMessage(new TextMessage(loginSuccessPromptStr));

        // run ssh terminal runable
        Tool.ThreadUtil.execute(
                new TerminalInteractionRunnable(this)
        );
    }

    /**
     * TELNET Terminal
     *
     * @param telnetClient
     * @throws IOException
     */
    public void openTelnetTerminal(TelnetClient telnetClient) throws IOException {
        this.inputStream = telnetClient.getInputStream();
        this.outputStream = telnetClient.getOutputStream();
        this.writer = new BufferedWriter(new OutputStreamWriter(this.outputStream, "UTF-8"));

        // send login Success Prompt
        String loginSuccessPromptStr = StrUtil.concat(true,
                "\r\nConnecting to " + asset.getManageIp() + ":" + asset.getAuthProtocolPort() + "\r\n",
                "Connection established. \r\n",
                "Login success，use user " + asset.getAuthUsername() + " \r\n");
        webSocketSession.sendMessage(new TextMessage(loginSuccessPromptStr));

        // run telnet terminal runable
        Tool.ThreadUtil.execute(
                new TerminalInteractionRunnable(this)
        );
    }

    /**
     * send Message To Monitor Terminal
     *
     * @param clientId
     * @param message
     */
    public void sendMessageToMonitorTerminal(String clientId, String message) {
        try {
            // monitor terminal session
            List<WebSocketSession> webSocketSessions = TerminalSession.TERMINAL_MONITORS_MAPPING.get(clientId);
            if (Tool.CollUtil.isNotEmpty(webSocketSessions)) {
                List<String> webSocketSessionIdList = webSocketSessions.stream().map(WebSocketSession::getId).collect(Collectors.toList());
                log.info("send Message To Monitor Terminal. clientId: {}, monitor terminal info: {}", clientId, Tool.JSONUtil.toJsonStr(webSocketSessionIdList));
                for (WebSocketSession socketSession : webSocketSessions) {
                    socketSession.sendMessage(new TextMessage(message));
                }
            }
        } catch (IOException e) {
            log.error(e, "send Message To Monitor Terminal error. clientId: {}, terminal_monitors_mapping: {}", clientId, Tool.JSONUtil.toJsonStr(TerminalSession.TERMINAL_MONITORS_MAPPING.get(clientId)));
        }
    }

    /**
     * save Or Append Terminal Record Message
     *
     * @param tempFile
     * @param message
     * @throws IOException
     */
    public void saveOrAppendTerminalRecordMessage(File tempFile, String message) {
        // save Terminal Record Message
        // record file is larger than the system config size,save db.
        if (BooleanUtil.and(
                tempFile.exists(),
                tempFile.length() > TerminalConstant.RECORD_TEMPFILE_MAX_LENGTH)) {
            try {
                log.info("Save terminal record content. clientid: {}, file length: {}, File info: {}", clientId, tempFile.length(), tempFile.toString());
                TerminalRecordService terminalRecordService = (TerminalRecordService) TerminalSession.CLIENT_ATTRIBUTE_INFO.get("terminalRecordService");
                List<TerminalRecord> list = terminalRecordService.list(new LambdaQueryWrapper<TerminalRecord>().eq(TerminalRecord::getUuid, clientId).orderByAsc(TerminalRecord::getId));
                TerminalRecord terminalRecord = new TerminalRecord();
                terminalRecord.setUuid(clientId);
                // first time, start time = 0
                if (Tool.CollUtil.isEmpty(list)) {
                    terminalRecord.setStartTime(0);
                } else {
                    // start time == end of the last time
                    terminalRecord.setStartTime(list.get(list.size() - 1).getEndTime());
                }
                long terminalSessionStartTime = (long) TerminalSession.getTerminalAttributeInfo(clientId).get("terminalSessionStartTime");
                terminalRecord.setEndTime((int) (System.currentTimeMillis() - terminalSessionStartTime));
                terminalRecord.setContent(Files.readAllBytes(tempFile.toPath()));
                terminalRecordService.saveTerminalRecordContent(terminalRecord);
            } catch (Exception e) {
                log.error(e, "Save terminal record content error. clientid: {}, file length: {}, File info: {}", clientId, tempFile.length(), tempFile.toString());
            } finally {
                FileUtil.del(tempFile);
            }
        }

        // Append Terminal Record Message
        try {
            long terminalSessionStartTime = (long) TerminalSession.getTerminalAttributeInfo(clientId).get("terminalSessionStartTime");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("t", (int) (System.currentTimeMillis() - terminalSessionStartTime));
            jsonObject.put("c", message);
            String str = StrUtil.concat(true, JSONUtil.toJsonStr(jsonObject), ",");
            FileUtil.writeBytes(str.getBytes(), tempFile, 0, str.getBytes().length, true);
        } catch (Exception e) {
            log.error(e, "failed to append terminal record file content. clientid: {}, File info: {}", clientId, tempFile != null && tempFile.exists() ? tempFile.toString() : "file does not exist");
        }
    }

    /**
     * 处理服务器方向键以及tab操作返回的指令信息 便于做日志记录
     *
     * @param serverResponse 服务器响应内容
     */
    public void processCommand(String serverResponse) {
        Map terminalAttributeInfo = TerminalSession.getTerminalAttributeInfo(clientId);
        Integer cmdCharPos = (Integer) terminalAttributeInfo.get("cmdCharPos");

        // cmd info
        List<Character> list = Tool.CollUtil.defaultIfEmpty(TerminalSession.TERMINAL_CMD_INFO.get(clientId), Tool.ListUtil.list(true));

        char[] charArray = serverResponse.toCharArray();

        // 转义字符
        boolean esc_mode = false;
        int esc_arg = 0;

        boolean maybeCmd = false;
        if (charArray.length >= 2) {
            // 0x0d 13 \r  0x0a 10 \n
            maybeCmd = (charArray[0] == 13 && charArray[1] == 10);
        }

        boolean client_single_char = false;
        if (charArray.length == 1) {
            int c = charArray[0];
            if (TerminalConstant.INVISIBLE_CHAR.contains(c)) {
                client_single_char = true;
            }
        }

        int offset = 0;
        int len = charArray.length;
        for (; offset < charArray.length; ) {
            char ch = charArray[offset];
            if (esc_mode) {
                switch (ch) {
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9': {
                        // 字符转数字
                        esc_arg = esc_arg * 10 + (ch - '0');
                        break;
                    }
                    case 63:
                    case ';':
                    case '>': {
                        TerminalSession.TERMINAL_CMD_INFO.put(clientId, Tool.ListUtil.list(true));
                        TerminalSession.setTerminalAttributeInfo(clientId, "cmdCharPos", 0);
                        return;
                    }
                    case 75: { // K
                        if (0 == esc_arg) {
                            list = list.subList(0, cmdCharPos);
                            cmdCharPos = list.size();
                        } else if (1 == esc_arg) {
                            list = list.subList(cmdCharPos, list.size());
                            cmdCharPos = list.size();
                        } else if (2 == esc_arg) {
                            list = new ArrayList<>();
                            cmdCharPos = 0;
                        }
                        TerminalSession.TERMINAL_CMD_INFO.put(clientId, list);
                        TerminalSession.setTerminalAttributeInfo(clientId, "cmdCharPos", cmdCharPos);
                        esc_mode = false;
                        break;
                    }
                    case 67: {    // ^[C
                        // 光标右移
                        if (esc_arg == 0) {
                            esc_arg = 1;
                        }

                        for (int j = 0; j < esc_arg; ++j) {
                            if (cmdCharPos != list.size()) {
                                cmdCharPos++;
                            }
                        }
                        TerminalSession.setTerminalAttributeInfo(clientId, "cmdCharPos", cmdCharPos);
                        esc_mode = false;
                        break;
                    }
                    case 68: { // ^[D
                        // 光标左移
                        if (esc_arg == 0) {
                            esc_arg = 1;
                        }
                        for (int j = 0; j < esc_arg; ++j) {
                            if (cmdCharPos != 0) {
                                cmdCharPos--;
                            }
                        }
                        TerminalSession.setTerminalAttributeInfo(clientId, "cmdCharPos", cmdCharPos);
                        esc_mode = false;
                        break;
                    }
                    case 80: { // 'P' 删除指定数量的字符
                        if (esc_arg == 0) {
                            esc_arg = 1;
                        }

                        for (int j = 0; j < esc_arg; ++j) {
                            // 不是最后一个
                            if (cmdCharPos != list.size()) {
                                int index = cmdCharPos;
                                list.remove(index);
                            }
                        }
                        TerminalSession.TERMINAL_CMD_INFO.put(clientId, list);
                        TerminalSession.setTerminalAttributeInfo(clientId, "cmdCharPos", cmdCharPos);
                        esc_mode = false;
                        break;
                    }
                    case 64: {
                        if (esc_arg == 0) {
                            esc_arg = 1;
                        }
                        for (int j = 0; j < esc_arg; ++j) {
                            list.add(cmdCharPos, ' ');
                            cmdCharPos++;
                        }
                        TerminalSession.TERMINAL_CMD_INFO.put(clientId, list);
                        TerminalSession.setTerminalAttributeInfo(clientId, "cmdCharPos", cmdCharPos);
                        esc_mode = false;
                        break;
                    }
                    default:
                        esc_mode = false;
                        break;
                }

                offset++;
                continue;
            }

            switch (ch) {
                case 7:
                    // 响铃
                    break;
                case 8: {
                    // 光标左移
                    if (cmdCharPos != 0 && CollectionUtils.isNotEmpty(list)) {
                        cmdCharPos--;
                        TerminalSession.TERMINAL_CMD_INFO.put(clientId, list);
                        TerminalSession.setTerminalAttributeInfo(clientId, "cmdCharPos", cmdCharPos);
                    }
                    break;
                }
                case 27: {  // 0x1b esc
                    if (offset + 1 < len) {
                        if (charArray[offset + 1] == 91 || charArray[offset + 1] == 93) {
                            if (offset == 0 && client_single_char) {
                                TerminalSession.removeTerminalCmd(clientId);
                                TerminalSession.setTerminalAttributeInfo(clientId, "cmdCharPos", 0);
                                maybeCmd = false;
                                client_single_char = false;
                                return;
                            }
                        }

                        if (charArray[offset + 1] == 91 || charArray[offset + 1] == 93) {
                            esc_mode = true;

                            esc_arg = 0;
                            offset += 1;
                        }
                    }
                    break;
                }
                // 0x0d 13 \r  0x0a 10 \n
                case 13: {
                    if (offset + 1 < len && charArray[offset + 1] == 10) {
                        if (maybeCmd) {
                            if (Tool.CollUtil.isNotEmpty(list)) {
                                TerminalCmdService terminalCmdService = (TerminalCmdService) TerminalSession.CLIENT_ATTRIBUTE_INFO.get("terminalCmdService");
                                TerminalCmd terminalCmd = new TerminalCmd();
                                terminalCmd.setCmd(StrUtil.join(StrUtil.EMPTY, list));
                                terminalCmd.setUuid(clientId);
                                long terminalSessionStartTime = (long) TerminalSession.getTerminalAttributeInfo(clientId).get("terminalSessionStartTime");
                                terminalCmd.setTime((int) (System.currentTimeMillis() - terminalSessionStartTime));
                                terminalCmdService.save(terminalCmd);
                            }
                            TerminalSession.removeTerminalCmd(clientId);
                            TerminalSession.setTerminalAttributeInfo(clientId, "cmdCharPos", 0);
                            maybeCmd = false;
                        }
                    }
                    TerminalSession.removeTerminalCmd(clientId);
                    TerminalSession.setTerminalAttributeInfo(clientId, "cmdCharPos", 0);
                    return;
                }
                default: {
                    if (cmdCharPos != list.size()) {
                        int index = cmdCharPos;
                        list.remove(index);
                        list.add(cmdCharPos, ch);
                        cmdCharPos++;
                    } else {
                        list.add(ch);
                        cmdCharPos = list.size();
                    }
                    TerminalSession.TERMINAL_CMD_INFO.put(clientId, list);
                    TerminalSession.setTerminalAttributeInfo(clientId, "cmdCharPos", cmdCharPos);
                }
            }
            offset++;
        }
    }

    /**
     * 向终端输入内容
     *
     * @param message
     * @throws IOException
     */
    public void writeContentToTerminal(String message) {
        // write
        if (Tool.ObjectUtil.isNotNull(outputStream)) {
            IoUtil.write(outputStream, false, message.getBytes());
            IoUtil.flush(outputStream);
        }
    }

    /**
     * resize
     *
     * @param cols
     * @param rows
     * @param width
     * @param height
     */
    public void resize(Integer cols, Integer rows, Integer width, Integer height) {
        log.info("terminal re window size. col: {}, rows: {}, width: {}, height: {}", cols, rows, width, height);
        try {
            if (this.shellChannel != null) {
                this.shellChannel.setPtySize(cols, rows, width, height);
            }
        } catch (Exception e) {
            log.error(e, "terminal re window size error. col: {}, rows: {}, width: {}, height: {}", cols, rows, width, height);
        }
    }

    /**
     * disconnect
     *
     * @throws IOException
     */
    public void disconnect() {
        log.info("[disconnect] [begin] [client id: {}]", clientId);

        IoUtil.close(writer);
        IoUtil.close(inputStream);
        IoUtil.close(outputStream);

        if (shellChannel != null) {
            shellChannel.disconnect();
        }

        if (session != null) {
            session.disconnect();
            session = null;
            // remove cache
            TerminalSession.TERMINAL_CONNECTED_CACHE.remove(clientId);
            TerminalSession.TERMINAL_CONNECTED_LAST_ACTIVE_MAP.remove(clientId);
        }

        if (webSocketSession != null) {
            IoUtil.close(webSocketSession);
            TerminalSession.remove(webSocketSession);
        }

        log.info("[disconnect] [finshed] [client id: {}]", clientId);
    }

}
