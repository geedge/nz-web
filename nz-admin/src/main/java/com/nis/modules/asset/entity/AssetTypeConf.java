package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.List;


/**
 * 资产类型配置实体
 *
 * @author shizhendong
 * @date 2021-03-08
 */
@Data
@TableName("asset_type_conf")
public class AssetTypeConf implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 父id
     */
    private Integer pid;

    /**
     * 父名称
     */
    @TableField(exist = false)
    private String pname;

    /**
     * 是否为虚拟机宿主  0:不是宿主机，不支持挂载 虚拟机 1:是宿主机，支持挂载虚拟机
     */
    private Integer vm;

    /**
     * 是否为虚拟机宿主  0:不是宿主机，不支持挂载 虚拟机 1:是宿主机，支持挂载虚拟机
     */
    private Integer vmh;

    /**
     * 名称
     */
    private String name;
    
    /**
     * 认证协议
     */
    private Integer authProtocol;
    
    /**
     * 是否支持snmp
     */
    private Integer snmpEnable;
    
    /**
     * 是否开启snmp采集
     */
    private Integer snmpCollect;
    
    /**
     * 是否开启ssh采集
     */
    private Integer sshCollect; 
    
    /**
     * ssh自动化采集脚本路径
     */
    private String sshCollectScript;

    /**
     * 是否支持 nz 客户端（talon）  默认：0   0：不开启  1：开启
     */
    private Integer clientEnable;
    
    /**
     * 描述
     */
    private String remark;

    /**
     * 是否内置 1:内置，0：非内置，默认：0 内置数据不允许删除
     */
    private Integer buildIn;

    /**
     * 子 配置项集合
     */
    @TableField(exist = false)
    private List<AssetTypeConf> children;

}
