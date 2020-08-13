package com.nis.common.utils;

import static com.nis.common.utils.CommonUtils.notEmpty;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Base64;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.terminal.config.SshUserInfo;
import com.nis.modules.terminal.entity.Account;

/**
 * 该类用以提供程序进行ssh连接服务器
 * @author Th
 *
 */
public class SshUtil {
	
	public static Session getSshConnection(Asset asset,Account account) throws Exception {
		Map<String, String> params = JSONObject.parseObject((String) account.getParams(), Map.class);
		JSch jSch =new JSch();
		Session session = jSch.getSession(params.get("username"), asset.getHost(), account.getPort());
		String sshType = params.get("method");
        switch (sshType) {
            case "key": {
                String privateKey = params.get("key");
                String keyPassword = params.get("keyPassword");
                if (notEmpty(privateKey)) {
                    File keyFile = new File(account.getPrivateKeyPath());
                    if (!keyFile.exists()) {
                        //将数据库中的私钥写到用户的机器上
                        IOUtils.writeFile(keyFile, new ByteArrayInputStream(privateKey.getBytes()));
                    }
                    if (notEmpty(keyPassword)) {
                        //设置带口令的密钥
                        jSch.addIdentity(account.getPrivateKeyPath(), keyPassword);
                    } else {
                        //设置不带口令的密钥
                        jSch.addIdentity(account.getPrivateKeyPath());
                    }
                    UserInfo userInfo = new SshUserInfo();
                    session.setUserInfo(userInfo);
                }
                session.setConfig("StrictHostKeyChecking", "no");
                break;
            }
            case "password": {
                session.setConfig("StrictHostKeyChecking", "no");
                session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
                session.setPassword(new String(Base64.getDecoder().decode(params.get("password"))));
                break;
            }
            default: {
            }
        }
//        session.setServerAliveInterval(SERVER_ALIVE_INTERVAL);
        session.connect(5000);
        return session;
	}
	
	public static void close(Session session) {
		if(ToolUtil.isNotEmpty(session)) {
			session.disconnect();
		}
	}
}
