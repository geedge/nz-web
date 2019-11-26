package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;


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
    private Long id;

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
    private Long principal;
}