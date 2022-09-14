package com.nis.modules.vsys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("vsys_asset_rel")
public class VsysAssetRel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 资产id
     */
    private Integer assetId;

    /**
     * vsys id
     */
    private Integer vsysId;

    /**
     * 时间戳
     */
    private Long uts;

    /**
     * rel asset obj
     */
    @TableField(exist = false)
    private Object asset;

}
