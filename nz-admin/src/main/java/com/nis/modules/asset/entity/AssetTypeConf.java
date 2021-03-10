package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;


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
     * ssh服务	 0：关闭 1：开启
     */
    private Integer ssh;

    /**
     * telnet服务	 0：关闭 1：开启
     */
    private Integer telnet;

    /**
     * 描述
     */
    private String remark;

    /**
     * 是否内置 1:内置，0：非内置，默认：0 内置数据不允许删除
     */
    private Integer buildIn;

    @Override
    public String toString() {
        return "AssetTypeConf{" +
                "id=" + id +
                ", pid=" + pid +
                ", vm=" + vm +
                ", vmh=" + vmh +
                ", name='" + name + '\'' +
                ", ssh=" + ssh +
                ", telnet=" + telnet +
                ", remark='" + remark + '\'' +
                ", buildIn=" + buildIn +
                '}';
    }
}
