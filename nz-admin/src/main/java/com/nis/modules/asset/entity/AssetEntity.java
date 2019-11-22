package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


/**
 * 资产实体
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Data
@TableName("asset")
public class AssetEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 资产序列化 (唯一)
     */
    private String sn;

    /**
     * 产品型号ID 与model表关联
     */
    private Long modelId;

    /**
     * 数据中心id
     */
    private Long idcId;

    /**
     * 机柜编号
     */
    private Long cabinetId;

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
    private Date purchaseDate;

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
}