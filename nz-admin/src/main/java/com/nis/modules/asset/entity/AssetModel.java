package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;


/**
 * 资产型号实体
 *
 * @author shizhendong
 * @date 2021-03-16
 */
@Data
@TableName("asset_model")
public class AssetModel implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 品牌id
     */
    private Integer brandId;

    /**
     * 名称
     */
    private String name;

    /**
     * 备注
     */
    private String remark;

    /**
     * 导入seq
     */
    private String seq;

    @TableField(exist = false)
    private AssetBrand brand;
}
