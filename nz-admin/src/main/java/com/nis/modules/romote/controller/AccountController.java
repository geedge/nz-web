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

package com.nis.modules.romote.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.romote.entity.Account;
import com.nis.modules.romote.service.AccountService;
import com.nis.modules.romote.support.TerminalClient;
import com.nis.modules.romote.support.TerminalContext;
import com.nis.modules.romote.support.TerminalOneProcessor;
import com.nis.modules.romote.support.TerminalSession;
import com.nis.modules.romote.util.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.WebSocketSession;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * benjobs..
 */
@Controller
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private TerminalContext terminalContext;

    @Value("${nezha.uploadTempPath}")
    private String configUploadPath;

    @Autowired
    private TerminalOneProcessor terminalOneProcessor;

    @GetMapping
    @ResponseBody
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page= accountService.queryPage(params);
        return R.ok(page);

    }
    @PostMapping
    @ResponseBody
    @SysLog("save account")
    public R save( Account account,@RequestParam(value="cert",required = false) MultipartFile sshKey){
        accountService.validateAccount(account,sshKey);
        accountService.saveOrUpdateAccount(account,sshKey);
        return R.ok();
    }

    @PutMapping
    @ResponseBody
    @SysLog("update account")
    public R update( Account account,@RequestParam(value="cert",required = false) MultipartFile sshKey){
        accountService.validateAccount(account,sshKey);
        accountService.saveOrUpdateAccount(account,sshKey);
        return R.ok();
    }

    @DeleteMapping
    @ResponseBody
    @SysLog("delete accounts")
    public R delete(@RequestParam String ids){
        ValidateUtils.is(ids).notNull(RCode.ACCOUNT_ID_ISNULL);
        accountService.delete(ids);
        return R.ok();
    }




    /**web ssh 操作**/

    @RequestMapping(value = "ssh.do", method = RequestMethod.POST)
    @ResponseBody
    public synchronized R ssh(HttpSession session,@RequestBody Account terminal) {

        terminal = accountService.getById(terminal.getId());

        Account.AuthStatus authStatus = accountService.auth(terminal);
        //登陆认证成功
        Map<String,String> map=new HashMap<String,String>();
        if (authStatus.equals(Account.AuthStatus.SUCCESS)) {
            String token = CommonUtils.uuid();
            terminalContext.put(token, terminal);
            map.put("status", "success");
            map.put("token", token);
            map.put("name",terminal.getUser()+"@"+terminal.getAsset().getHost());

            return R.ok(map);
        } else {
            map.put("status", authStatus.status);
            return R.ok(map);
        }
    }

    @RequestMapping(value = "exists.do", method = RequestMethod.POST)
    @ResponseBody
    public boolean exists(Account terminal) throws Exception {
        return accountService.exists(terminal.getUser(), terminal.getAsset().getHost());
    }

    @RequestMapping(value="close.do",method = RequestMethod.POST)
    @ResponseBody
    public R close(HttpSession session, String ... tokens) {
        try{
            closeWithTokens(tokens);
        }catch (Exception e){
            throw new NZException(RCode.SYS_ERROR);
        }

        return R.ok();
    }
    @RequestMapping(value = "resize.do", method = RequestMethod.POST)
    @ResponseBody
    public R resize(String token, Integer cols, Integer rows, Integer width, Integer height) throws Exception {
        return terminalOneProcessor.resize(token, cols, rows, width, height);
    }
    private void closeWithTokens(String ...tokens) throws IOException {
        for (String token:tokens){
            Account account = terminalContext.remove(token);
            if(account!=null){
                String reKey = account.getId() + "_" + token;
                terminalContext.remove(reKey);
            }
            TerminalClient terminalClient = TerminalSession.get(token);
            if(terminalClient!=null){
                terminalClient.write("client will be closed..");
                WebSocketSession webSocketSession = terminalClient.getWebSocketSession();
                TerminalSession.remove(webSocketSession);
                terminalClient.disconnect();
            }

        }
    }
    /**
     * 不能重复复制会话,可以通过ajax的方式重新生成token解决....
     *
     * @param id
     * @param token
     * @return
     * @throws Exception
     */
    @RequestMapping("reopen.do")
    @ResponseBody
    public R reopen(Long id, String token) {
        String reKey = id + "_" + token;
        Account terminal = terminalContext.remove(reKey);//reKey
        if (terminal != null) {
            Map<String,String> map=new HashMap<String,String>();
            token = CommonUtils.uuid();
            terminalContext.put(token, terminal);
            map.put("status", "success");
            map.put("token", token);
            map.put("name",terminal.getUser()+"@"+terminal.getAsset().getHost());
            return R.ok(map);
        }
        throw new NZException(RCode.SYS_ERROR);
    }


    @RequestMapping(value = "sendAll.do", method = RequestMethod.POST)
    @ResponseBody
    public R sendAll(String token, String cmd) throws Exception {
        return terminalOneProcessor.sendAll(token, cmd);
    }

    @RequestMapping(value = "upload.do", method = RequestMethod.POST)
    @ResponseBody
    public R upload(HttpSession httpSession, String token, @RequestParam(value = "file", required = false) MultipartFile file, String path) {
        String rootPath=null;
        try{
            rootPath= ResourceUtils.getURL("classpath:").getPath();
        }catch (Exception e){
            e.printStackTrace();
        }

        String tmpPath=rootPath+configUploadPath;
        File uploadDir=new File(tmpPath);
        if(!uploadDir.exists()){
            uploadDir.mkdirs();
        }else{//存在同名文件/文件夹
            if(!uploadDir.isDirectory()){//是一个同名文件
                uploadDir.delete();
                uploadDir.mkdirs();
            }
        }

        File tempFile = new File(tmpPath, file.getOriginalFilename());
        try {
            file.transferTo(tempFile);
            if (CommonUtils.isEmpty(path)) {
                path = ".";
            } else {
                if (path.endsWith("/")) {
                    path = path.substring(0, path.lastIndexOf("/"));
                }
            }

            tempFile.delete();
            return terminalOneProcessor.upload(token, tempFile, path + "/" + file.getOriginalFilename(), file.getSize());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return R.ok();
    }

}