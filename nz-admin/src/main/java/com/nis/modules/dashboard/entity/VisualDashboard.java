package com.nis.modules.dashboard.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@TableName("visual_dashboard")
public class VisualDashboard implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private Integer id;
	
	private String name;
	
	private Integer createBy;
	
	private String type;

	private Integer varType;

	private Integer tmplId;

	private Integer link;
	
	private Integer pid;
	
	private Integer weight;
	
	private Integer buildIn;
	
	private String seq;

	private Object param;

	private String remark;

	private Long uts;
	
	@TableField(exist = false)
    private List<VisualDashboard> children;

    @TableField(exist = false)
    private VisualDashboard parent;
    
    @TableField(exist = false)
    private Integer chartNum;

	@TableField(exist = false)
	private List<VisualChart> charts;

	/**
	 * 是否收藏 0:未收藏 1:已收藏
	 */
	@TableField(exist = false)
	private Integer starred;
}
