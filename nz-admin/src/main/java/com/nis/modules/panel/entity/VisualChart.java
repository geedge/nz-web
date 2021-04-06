package com.nis.modules.panel.entity;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("visual_chart")
public class VisualChart {

	private Integer id;
	
	private String name;
	
	private Integer panelId;
	
	private Integer groupId;
	
	private Integer span;
	
	private Integer height;
	
	private Integer updateBy;
	
	private Date updateAt;
	
	private String type;
	
	private Integer unit;
	
	private Integer weight;
	
	private Object param;
	
	private Integer pid;
	
	private Integer buildIn;
	
	private String remark;
	
	private String seq;
	
	@TableField(exist = false)
    private List<ChartElement> elements;

    /**
     * 同步标识
     */
    @TableField(exist = false)
    private Integer sync;
    
    @TableField(exist = false)
    private VisualPanel panel;
    
    @TableField(exist = false)
    private VisualChart group;
    
    @TableField(exist = false)
    private List<VisualChart> children;
    
    @TableField(exist = false)
    private Integer chartNums;
}
