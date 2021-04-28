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

package com.nis.modules.terminal.config;

import com.nis.modules.asset.entity.AssetAsset;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


@Component
public class TerminalContext implements Serializable {

    //key-->uuid value--->Terminal User (账号信息保存在 AssetAsset entity 中)
    public static final Map<String, AssetAsset> terminalContext = new HashMap<>();

    public static final Map<String, Set<String>> terminalUuid = new HashMap<>();

    private String uuid;

    public AssetAsset get(String key) {
        return terminalContext.get(key(key));
    }

    public void put(String key, AssetAsset asset) {
        this.uuid = key;
        //该终端实例只能被的打开一次,之后就失效
        terminalContext.put(key(key), asset);
        /**
         * 为复制会话
         */
        String reKey = asset.getId() + "_" + key;
        terminalContext.put(key(reKey), asset);
    }

    public AssetAsset remove(String key) {
        return terminalContext.remove(key(key));
    }

    private String key(String key) {
        return Constants.PARAM_TERMINAL_PREFIX_KEY + key;
    }

    public String getToken() {
        return this.uuid;
    }

}
