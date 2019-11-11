package com.nis.modules.dashboard.entity;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class ChartStatisticalEntity implements Serializable{
	 private static final long serialVersionUID = 1L;
	 private Integer   bars=0;      			// 是否以柱状图展示 0-不展示 1-展示
	 private Integer   lines=0;     			// 是否以线条图形展示 0-不展示 1-展示
	 private Integer   points=0;    			// 是否以点展示 0-不展示 1-展示
	 private Integer   fill;        			// 系列的颜色填充量 0代表没有
	 private Integer   lineWidth;   			// 线条的宽度
	 private String    title;       			// 标题
	 private Integer   yaxes;       			// 是否展示左Y轴 0-不展示 1-展示
	 private Integer   yaxis;       			// 是否展示右Y轴 0-不展示 1-展示
	 private Integer   xaxis;       			// 是否展示X轴 0-不展示 1-展示
	 private String    yaxesName;  			// Y轴自定义名称
	 private String    yaxisName;  			// Y轴自定义名称
	 private String    xaxisName;  			// X轴自定义名称
	 private Integer   legend=0;      		// 是否展示legend 
	 private String    legendXPosition;     	// legendX设置位置
	 private String    legendYPosition;     	// legendY设置位置
	 private String    descr;                 // 描述信息
	 private List<UserSql>  userSqls;      			// 用户sql
	 private String    series;       			//  结果数据
	 private String    describe;
}
