package com.nis.modules.panel.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@TableName("chart")
public class Chart {

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
    @TableField(exist = false)
    private List<ChartElement> elements;

}
