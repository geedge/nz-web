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
     * 停用资产数量
     */
    private long assetSuspended;

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
    private long alertP1;

    /**
     * 告警级别为 中 的数量
     */
    private long alertP2;

    /**
     * 告警级别为 低 的数量
     */
    private long alertP3;

    public DcStat() {
    }

    public DcStat(long assetTotal, long assetInStock, long assetOutStock,long assetSuspended, long assetPingUp, long assetPingDown, long alertTotal, long alertP1, long alertP2, long alertP3) {
        this.assetTotal = assetTotal;
        this.assetInStock = assetInStock;
        this.assetOutStock = assetOutStock;
        this.assetSuspended = assetSuspended;
        this.assetPingUp = assetPingUp;
        this.assetPingDown = assetPingDown;
        this.alertTotal = alertTotal;
        this.alertP1 = alertP1;
        this.alertP2 = alertP2;
        this.alertP3 = alertP3;
    }
}
