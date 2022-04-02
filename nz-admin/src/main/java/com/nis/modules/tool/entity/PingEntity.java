package com.nis.modules.tool.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PingEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * ip地址
     */
    private List<String> ips;

    /**
     * 包大小
     * 默认：64
     */
    private Integer packetsize;

    /**
     * 执行次数
     * 默认：4
     */
    private Integer count;

    /**
     * 超时时间
     * 默认：10s
     */
    private Integer timeout;

    /**
     * 并发线程数量，默认：10
     */
    private Integer concurrent;

    /**
     * maxHops
     */
    private Integer maxHops;
}
