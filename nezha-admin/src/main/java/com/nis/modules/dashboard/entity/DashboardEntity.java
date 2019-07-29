package com.nis.modules.dashboard.entity;

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.sys.shiro.ShiroUtils;

import lombok.Data;

@Data
@TableName("dashboard")
public class DashboardEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	@TableId
	private Long id;
	private String name;
	@NotBlank(message="业务系统ID不能为空")
	private Long systemId;
	@NotBlank(message="仪表盘类型不能为空")
	private Integer chartType;
	@NotNull(message="仪表盘x轴不能为空")
	private Integer x;
	@NotNull(message="仪表盘y轴不能为空")
	private Integer y;
	@NotNull(message="仪表盘宽度不能为空")
	private Integer width;
	@NotNull(message="仪表盘高度不能为空")
	private Integer height;
	private String data;
	private Long operator;
	private Date opTime;
	/*@TableField(exist = false)
	private ChartSingleEntity chartSingleEntity;
	@TableField(exist = false)
	private ChartTableEntity chartTableEntity;
	@TableField(exist = false)
	private ChartStatisticalEntity chartStatisticalEntity;*/
	
	
	public void setOperator() {
		this.operator=ShiroUtils.getUserEntity().getUserId();
	}
	public void setOptime() {
		this.opTime=new Date();
	}
}