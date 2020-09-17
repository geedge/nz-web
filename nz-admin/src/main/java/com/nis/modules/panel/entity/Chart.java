package com.nis.modules.panel.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.asset.entity.Asset;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@TableName("chart")
public class Chart extends Node{

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
     * 模板ID
     */
    private Integer pid;

    /**
     * 是否为内置规则
     */
    private Integer buildIn;

    /**
     * 导入唯一标识
     */
    private String seq;

    /**
     * 参数
     */
    @TableField(strategy = FieldStrategy.IGNORED)
    private Object param;

    @TableField(exist = false)
    private List<ChartElement> elements;

    /**
     * 同步标识
     */
    @TableField(exist = false)
    private Integer sync;

    /**
     * chart 对应的 asset
     */
    @TableField(exist = false)
    private Asset asset;
}
