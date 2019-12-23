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

package com.nis.modules.romote.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.romote.dao.AccountDao;
import com.nis.modules.romote.entity.Account;
import com.nis.modules.romote.service.AccountService;
import com.nis.modules.romote.support.SshUserInfo;
import com.nis.modules.romote.support.TerminalClient;
import com.nis.modules.romote.util.Constants;
import com.nis.modules.romote.util.IOUtils;
import com.nis.modules.romote.util.StringUtils;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.service.SysDictService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.BadPaddingException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static com.nis.modules.romote.util.CommonUtils.notEmpty;

/**
 * @author <a href="mailto:benjobs@qq.com">benjobs@qq.com</a>
 * @name:CommonUtil
 * @version: 1.0.0
 * @company: com.jobxhub
 * @description: webconsole核心类
 * @date: 2016-05-25 10:03<br/><br/>
 * <p>
 * <b style="color:RED"></b><br/><br/>
 * 你快乐吗?<br/>
 * 风轻轻的问我<br/>
 * 曾经快乐过<br/>
 * 那时的湖面<br/>
 * 她踏着轻舟泛过<br/><br/>
 * <p>
 * 你忧伤吗?<br/>
 * 雨悄悄的问我<br/>
 * 一直忧伤着<br/>
 * 此时的四季<br/>
 * 全是她的柳絮飘落<br/><br/>
 * <p>
 * 你心痛吗?<br/>
 * 泪偷偷的问我<br/>
 * 心痛如刀割<br/>
 * 收到记忆的包裹<br/>
 * 都是她冰清玉洁还不曾雕琢<br/><br/>
 * <p>
 * <hr style="color:RED"/>
 */

@Service
public class AccountServiceImpl extends ServiceImpl<AccountDao,Account> implements AccountService {
    private static Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);
    @Autowired
    private AssetService assetService;
    @Autowired
    private SysDictService sysDictService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        String id=null;
        String assetId=null;
        String authType=null;
        String user=null;

        if(params != null && params.size() > 0){
            id = (String)params.get("id");
            assetId = (String)params.get("assetId");
            authType = (String)params.get("authType");
            user = (String)params.get("user");
        }

        QueryWrapper<Account> accountQueryWrapper=new QueryWrapper<Account>();
        accountQueryWrapper.eq(StringUtils.isNotEmpty(id),"id",id);
        accountQueryWrapper.eq(StringUtils.isNotEmpty(assetId),"asset_id",assetId);
        accountQueryWrapper.eq(StringUtils.isNotEmpty(authType),"auth_type",authType);
        accountQueryWrapper.eq(StringUtils.isNotEmpty(user),"user",user);
        IPage<Account> page=this.page(new Query<Account>().getPage(params),accountQueryWrapper);

        return new PageUtils(page);
    }

    @Override
    public void validateAccount(Account account, MultipartFile file) {
        //1.资产id不能为空
        ValidateUtils.is(account.getAssetId()).notNull(RCode.ASSET_ID_ISNULL);

        //2.资产必须存在
        Asset asset=assetService.getById(account.getAssetId());
        if(asset==null){
            throw new NZException(RCode.ASSET_NOT_EXIST);
        }
        //3.认证类型必须合法
        ValidateUtils.is(account.getAuthType()).notNull(RCode.ACCOUNT_AUTHTYPE_ISNULL);
        QueryWrapper<SysDictEntity> sysDictyQueryWrapper=new QueryWrapper<SysDictEntity>();
        sysDictyQueryWrapper.eq("type",Constant.DICT_ACCOUNT_AUTH_TYPE);
        sysDictyQueryWrapper.eq("value",account.getAuthType());
        List<SysDictEntity> authTypeList = sysDictService.list(sysDictyQueryWrapper);
        if(authTypeList==null||authTypeList.size()>0){
            throw new NZException(RCode.ACCOUNT_AUTHTYPE_INVALIDE);
        }

        //4.用户名不能为空
        ValidateUtils.is(account.getUser()).notNull(RCode.ACCOUNT_USER_ISNULL);

        //5.密码登录，密码不能为空，证书登录，证书不能为空
        if(Constant.AUTH_PASSWORD==account.getAuthType()){
            ValidateUtils.is(account.getPwd()).notNull(RCode.ACCOUNT_PWD_ISNULL);
        }else{
            ValidateUtils.is(file).notNull(RCode.ACCOUNT_KEYFILE_ISNULL);
        }

        //6.port 不能为空
        ValidateUtils.is(account.getPort()).notNull(RCode.ACCOUNT_PORT_ISNULL);
    }

    @Override
    public void saveOrUpdateAccount(Account account,MultipartFile file) {
        if (Constant.AUTH_CERTIFICATE==account.getAuthType()){
            try{
                account.setPrivateKey(file.getBytes());
            }catch (Exception e){
                logger.error("transfer ssh certificate error",e);
            }
        }
        /*if(StringUtils.isNotEmpty(account.getPwd())){
            String encodePwd=Base64.getEncoder().encodeToString(account.getPwd().getBytes());
            account.setPwd(encodePwd);
        }*/
        if(account.getId()==null){
            if(StringUtils.isNotEmpty(account.getPwd())){
                String encodePwd=Base64.getEncoder().encodeToString(account.getPwd().getBytes());
                account.setPwd(encodePwd);
            }

            this.save(account);
        }else{
            // 修改参数含有密码
            if(StringUtils.isNotEmpty(account.getPwd())){
                Account accountEntity = this.getById(account.getId());

                // 前台传参密码和数据库存储密码不对应 证明修改了密码
               if(!account.getPwd().equals(accountEntity.getPwd())){
                   String encodePwd=Base64.getEncoder().encodeToString(account.getPwd().getBytes());
                   account.setPwd(encodePwd);
               }
            }

            this.updateById(account);
        }

    }

    @Override
    public void delete(String ids) {
        String[] idArr=ids.split(",");
        List<Integer> delIds=new ArrayList<Integer>();
        for (String id : idArr){
            try{
               delIds.add(Integer.parseInt(id));
            }catch(NumberFormatException e){
                throw new NZException(RCode.ACCOUNT_ID_INVALIDE);
            }
        }

        this.removeByIds(delIds);
    }

    @Override
    public boolean exists(String userName, String host) throws Exception {
        int count = this.baseMapper.getCount(userName,host);
        return count > 0;
    }

    @Override
    public Account.AuthStatus auth(Account terminal) {
        JSch jSch = new JSch();
        Session session = null;
        try {
            session = jSch.getSession(terminal.getUser(), terminal.getAsset().getHost(), terminal.getPort());
            Constants.SshType sshType = Constants.SshType.getByType(terminal.getAuthType());
            switch (sshType) {
                case SSHKEY:
                    if (notEmpty(terminal.getPrivateKey())) {
                        File keyFile = new File(terminal.getPrivateKeyPath());
                        if (keyFile.exists()) {
                            keyFile.delete();
                        }
                        //将数据库中的私钥写到用户的机器上
                        IOUtils.writeFile(keyFile, new ByteArrayInputStream(terminal.getPrivateKey()));
                        if (notEmpty(terminal.getPwd())) {
                            //设置带口令的密钥
                            jSch.addIdentity(terminal.getPrivateKeyPath(), terminal.getPwd());
                        } else {
                            //设置不带口令的密钥
                            jSch.addIdentity(terminal.getPrivateKeyPath());
                        }
                        UserInfo userInfo = new SshUserInfo();
                        session.setUserInfo(userInfo);
                    }
                    break;
                case ACCOUNT:
                    session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
                    session.setPassword(new String(Base64.getDecoder().decode(terminal.getPwd())));
                    break;
            }
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(TerminalClient.SESSION_TIMEOUT);
            return Account.AuthStatus.SUCCESS;
        } catch (Exception e) {
            if (e.getMessage().toLowerCase().contains("userauth fail")) {
                return Account.AuthStatus.PUBLIC_KEY_FAIL;
            } else if (e.getMessage().toLowerCase().contains("auth fail") || e.getMessage().toLowerCase().contains("auth cancel")) {
                return Account.AuthStatus.AUTH_FAIL;
            } else if (e.getMessage().toLowerCase().contains("unknownhostexception")) {
                if (logger.isInfoEnabled()) {
                    logger.info("[JobX]:error: DNS Lookup Failed ");
                }
                return Account.AuthStatus.HOST_FAIL;
            } else if (e instanceof BadPaddingException) {//RSA解码错误..密码错误...
                return Account.AuthStatus.AUTH_FAIL;
            } else {
                return Account.AuthStatus.GENERIC_FAIL;
            }
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }
    }

    @Override
    public Account getById(Integer id) {
        Account account = this.baseMapper.getAccountById(id);
        return account;
    }

}


