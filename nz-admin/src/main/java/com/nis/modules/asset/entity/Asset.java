package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.nis.modules.terminal.entity.Account;
import com.nis.modules.sys.entity.SysUserEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;


/**
 * 资产实体
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Data
@TableName("asset")
public class Asset implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 资产序列化 (唯一)
     */
    private String sn;

    /**
     * 产品型号ID 与model表关联
     */
    private Integer modelId;

    /**
     * 数据中心id
     */
    private Integer idcId;

    /**
     * 机柜编号
     */
    @TableField(strategy = FieldStrategy.IGNORED)
    private Integer cabinetId;

    /**
     * 主机地址 ip
     */
    private String host;

    /**
     * 资产状态
     */
    private Integer state;

    /**
     * 购买日期
     */
    private String purchaseDate;

    /**
     * 添加日期
     */
    private Date createAt;

    /**
     * 更新日期
     */
    private Date updateAt;

    /**
     * 添加人 user表关联
     */
    private Long createBy;

    /**
     * 更新人
     */
    private Long updateBy;


    /**
     * 用于撤销具体某次批量导入的uuid
     */
    private String seq;

    /**
     * 机柜开始位置
     */
    private Integer cabinetStart;

    /**
     * 机柜结束位置
     */
    private Integer cabinetEnd;

    /**
     * ipmi配置信息，只有当 type = server才有
     */
    @TableField(strategy = FieldStrategy.IGNORED)
    private Object ipmi;

    /**
     * 资产型号
     */
    @TableField(exist = false)
    private Model model;


    /**
     * 数据中心
     */
    @TableField(exist = false)
    private Idc idc;

    /**
     * 机柜
     */
    @TableField(exist = false)
    private Cabinet cabinet;

    /**
     * 创建人
     */
    @TableField(exist = false)
    private SysUserEntity createByUser;


    /**
     * 更新人
     */
    @TableField(exist = false)
    private SysUserEntity updateByUser;

    /**
     * module统计 关联endpoinet 表
     */
    @TableField(exist = false)
    private Integer moduleNum;

    /**
     * alert统计
     */
    @TableField(exist = false)
    private Integer alertNum;

    /**
     * endpoint统计
     */
    @TableField(exist = false)
    private Integer endpointNum;

    /**
     * account 集合
     */
    @TableField(exist = false)
    private List<Account> accounts;

    /**
     * 标识
     * 是否安装node_exporter，1：安装，0：不安装
     */
    @TableField(exist = false)
    private Integer exporter;

    /**
     * ping状态
     */
    @TableField(exist = false)
    private Integer pingState;

    /**
     * ping网络延时
     */
    @TableField(exist = false)
    private Double pingRtt;

    /**
     * ping检查时间
     */
    @TableField(exist = false)
    private Date pingLastUpdate;

    /**
     * 标签
     */
    @TableField(exist = false)
    private List<AssetTag> tags;
}
