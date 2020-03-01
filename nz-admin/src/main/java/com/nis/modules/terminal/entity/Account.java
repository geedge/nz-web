package com.nis.modules.terminal.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.terminal.config.Constants;
import com.nis.modules.asset.entity.Asset;
import lombok.Data;

@Data
@TableName("account")
public class Account {
    @TableId
    private Integer id;

    private Integer assetId;
    private Integer authType;
    private String user;

    @TableField(strategy = FieldStrategy.IGNORED)
    private String pwd;

    /*@TableField(strategy = FieldStrategy.IGNORED)
    private byte[] privateKey;*/

    @TableField(strategy = FieldStrategy.IGNORED)
    private String privateKey;

    private Integer port;
    @TableField(exist = false)
    private Asset asset;

    /**
     * 用于撤销具体某次批量导入的uuid
     */
    private String seq;

    /*public byte[] getPrivateKey() {
        //拿本地的公钥解密ssh的私钥
        if (this.privateKey != null) {
            try {
                return RSAUtils.decryptByPrivateKey(this.privateKey, Auth.getPrivateKey());
            } catch (Exception e) {
                throw new RuntimeException("getPrivateKey error!");
            }
        }
        return null;
    }*/

    /*public void setPrivateKey(byte[] privateKey) throws Exception {
        //拿本地的公钥加密ssh的私钥
        if (privateKey!=null) {
            this.privateKey = RSAUtils.encryptByPublicKey(privateKey, Auth.getPublicKey());
        }
    }*/

    public String getPwd(){
        return this.pwd;
    }

    public void setPwd(String pwd){
        this.pwd=pwd;
//        this.pwd=Base64.getEncoder().encodeToString(pwd.getBytes());
    }

    public String getPrivateKeyPath() {
        if(this.getAsset()!=null){
            return Constants.NZ_USER_HOME.concat("/").concat(this.getAsset().getHost()).concat("@").concat(this.getUser()).concat("_id_rsa");
        }
        return null;
    }
    
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Account terminal = (Account) o;

        if (user != null ? !user.equals(terminal.user) : terminal.user != null) return false;
        return user != null ? user.equals(terminal.user) : terminal.user == null;
    }

    @Override
    public int hashCode() {
        int result = user != null ? user.hashCode() : 0;
        result = 31 * result + (user != null ? user.hashCode() : 0);
        return result;
    }
    public enum AuthStatus {
        INITIAL("initial"),
        AUTH_FAIL("authfail"),
        PUBLIC_KEY_FAIL("keyauthfail"),
        GENERIC_FAIL("genericfail"),
        HOST_FAIL("hostfail"),
        SUCCESS("success");
        public String status;

        AuthStatus(String status) {
            this.status = status;
        }

    }
}
