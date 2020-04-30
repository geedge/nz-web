package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@TableName("idc_traffic")
public class IdcTraffic implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 数据中心id
     */
    private Integer idcId;

    /**
     * 流量方向 可选值 rx(接收） tx（发送）
     */
    private String direction;

    /**
     * 端口号
     */
    private Integer ifindex;

    /**
     * 端口描述
     */
    private String ifdescr;

    /**
     * 标签 格式为json对象
     */
    private Object tags;

    /**
     * ip地址
     */
    private String host;

    /**
     * 端口 默认161
     */
    private Integer port;

    /**
     * snmp版本 默认v2
    */
    private Integer version;

    /**
     * snmp团体名 默认 public
     */
    private String community;

    /**
     * snmp认证信息
     */
    private Object auth;

    /**
     * 关联idc对象
     */
    @TableField(exist = false)
    private Idc idc;

    @TableField(exist = false)
    private List<IdcTraffic> setting;
}
