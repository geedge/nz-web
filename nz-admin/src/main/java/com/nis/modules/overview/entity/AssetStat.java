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
     * 各类维度统计名称
     */
    private String name;

    /**
     * 资产总数
     */
    private int total;

    /**
     * 在库资产数量
     */
    private int inStock;

    /**
     * 出库资产数量
     */
    private int outStock;

    /**
     * 可达资产数量
     */
    private int pingUp;

    /**
     * 不可达资产数量
     */
    private int pingDown;

    /**
     * 告警消息总数
     */
    private int alertTotal;

    /**
     * 告警级别为 高 的数量
     */
    private int alertHigh;
    /**
     * 告警级别为 中 的数量
     */
    private int alertMedium;
    /**
     * 告警级别为 低 的数量
     */
    private int alertLow;

}
