package com.nis.modules.overview.entity;

import lombok.Data;

/**
 * overview dc 为维度统计相关信息
 */
@Data
public class DcStat extends EndpointStat {
    /**
     * 资产 总数
     */
    private long assetTotal;

    /**
     * 在库资产数量
     */
    private long assetInStock;

    /***
     * 出库资产数量
     */
    private long assetOutStock;

    /**
     * 可达资产数量
     */
    private long assetPingUp;

    /**
     * 不可达资产数量
     */
    private long assetPingDown;

    /**
     * 告警消息总数
     */
    private long alertTotal;

    /**
     * 告警级别为 高 的数量
     */
    private long alertHigh;

    /**
     * 告警级别为 中 的数量
     */
    private long alertMedium;

    /**
     * 告警级别为 低 的数量
     */
    private long alertLow;

    public DcStat() {
    }

    public DcStat(long assetTotal, long assetInStock, long assetOutStock, long assetPingUp, long assetPingDown, long alertTotal, long alertHigh, long alertMedium, long alertLow) {
        this.assetTotal = assetTotal;
        this.assetInStock = assetInStock;
        this.assetOutStock = assetOutStock;
        this.assetPingUp = assetPingUp;
        this.assetPingDown = assetPingDown;
        this.alertTotal = alertTotal;
        this.alertHigh = alertHigh;
        this.alertMedium = alertMedium;
        this.alertLow = alertLow;
    }
}
