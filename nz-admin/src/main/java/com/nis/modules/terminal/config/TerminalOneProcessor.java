/**
 * Copyright (c) 2015 The JobX Project
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


import com.alibaba.fastjson.JSON;
import com.nis.common.utils.R;
import com.nis.common.utils.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URLDecoder;
import java.util.List;

@Component
public class TerminalOneProcessor {
    private Logger logger= LoggerFactory.getLogger(TerminalOneProcessor.class);

//    @MethodMark
    public R sendAll(String token, String cmd) throws Exception {

        cmd = URLDecoder.decode(cmd, Constants.CHARSET_UTF8);
        List<TerminalClient> terminalClients=null;
        if(StringUtils.isEmpty(token)){
            terminalClients=TerminalSession.findClient();
        }else{
            TerminalClient terminalClient = TerminalSession.get(token);
            if (terminalClient != null) {
                terminalClients= TerminalSession.findClient(terminalClient.getHttpSessionId());
            }
        }
        if(terminalClients!=null&&terminalClients.size()>0){
            for (TerminalClient client : terminalClients) {
                client.write(cmd);
            }
        }

        return R.ok();
    }

//    @MethodMark
    public R theme(String token, String theme) throws Exception {
        TerminalClient terminalClient = TerminalSession.get(token);
        return R.ok();
    }

//    @MethodMark
    public R resize(String token, Integer cols, Integer rows, Integer width, Integer height) throws Exception {
        List<TerminalClient> terminalClients = TerminalSession.getAllTerminalClient(token);
        if (terminalClients != null && terminalClients.size()>0) {
        	for(TerminalClient terminalClient : terminalClients) {
        		terminalClient.resize(cols, rows, width, height);
        	}
        }
        return R.ok();
    }

//    @MethodMark
    public R upload(String token, File file, String name, long size) {
        TerminalClient terminalClient = TerminalSession.get(token);
        logger.debug("terminalClient-->",terminalClient==null?null:terminalClient.getClientId());
        boolean success = true;
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            if (terminalClient != null) {
                terminalClient.upload(file.getAbsolutePath(), name, size);
            }

            logger.debug("上传完成");
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }

        return R.ok();
    }

}
