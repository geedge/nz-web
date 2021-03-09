package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;


/**
 * 资产状态配置实体
 *
 * @author shizhendong
 * @date 2021-03-08
 */
@Data
@TableName("asset_state_conf")
public class AssetStateConf implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 名称
     */
    private String name;

    /**
     * 开启ping 0：关闭  1：开启
     */
    private Integer ping;

    /**
     * 开启监控 0：关闭  1：开启
     */
    private Integer monitor;

    /**
     * 开启告警 0：关闭  1：开启
     */
    private Integer alert;

    /**
     * 是否内置 1:内置，0：非内置，默认：0 内置数据不允许删除
     */
    private Integer buildIn;

    /**
     * 描述
     */
    private String remark;

    /**
     * 资产数量统计
     */
    @TableField(exist = false)
    private Integer assetStat;
}
