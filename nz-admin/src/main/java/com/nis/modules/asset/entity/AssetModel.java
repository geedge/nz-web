package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.nis.modules.dashboard.entity.VisualChart;
import com.nis.modules.dashboard.entity.VisualDashboard;
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
    @TableId(type = IdType.INPUT)
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

    /**
     * 关联 asset_type_conf.id
     */
    private Integer typeId;

    /**
     * 唯一不重复
     * 用于snmp 自动扫描设备型号识别
     */
    private String sysObjectId;

    /**
     * template id
     */
    @TableField(strategy = FieldStrategy.IGNORED)
    private Integer dashboardId;


    /**
     * TSG 产品
     * 默认： 0
     * 0：非tsg 设备
     * 1：tsg 设备，支持vsys
     */
    private Integer tsgAppliance;

    @TableField(exist = false)
    private AssetTypeConf type;

    @TableField(exist = false)
    private AssetBrand brand;
    
    private String chartIds;
    
    @TableField(exist = false)
    private List<VisualChart> charts;

    @TableField(exist = false)
    private VisualDashboard dashboard;

    @TableField(exist = false)
    private Integer assetNum;

    /**
     * 是否收藏 0:未收藏 1:已收藏
     */
    @TableField(exist = false)
    private Integer starred;
}
