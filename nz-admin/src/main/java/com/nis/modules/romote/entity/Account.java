package com.nis.modules.romote.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.romote.util.RSAUtils;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

@Data
@TableName("account")
public class Account {
    @TableId
    private Integer id;

    private Integer assetId;
    private Integer authType;
    private String user;
    private String pwd;
    private byte[] privateKey;
    private Integer port;
    @TableField(exist = false)
    private Asset asset;

    public byte[] getPrivateKey() {
        //拿本地的公钥解密ssh的私钥
        if (this.privateKey != null) {
            try {
                return RSAUtils.decryptByPrivateKey(this.privateKey, Auth.getPrivateKey());
            } catch (Exception e) {
                throw new RuntimeException("[JobX] getPrivateKey error!");
            }
        }
        return null;
    }

    public void setPrivateKey(byte[] privateKey) throws Exception {
        //拿本地的公钥加密ssh的私钥
        if (privateKey!=null) {
            this.privateKey = RSAUtils.encryptByPublicKey(privateKey, Auth.getPublicKey());
        }
    }

    public String getPwd(){
        return this.pwd;
    }

    public void setPwd(String pwd){
        this.pwd=pwd;
//        this.pwd=Base64.getEncoder().encodeToString(pwd.getBytes());
    }

    public String getPrivateKeyPath() {
        if(this.getAsset()!=null){
            return Auth.getKeyPath().concat("/").concat(this.getAsset().getHost()).concat("@").concat(this.getUser()).concat("_id_rsa");
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
