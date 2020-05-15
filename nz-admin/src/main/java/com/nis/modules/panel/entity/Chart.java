package com.nis.modules.panel.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@TableName("chart")
public class Chart {

    @TableId
    private Integer id;
    /**
     * 图表id，关联panel表
     * */
    private Integer panelId;
    /**
     *图表标题
     * */
    private String title;
    /**
     * 宽
     * */
    private Integer span;
    /**
     * 高
     * */
    private Integer height;
    /**
     * 创建时间
     * */
    private Date createAt;
    /**
     * 图标类型 line,bar,table
     * */
    private String type;
    
    /**
     * 单位
     */
    private Integer unit;

    /**
     * 排序字段，前一个元素
     */
    private Integer prev;

    /**
     * 排序字段，后一个元素
     */
    private Integer next;

    /**
     * 模板ID
     */
    private Integer pid;

    /**
     * 是否为内置规则
     */
    private Integer buildIn;

    /**
     * 参数
     */
    @TableField(strategy = FieldStrategy.IGNORED)
    private Object param;

    @TableField(exist = false)
    private List<ChartElement> elements;

}
