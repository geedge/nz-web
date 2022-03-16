package com.nis.modules.ipam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.asset.entity.AssetAsset;
import lombok.Data;

@Data
@TableName("ipam_ip")
public class IpamIp {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 主机名
     */
    private String name;

    /**
     * 子网id
     */
    private Integer subnetId;

    /**
     * 子网地址
     */
    private String addr;

    /**
     * MAC地址
     */
    private String mac;

    /**
     * 资产id
     */
    private Integer assetId;

    /**
     * 状态
     */
    private Integer state;


    /**
     * 备注
     */
    private String remark;

    /**
     * 更新时间戳
     */
    private Long uts;

    private String seq;

    @TableField(exist = false)
    private AssetAsset asset;

    @TableField(exist = false)
    private IpamSubnet ipamSubnet;


}
