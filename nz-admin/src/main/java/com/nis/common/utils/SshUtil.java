package com.nis.common.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.URLUtil;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.terminal.utils.SshUserInfo;
import com.nis.modules.terminal.utils.TerminalConstant;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Base64;

import static com.nis.common.utils.CommonUtils.notEmpty;

/**
 * 该类用以提供程序进行ssh连接服务器
 *
 * @author Th
 */
public class SshUtil {

    // session connectTimeout
    public static final int SESSION_TIMEOUT = 60000;

    //  session client Server Alive Interval
    public static final int SESSION_SERVER_ALIVE_INTERVAL = 60 * 1000;

    /**
     * get Ssh Connection
     *
     * @param asset
     * @return
     * @throws JSchException
     */
    public static Session getSshConnection(AssetAsset asset) throws JSchException {
        JSch jSch = new JSch();
        Session session = jSch.getSession(asset.getAuthUsername(), asset.getManageIp(), asset.getAuthProtocolPort());

        TerminalConstant.SshAuthType sshType = TerminalConstant.SshAuthType.getEntityByType(asset.getAuthType());
        switch (sshType) {
            case KEY: {
                String privateKey = asset.getAuthPriKey();
                String keyPassword = asset.getAuthPin();
                if (notEmpty(privateKey)) {
                    File keyFile = FileUtil.file(asset.getPrivateKeyPath());
                    if (!keyFile.exists()) {
                        //将数据库中的私钥写到用户的机器上
                        FileUtil.writeFromStream(new ByteArrayInputStream(privateKey.getBytes()), keyFile);
                    }
                    if (notEmpty(keyPassword)) {
                        //设置带口令的密钥
                        jSch.addIdentity(asset.getPrivateKeyPath(), StringUtils.str(Base64.getDecoder().decode(keyPassword)));
                    } else {
                        //设置不带口令的密钥
                        jSch.addIdentity(asset.getPrivateKeyPath());
                    }
                    session.setUserInfo(new SshUserInfo());
                }
                session.setConfig("StrictHostKeyChecking", "no");
                break;
            }
            case PIN: {
                session.setConfig("StrictHostKeyChecking", "no");
                session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
                session.setPassword(StringUtils.str(Base64.getDecoder().decode(URLUtil.decode(asset.getAuthPin()))));
                break;
            }
            default:
                break;
        }
        session.setServerAliveInterval(SESSION_SERVER_ALIVE_INTERVAL);
        session.connect(SESSION_TIMEOUT);
        return session;
    }

    public static void close(Session session) {
        if (ToolUtil.isNotEmpty(session)) {
            session.disconnect();
        }
    }
}
