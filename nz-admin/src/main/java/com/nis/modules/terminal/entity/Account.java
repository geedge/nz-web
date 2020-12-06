package com.nis.modules.terminal.entity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.terminal.config.Constants;
import lombok.Data;

import java.util.Map;

@Data
@TableName("account")
public class Account {
    @TableId
    private Integer id;

    private Integer assetId;

    private String protocol;

    private Integer port;

    private Object params;

    private String seq;

    @TableField(exist = false)
    private Asset asset;

    public String getPrivateKeyPath() {
        if(this.getAsset()!=null){
        	Map<String, String> params = (Map)JSON.parse((String)this.getParams());
            String user = params.get("username");
            return Constants.NZ_USER_HOME.concat("/").concat(this.getAsset().getHost()).concat("@").concat(user).concat("_id_rsa");
        }
        return null;
    }
}
