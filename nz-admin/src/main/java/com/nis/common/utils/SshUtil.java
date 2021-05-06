package com.nis.common.utils;

import cn.hutool.core.io.FileUtil;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.terminal.config.SshUserInfo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Base64;

import static com.nis.common.utils.CommonUtils.notEmpty;

/**
 * 该类用以提供程序进行ssh连接服务器
 * @author Th
 *
 */
public class SshUtil {
	
	public static Session getSshConnection(AssetAsset asset) throws JSchException {
		JSch jSch =new JSch();
		Session session = jSch.getSession(asset.getAuthUsername(), asset.getManageIp(), asset.getAuthProtocolPort());
		Integer sshType = asset.getAuthType();
        switch (sshType) {
            case 2: {
                String privateKey = asset.getAuthPriKey();
                String keyPassword = asset.getAuthPin();
                if (notEmpty(privateKey)) {
                    File keyFile = FileUtil.file(asset.getPrivateKeyPath());
                    if (!keyFile.exists()) {
                        //将数据库中的私钥写到用户的机器上
                        FileUtil.writeFromStream(new ByteArrayInputStream(privateKey.getBytes()),keyFile);
                    }
                    if (notEmpty(keyPassword)) {
                        //设置带口令的密钥
                        jSch.addIdentity(asset.getPrivateKeyPath(), keyPassword);
                    } else {
                        //设置不带口令的密钥
                        jSch.addIdentity(asset.getPrivateKeyPath());
                    }
                    UserInfo userInfo = new SshUserInfo();
                    session.setUserInfo(userInfo);
                }
                session.setConfig("StrictHostKeyChecking", "no");
                break;
            }
            case 1: {
                session.setConfig("StrictHostKeyChecking", "no");
                session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
                session.setPassword(new String(Base64.getDecoder().decode(asset.getAuthPin())));
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
