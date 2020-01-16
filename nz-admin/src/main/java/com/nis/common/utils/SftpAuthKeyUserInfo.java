package com.nis.common.utils;

import com.jcraft.jsch.UserInfo;

/**
 * 口令所需
 * sftp登录时为sshkey时，要执行密钥文件并输入口令，当前没有在使用
 */
public class SftpAuthKeyUserInfo implements UserInfo {
    /**
     * ssh private key passphrase
     */
    private String passphrase;

    public SftpAuthKeyUserInfo (String passphrase) {
        this.passphrase = passphrase;
    }

    @Override
    public String getPassphrase() {
        return passphrase;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public boolean promptPassphrase(String s) {
        return true;
    }

    @Override
    public boolean promptPassword(String s) {
        return false;
    }

    @Override
    public boolean promptYesNo(String s) {
        return true;
    }
    
    @Override
    public void showMessage(String message) {
        //log.info ("SSH Message:{}", message);
    }
}