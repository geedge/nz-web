package com.nis.modules.dashboard.entity;

import java.io.Serializable;

import lombok.Data;

@Data
public class ChartTableEntity implements Serializable{
	private static final long serialVersionUID = 1L;
	private Integer fontSize;    	//表格字体大小
    private String title;        	//标题
    private String descr;        	//描述
    private String valueName;    	//值列展示名
    private String timeName;     	//时间列展示名
    private Integer decimal;	 	//小数位格式化
    private String timeFormat;   	//时间格式化
    private Integer pageSize;    	//表格分页数量
    private String[] userSqls;      // 用户sql
	private String series;       	//  结果数据
}
