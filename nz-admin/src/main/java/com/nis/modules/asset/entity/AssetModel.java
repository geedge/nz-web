package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.panel.entity.VisualChart;

import lombok.Data;

import java.io.Serializable;
import java.util.List;


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

    /**
     * u位大小
     */
    private Integer usize;

    @TableField(exist = false)
    private AssetBrand brand;
    
    private String chartIds;
    
    @TableField(exist = false)
    private List<VisualChart> charts;
    
    @TableField(exist = false)
    private Integer assetNum;
}
