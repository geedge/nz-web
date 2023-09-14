package com.nis.modules.dashboard.entity;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.common.utils.Tool;
import com.nis.modules.asset.entity.AssetAsset;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@TableName("visual_chart")
public class VisualChart implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
	
	private String name;
	
	private Integer dashboardId;
	
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

    /**
     * 同步标识
     */
    @TableField(exist = false)
    private Integer sync;
    
    @TableField(exist = false)
    private VisualDashboard dashboard;
    
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

    private String elementArr;

    @TableField(exist = false)
    private List<Map> elements;

    public void setName(String name) {
        this.name = Tool.StrUtil.trim(name);
    }

    public String getElementArr() {
        return Tool.StrUtil.emptyToDefault(elementArr, "[]");
    }

    public List<Map> getElements() {
        if (Tool.ObjectUtil.isNull(this.elements)) {
            List<Map> visualChartElements = JSONUtil.toList(this.getElementArr(), Map.class);
            return visualChartElements;
        }
        return this.elements;
    }

}
