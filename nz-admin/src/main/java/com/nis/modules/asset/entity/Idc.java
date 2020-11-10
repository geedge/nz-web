package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.overview.entity.AssetStat;
import com.nis.modules.sys.entity.SysArea;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;


/**
 * 数据中心实体
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Data
@TableName("idc")
public class Idc implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId
    private Integer id;

    /**
     * 数据中心名称
     */
    private String name;

    /**
     * 数据中心位置
     */
    private String location;

    /**
     * 联系电话
     */
    private String tel;

    /**
     * 机房负责人
     */
    private Integer principal;

    /**
     * 用于撤销具体某次批量导入的uuid
     */
    private String seq;
    
    /**
     * 列
     */
    private Integer x;

    /**
     * 行
     */
    private Integer y;

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 纬度
     */
    private Double latitude;

    /**
     *  状态
     */
    private String state;


    /**
     * 资产统计
     */
    @TableField(exist = false)
    private Map assetStat;

    /**
     * 机柜计数
     */
    @TableField(exist = false)
    private Integer cabinetNum;

    /**
     * 告警统计
     */
    @TableField(exist = false)
    private Map alertStat;
}