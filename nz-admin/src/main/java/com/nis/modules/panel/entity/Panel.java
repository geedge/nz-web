package com.nis.modules.panel.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.sys.entity.SysUserEntity;
import lombok.Data;

import java.util.List;

@Data
@TableName("panel")
public class Panel extends Node {

    /**
     * 图表名
     * */
    private String name;
    /**
     *创建人id
     * */
    private Integer createBy;

    /**
     * panel类型
     */
    private String type;

    /**
     * 关联id
     */
    private Integer link;

    /**
     * 是否为内置规则
     */
    private Integer buildIn;

    /**
     * 导入唯一标识
     */
    private String seq;

    /**
     * 父ID
     */
    private Integer pid;

    /**
     * 排序
     */
    private Integer weight;

    @TableField(exist = false)
    private List<Panel> children;

    @TableField(exist = false)
    private SysUserEntity creator;

}
