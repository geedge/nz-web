package com.nis.modules.integration.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("int_metadata")
public class IntMetadataEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @TableId
    private Integer id;
    /**
     * 名称
     */
    private String name;

    /**
     * 集成模块id
     */
    private Integer intModuleId;

    /**
     * 指标描述
     */
    private String remark;

    /**
     * 指标类型
     */
    private String type;

    /**
     * 状态
     * 0：隐藏
     * 1：显示
     */
    private Integer status;
    /**
     * 是否内置
     */
    private Integer buildIn;
    /**
     * 用户id
     */
    private Integer uid;
    /**
     * 更新时间戳
     */
    private Long uts;

    @TableField(exist = false)
    private IntModuleEntity intModule;
}
