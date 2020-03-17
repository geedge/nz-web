package com.nis.modules.overview.entity;

import lombok.Data;

/**
 * overview  通过dc为维度 endpoint  统计返回实体
 */
@Data
public class EndpointStat {

    /**
     * dc 名称
     */
    private String name;

    /**
     * prom server 总数
     */
    private int promTotal;

    /**
     * prom server 健康计数
     */
    private int promUp;

    /**
     * prom server 挂掉计数
     */
    private int promDown;

    /**
     * endpoint 总数
     */
    private int endpointTotal;

    /**
     * endpoint 健康计数
     */
    private int endpointUp;

    /**
     * endpoint 挂掉计数
     */
    private int endpointDown;

}
