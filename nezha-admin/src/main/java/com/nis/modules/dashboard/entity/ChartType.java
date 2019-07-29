package com.nis.modules.dashboard.entity;

import java.util.List;

public enum ChartType {
	Pie(1,"pie","/images/icn-pie.png","chartType.pie","chart_pie"),
	Statistical(2,"statistical","/images/icn-graph-panel.png","chartType.statistical","chart_statistical"),
	Single(3,"single","/images/icn-singlestat-panel.png","chartType.single","chart_single"),
	Table(4,"table","/images/icn-table-panel.png","chartType.table","chart_table"),
	Text(5,"text","/images/icn-text-panel.png","chartType.text","chart_text");
	
	private int chartTypeId;
	private String chartTypeName;
	private String iconPath;
	private String i18nChartName;
	private String chartTableName;
	
	private ChartType(int chartTypeId,String chartTypeName,String iconPath,String i18nChartName,String chartTableName) {
		this.chartTypeId=chartTypeId;
		this.chartTypeName=chartTypeName;
		this.iconPath=iconPath;
		this.i18nChartName=i18nChartName;
		this.chartTableName=chartTableName;
	}
	
	public static ChartType getChartNameByTypeId(Integer typeId) {
		ChartType[] values = ChartType.values();
        int length = values.length;
        for (int i = 0; i < length; i++) {
            if (typeId==values[i].chartTypeId) {
                return values[i];
            }
        }
        return null;
	}

	public static List<ChartType> getAllChartTypes(){
		ChartType[] values = ChartType.values();
		return java.util.Arrays.asList(values);
	}
	
	public int getChartTypeId() {
		return chartTypeId;
	}

	public String getChartTypeName() {
		return chartTypeName;
	}

	public String getIconPath() {
		return iconPath;
	}

	public String getI18nChartName() {
		return i18nChartName;
	}

	public String getChartTableName() {
		return chartTableName;
	}
}
