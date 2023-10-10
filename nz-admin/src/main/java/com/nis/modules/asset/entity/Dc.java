package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.sys.entity.SysUserEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.List;


/**
 * 数据中心实体
 *
 * @author shizhendong
 * @date 2021-04-06
 */
@Data
@TableName("dc")
public class Dc implements Serializable {
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
    @TableField(updateStrategy = FieldStrategy.IGNORED, insertStrategy = FieldStrategy.IGNORED)
    private Double longitude;

    /**
     * 纬度
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED, insertStrategy = FieldStrategy.IGNORED)
    private Double latitude;

    /**
     *  状态
     */
    private String state;

    /**
     * 机房负责人
     */
    @TableField(exist = false)
    private SysUserEntity adminUser;

    /**
     * 机柜计数
     */
    @TableField(exist = false)
    private Integer cabinetNum;

    /**
     * 资产计数
     */
    @TableField(exist = false)
    private Integer assetNum;

    /**
     * alert计数
     */
    @TableField(exist = false)
    private Integer alertNum;

    @TableField(exist = false)
    private List alert;

    @TableField(exist = false)
    private Agent agent;

    /**
     * 是否收藏 0:未收藏 1:已收藏
     */
    @TableField(exist = false)
    private Integer starred;

}
