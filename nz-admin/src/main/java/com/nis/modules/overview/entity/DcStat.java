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
    private int assetTotal;

    /**
     * 在库资产数量
     */
    private int assetInStock;

    /***
     * 出库资产数量
     */
    private int assetOutStock;

    /**
     * 可达资产数量
     */
    private int assetPingUp;

    /**
     * 不可达资产数量
     */
    private int assetPingDown;

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

    public DcStat() {
    }

    public DcStat(int assetTotal, int assetInStock, int assetOutStock, int assetPingUp, int assetPingDown, int alertTotal, int alertHigh, int alertMedium, int alertLow) {
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
