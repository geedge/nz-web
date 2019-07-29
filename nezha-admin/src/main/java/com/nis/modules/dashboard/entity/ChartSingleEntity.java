package com.nis.modules.dashboard.entity;

import java.io.Serializable;
import lombok.Data;
@Data
public class ChartSingleEntity implements Serializable{
	private static final long serialVersionUID = 1L;
	private Integer x;					// x轴
	private Integer y;					// y轴
	private Integer height;				// 图形高度
	private Integer width;				// 图形宽度
	private String colorSuffix;			// 后缀字体颜色是否填充 0-不填充 1-填充
	private String colorPrefix;			// 前缀字体颜色是否填充 0-不填充 1-填充
	private String colorValue;			// 值字体颜色是否填充 0-不填充 1-填充
	private Integer decimal;			// 控制图例值显示的小数位数（以及图形悬停工具提示）
	private String suffix;				// 后缀名称
	private Integer suffixFontSize;		// 后缀字体大小
	private String prefix;				// 前缀名称
	private Integer prefixFontSize;		// 前缀字体大小
	private String title;				// 标题
	private Integer valueFontSize;		// 值展示的大小
	private String descr; 				//图表的描述信息
	private Integer isGauge=0;   		//是否开启仪表盘
	private Integer isThreshold=0;		//是否开启阈值
	private Integer isScale=0;   		//是否开启刻度
	private Integer maxValue;   		//最大值
	private Integer minValue;   		//最小值
	private String threshold;  			//阈值
	private String[] userSqls;   		// 用户sql
	private String series;   			//  结果数据
}
