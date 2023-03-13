package com.nis.modules.dashboard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 表达式模板实体
 *
 * @author shizhendong
 * @date 2021-03-11
 */
@Data
@TableName("visual_expression_tmpl")
public class VisualExpressionTmpl implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 名称
     */
    private String name;

    /**
     * 组名	 默认：default
     */
    private String gname;

    /**
     * 表达式内容
     */
    private String expression;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否内置 1:内置，0：非内置，默认：0
     */
    private Integer buildIn;

    /**
     * 用于导入撤销
     */
    private String seq;

    /**
     * expression中变量 {{}}
     */
    @TableField(exist = false)
    private String[] vars;

    @TableField(exist = false)
    private Map varsVal;

}
