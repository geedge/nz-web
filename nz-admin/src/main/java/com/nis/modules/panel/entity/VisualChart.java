package com.nis.modules.panel.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.asset.entity.AssetAsset;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@TableName("visual_chart")
public class VisualChart {

	private Integer id;
	
	private String name;
	
	private Integer panelId;
	
	private Integer groupId;

	private Double span;
	
	private Double height;
	
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

	private Double x;

	private Double y;

	@TableField(exist = false)
    private List<VisualChartElement> elements;

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
    
    @TableField(exist = false)
    private AssetAsset asset;
    
    private Integer varType;
    
    private Integer varId;
    
    @TableField(exist = false)
    private String varName;
    
    private String datasource;
}
