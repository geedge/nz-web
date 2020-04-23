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
     * 资产id
     */
    private Integer assetId;

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
     * 关联idc对象
     */
    @TableField(exist = false)
    private Idc idc;

    /**
     * 关联asset对象
     */
    @TableField(exist = false)
    private Asset asset;

    @TableField(exist = false)
    private List<IdcTraffic> setting;
}
