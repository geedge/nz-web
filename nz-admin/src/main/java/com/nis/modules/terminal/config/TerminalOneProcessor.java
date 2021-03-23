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


import cn.hutool.log.Log;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.nis.common.utils.*;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

@Component
public class TerminalOneProcessor {
    private Log logger= Log.get();

    public R sendAll(String token, String cmd) throws IOException,UnsupportedEncodingException,RuntimeException {

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
        if(ToolUtil.isNotEmpty(terminalClients)){
            for (TerminalClient client : terminalClients) {
                client.write(cmd);
            }
        }

        return R.ok();
    }

    public R theme(String token, String theme) throws RuntimeException {
        TerminalClient terminalClient = TerminalSession.get(token);
        return R.ok();
    }

    public R resize(String token, Integer cols, Integer rows, Integer width, Integer height) throws IOException {
        List<TerminalClient> terminalClients = TerminalSession.getAllTerminalClient(token);
        if (ToolUtil.isNotEmpty(terminalClients)) {
        	for(TerminalClient terminalClient : terminalClients) {
        		terminalClient.resize(cols, rows, width, height);
        	}
        }
        return R.ok();
    }

    public R upload(String token, File file, String name, long size) {
        TerminalClient terminalClient = TerminalSession.get(token);
        logger.info("terminalClient-->",terminalClient==null?null:terminalClient.getClientId());
        boolean success = true;
        if(CommonUtils.isEmpty(terminalClient)) {
        	logger.info("terminalClient upload fail terminal is null");
        	success=false;
        	return R.error(RCode.TERMINAL_ISNULL);
        }
        if(!terminalClient.isTransfer()) {
        	logger.info("terminalClient upload fail telnet can not transfer");
        	success=false;
        	return R.error(RCode.TERMINAL_TELNET_TRANSFER);
        }
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
        } catch (InterruptedException | JSchException | SftpException | IOException e) {
            logger.error(e);
            success = false;
        }

        return R.ok(success);
    }
    
    public void download(String uuid, String path,HttpServletResponse res) {
    	InputStream downloadFile =null;
        TerminalClient terminalClient = TerminalSession.get(uuid);
        logger.info("terminalClient-->",terminalClient==null?null:terminalClient.getClientId());
        if(CommonUtils.isEmpty(terminalClient)) {
        	logger.info("terminalClient download fail terminal is null");
        	return;
        }
        if(!terminalClient.isTransfer()) {
        	logger.info("terminalClient upload fail telnet can not transfer");
        	return;
        }
        try {
        	terminalClient.download(path,res);
		} catch (IOException | SftpException | JSchException | InterruptedException e) {
			logger.error(e);
		}
    }
}
