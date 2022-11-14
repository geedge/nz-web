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

import cn.hutool.core.util.StrUtil;
import com.nis.common.utils.Tool;
import org.springframework.util.ObjectUtils;
import org.springframework.web.socket.WebSocketSession;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class TerminalSession implements Serializable {

    /**
     * 连接建立后，记录 connectionAsset、sftp、connectionEntity（jsch.session | telnet client）
     */
    public static final String CONNECTION_ASSET_KEY = "connectionAsset";
    public static final String CONNECTION_CONNECTION_ENTITY_KEY = "connectionEntity";

    /**
     * terminal login cache
     * key = uuid
     * value = {
     * connectionAsset: 连接实体信息
     * connectionEntity: SSH SESSION | TELNET CLIENT
     * }
     */
    public static final Map<String, Map<Object, Object>> TERMINAL_CONNECTED_CACHE = Tool.MapUtil.newHashMap();

    /**
     * terminal connnection last active time map
     * key = uuid
     * value = last active timestamp
     * 记录连接最后数据交互的时间，用以判断是否长期未实际使用，关闭连接
     * 窗口数据交互 & sftp 命令执行 都需要记录
     */
    public static final Map<String, Long> TERMINAL_CONNECTED_LAST_ACTIVE_MAP = Tool.MapUtil.newHashMap();

    /**
     * 当前websockt连接信息对应的服务器信息   将来可以实现远程连接列表 用以进行主动断开服务器连接功能
     */
    public static final Map<WebSocketSession, TerminalClient> TERMINAL_SESSION = Tool.MapUtil.newHashMap();

    /**
     * client terminal monitor list mapping
     */
    public static final Map<String, List<WebSocketSession>> TERMINAL_MONITORS_MAPPING = Tool.MapUtil.newConcurrentHashMap();

    /**
     * terminal service, Include terminal session & record & cmd 。。。 mybatis service object
     */
    public static final Map<String, Object> CLIENT_ATTRIBUTE_INFO = Tool.MapUtil.newHashMap();

    /**
     * 保存用户连接当前websocket的一些配置信息 包括光标位置、未登录时的一些状态信息
     * 每个连接功能会公用一个terminalHandler类 将各自属性根据uuid以及相应的状态信息进行保存
     */
    public static final Map<String, Map> TERMINAL_ATTRIBUTE_INFO = Tool.MapUtil.newHashMap();

    /**
     * terminal cmd info
     */
    public static final Map<String, List<Character>> TERMINAL_CMD_INFO = Tool.MapUtil.newHashMap();

    /**
     * remove Terminal Cmd
     *
     * @param uuid
     */
    public static void removeTerminalCmd(String uuid) {
        TERMINAL_CMD_INFO.remove(uuid);
    }


    /**
     * set Terminal Attribute Info
     *
     * @param uuid
     * @param key
     * @param value
     */
    public static void setTerminalAttributeInfo(String uuid, String key, Object value) {
        Map map = TERMINAL_ATTRIBUTE_INFO.get(uuid);
        if (Tool.MapUtil.isEmpty(map)) {
            // init Attribute map
            map = getTerminalAttributeInfo(uuid);
        }
        // put new key
        map.put(key, value);
        TERMINAL_ATTRIBUTE_INFO.put(uuid, map);
    }

    /**
     * 终端快捷键标识
     *
     * @param uuid
     * @return
     */
    public static Map getTerminalAttributeInfo(String uuid) {
        Map map = TERMINAL_ATTRIBUTE_INFO.get(uuid);
        if (ObjectUtils.isEmpty(map)) {
            // 第一次获取基础配置的时候 初始化配置信息
            map = Tool.MapUtil.newHashMap();
            // 指令光标位置信息
            map.put("cursorIndex", 0);
            // 光标是否移动标识
            map.put("cursorFlag", false);
            // 历史记录位置信息
            map.put("historyIndex", 0);
            // ssh连接登录标识
            map.put("sshLogin", false);
            // tab键标识
            map.put("tabFlag", false);

            // 光标位置
            map.put("cmdCharPos", 0);
            TERMINAL_ATTRIBUTE_INFO.put(uuid, map);
        }
        return map;
    }

    public static void removeTerminalAttributeInfo(String uuid) {
        TERMINAL_ATTRIBUTE_INFO.remove(uuid);
    }

    public static TerminalClient remove(WebSocketSession key) {
        return TERMINAL_SESSION.remove(key);
    }


    /**
     * 通过 uuid 得到 webSocketSession 对象
     *
     * @param uuid
     * @return
     */
    public static WebSocketSession findSession(String uuid) {
        Map.Entry<WebSocketSession, TerminalClient> mapEntry = TERMINAL_SESSION.entrySet().stream()
                .filter(Objects::nonNull)
                .filter(entry -> Tool.ObjectUtil.isNotNull(entry.getValue()) && StrUtil.equalsIgnoreCase(entry.getValue().getClientId(), uuid))
                .findFirst()
                .orElse(null);

        return Tool.ObjectUtil.isNotNull(mapEntry) ? mapEntry.getKey() : null;
    }

    /**
     * get all session client (onLine) by token
     *
     * @param token
     * @return
     */
    public static List<TerminalClient> getAllTerminalClient(String token) {
        List<TerminalClient> terminalClients = TERMINAL_SESSION.values().stream()
                .filter(client -> StrUtil.equals(client.getClientId(), token))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return terminalClients;
    }

}
