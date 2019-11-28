package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;


/**
 * 资产型号实体
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Data
@TableName("model")
public class Model implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId
    private Integer id;

    /**
     * 型号名称
     */
    private String name;

    /**
     * 厂商
     */
    private String vendor;

    /**
     * 类型
     */
    @TableField(strategy = FieldStrategy.IGNORED)
    private Integer type;

    /**
     * 备注
     */
    private String remark;

    /**
     * 码表翻译后的类型名称
     */
    @TableField(exist = false)
    private String typeName;
}