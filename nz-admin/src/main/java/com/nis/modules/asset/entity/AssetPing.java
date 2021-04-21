package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("asset_ping")
@AllArgsConstructor
@NoArgsConstructor
public class AssetPing implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * asset id
     */
    private Integer assetId;

    /**
     * 主机地址 ip
     */
    private String host;

    /**
     * 服务器联通状态 可达 为1，不可达为 0
     */
    private Integer status;

    /**
     * 传输时间延迟
     */
    private Double rtt;

    /**
     * 最后更新时间
     */
    private Date lastUpdate;

}
