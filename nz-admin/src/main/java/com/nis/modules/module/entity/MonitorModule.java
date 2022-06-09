package com.nis.modules.module.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.panel.entity.VisualChart;
import com.nis.modules.project.entity.MonitorProject;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@TableName("monitor_module")
public class MonitorModule implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 组件名称
     */
    private String name;

    /**
     * 业务系统id
     */
    private Integer projectId;

    /**
     * modulle 类型 可选值 ： http 、snmp
     */
    @TableField(exist = false)
    private String type;

    /**
     * endpoint name模板
     * 支持 {{}} 变量替换，asset，module
     * 例：{{module.name}}-{{asset.name}}
     * 最终endpoint的name为： mysql-192.168.40.1
     */
    private String endpointNameTmpl;

    /**
     * 描述
     */
    private String remark;

    /**
     * 配置参数
     */
    private String configs;


    /**
     * snmp_exporter需要的配置部分， 由web端根据snmp_param生成
     */
    private String snmpYml;

    /**
     * 是否内置 1:内置，0：非内置，默认：0 内置数据不允许删除
     */
    private Integer buildIn;

    private String seq;

    @TableField(exist = false)
    private MonitorProject project;

    /**
     * endpoint 列表
     */
    @TableField(exist = false)
    private List<MonitorEndpoint> children;

    /**
     * endpoint数量
     */
    @TableField(exist = false)
    private Integer endpointNum;

    /**
     * asset数量
     */
    @TableField(exist = false)
    private Integer assetNum;

    /**
     * alert数量
     */
    @TableField(exist = false)
    private Integer alertNum;
    
    private String chartIds;
    
    @TableField(exist = false)
    private List<VisualChart> charts;

    @TableField(exist = false)
    private List alert;

    /**
     * 是否收藏 0:未收藏 1:已收藏
     */
    @TableField(exist = false)
    private Integer starred;
}
