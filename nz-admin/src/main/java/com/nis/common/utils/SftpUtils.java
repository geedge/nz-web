/*
package com.nis.common.utils;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.jcraft.jsch.*;
import com.nis.common.exception.NZException;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Properties;

*/
/**
 * 1. sftp传输文件
 * 2. 执行命令
 *//*

public class SftpUtils {

    private static Log logger = Log.get();

    private ChannelSftp sftp;

    // session 信息
    private Session session;

    // 连接超时时间
    private static Integer connectTimeOut = 5000;


    */
/**
     * 登陆
     *
     * @param server     ip
     * @param port       port
     * @param username   username
     * @param authType   1 密码 2 密钥文件
     * @param password   密码
     * @param privateKey 密钥文件
     * @throws Exception
     *//*

    public void loginIn(String server, Integer port, String username, Integer authType, String pin, String privateKey, String keyPassword) throws NZException  {
        OutputStream out = null;
        File file = null;
        try {
            JSch jsch = new JSch();

            session = jsch.getSession(username, server, port);

            // 密码登录
            if (Constant.AUTH_PASSWORD.equals(authType)) {
                session.setPassword(pin);
            } else if (Constant.AUTH_CERTIFICATE.equals(authType)) {

                // 私钥文件登录
                file = new File("privateKey");
                out = new FileOutputStream(file);
                IoUtil.write(out,false,privateKey.getBytes());
                if (StringUtils.isNotEmpty(keyPassword)) {
                    jsch.addIdentity(file.getAbsolutePath(), keyPassword);
                } else {
                    jsch.addIdentity(file.getAbsolutePath());
                }
                session.setUserInfo(new SftpAuthKeyUserInfo(""));
            } else {
                throw new NZException("account auth type not found.");
            }

            Properties sshConfig = new Properties();
            sshConfig.put("StrictHostKeyChecking", "no");
            sshConfig.put("userauth.gssapi-with-mic", "no");
            session.setConfig(sshConfig);

            session.connect(connectTimeOut);
        } catch (JSchException e) {
            throw new NZException("user login SFTP server occur exception:" + e);
        } catch (FileNotFoundException e) {
            throw new NZException("user login SFTP server occur exception:" + e);
        } finally {
            if (out != null) {
                IOUtils.closeQuietly(out);
            }
            if (file != null && file.exists()) {
                file.delete();
            }
        }
    }

    */
/**
     * 上传到sftp
     *
     * @param targetPath 目标服务器安装目录
     * @param path       文件所在web服务地址
     * @param fileName   文件名称
     * @param shellName  脚本名称
     * @throws Exception
     *//*

    public void upload(String targetPath, String path, String fileName, String shellName) throws NZException, JSchException {
        // sftp通道
        Channel channel = session.openChannel("sftp");
        channel.connect(connectTimeOut);
        File file = null;
        sftp = (ChannelSftp) channel;
        InputStream in = null;
        try {
            // node exporter 文件
            file = new File(path + File.separator + fileName);
            in = new FileInputStream(file);

            // 上传文件
            try {
                sftp.cd(targetPath);
            } catch (SftpException e) {
                logger.warn("directory is not exist");
                // 创建文件夹 并进入
                createDir(targetPath);
                sftp.cd(targetPath);
            }
            // 放进sftp中进行传输
            sftp.put(in, file.getName());

            // 脚本文件
            file = new File(path + File.separator + shellName);
            in = new FileInputStream(file);

            // 放进sftp中进行传输
            sftp.put(in, file.getName());
        } catch (FileNotFoundException | SftpException e) {
            // 文件上传失败
            logger.error("build node exporter upload faild {}", e);
            // 抛出 controller 处理
            throw new NZException("build node exporter upload faild");
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error("inputstream close error");
                }
            }
        }
    }

    */
/**
     * 执行命令
     *
     * @param cmd
     *//*

    public void exec(String cmd) {
        try {
            // 执行命令通道
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(cmd);
            channel.connect(connectTimeOut);
        } catch (JSchException e) {
            // 执行默认启动脚本失败
            logger.error("exec init node exporter shell faild");
        }
    }

    */
/**
     * 退出登陆
     *//*

    public void logout() {
        if (sftp != null) {
            if (sftp.isConnected()) {
                sftp.disconnect();
            }
        }
        if (session != null) {
            if (session.isConnected()) {
                session.disconnect();
            }
        }
    }

    */
/**
     * 创建文件目录
     *//*

    private void createDir(String createpath) {
        try {
            if (isDirExist(createpath)) {
                sftp.cd(createpath);
                return;
            }
            String pathArry[] = createpath.split("/");
            StringBuffer filePath = new StringBuffer("/");
            for (String path : pathArry) {
                if (path.equals("")) {
                    continue;
                }
                filePath.append(path + "/");
                if (isDirExist(filePath.toString())) {
                    sftp.cd(filePath.toString());
                } else {
                    // 建立目录
                    sftp.mkdir(filePath.toString());
                    // 进入并设置为当前目录
                    sftp.cd(filePath.toString());
                }
            }
            sftp.cd(createpath);
        } catch (SftpException e) {
            logger.error("创建目标目录失败，目标路径为" + createpath,e);
        }
    }

    */
/**
     * 判断目录是否存在
     *//*

    public boolean isDirExist(String directory) {
        boolean isDirExistFlag = false;
        try {
            SftpATTRS sftpATTRS = sftp.lstat(directory);
            isDirExistFlag = true;
            return sftpATTRS.isDir();
        } catch (SftpException e) {
//            if (e.getMessage().toLowerCase().equals("no such file")) {
            if (StrUtil.equals(e.getMessage().toLowerCase(),"no such file")) {
                isDirExistFlag = false;
            }
        }
        return isDirExistFlag;
    }
}
*/
