package com.nis.modules.overview.entity;

import lombok.Data;
import org.apache.ibatis.type.Alias;

/**
 * overview asset 统计返回实体
 */
@Data
@Alias("assetStatInfo")
public class AssetStat {

    /**
     * id
     */
    private Integer id;

    /**
     * 各类维度统计名称
     */
    private String name;

    /**
     * 以model为维度时使用，代表assetType
     */
    private String type;

    /**
     * 以model为维度时使用，代表vendor
     */
    private String vendor;

    /**
     * 资产总数
     */
    private long total;

    /**
     * 在库资产数量
     */
    private long inStock;

    /**
     * 出库资产数量
     */
    private long outStock;

    /**
     * 停用资产数量
     */
    private long suspended;

    /**
     * 可达资产数量
     */
    private long pingUp;

    /**
     * 不可达资产数量
     */
    private long pingDown;

    /**
     * 告警消息总数
     */
    private long alertTotal;

    /**
     * 告警级别为 高 的数量
     */
    private long alertP1;
    /**
     * 告警级别为 中 的数量
     */
    private long alertP2;
    /**
     * 告警级别为 低 的数量
     */
    private long alertP3;

}
