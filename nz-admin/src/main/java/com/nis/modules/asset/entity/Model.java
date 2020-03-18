package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.overview.entity.AssetStat;
import com.nis.modules.sys.entity.SysDictEntity;
import lombok.Data;

import java.io.Serializable;


/**
 * 资产型号实体
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Data
@TableName("model")
public class Model implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId
    private Integer id;

    /**
     * 型号名称
     */
    private String name;

    /**
     * 厂商
     */
    @TableField(value = "vendor")
    private Integer vendorCode;

    /**
     * 类型
     */
    @TableField(value = "type")
    private Integer typeCode;

    /**
     * 备注
     */
    private String remark;

    /**
     * 用于撤销具体某次批量导入的uuid
     */
    private String seq;

    /**
     * 厂商对象 (字典表)
     */
    @TableField(exist = false)
    private SysDictEntity vendor;

    /**
     * 资产类型对象 (字典表)
     */
    @TableField(exist = false)
    private SysDictEntity type;

    /**
     * 资产统计
     */
    @TableField(exist = false)
    private AssetStat assetStat;
}