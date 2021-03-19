package com.nis.modules.module.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
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
    private String type;

    /**
     * endpoint name模板
     * 支持 {{}} 变量替换，asset，module
     * 例：{{asset.name}}-{{module.name}}
     * 最终endpoint的name为： 192.168.40.1-mysql
     */
    private String endpointNameTmpl;

    /**
     * 端口
     */
    private Integer port;


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
}
