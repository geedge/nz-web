package com.nis.modules.overview.entity;

import lombok.Data;

/**
 * overview project 统计返回实体
 */
@Data
public class ProjectStat {

    /**
     * project 名称
     */
    private String name;

    /**
     * project 包含的 module 数量
     */
    private int moduleNum;

    /**
     * project 包含的 endpoint 数量
     */
    private int endpointNum;

    /**
     * project 包含的 alert message 数量
     */
    private int alertNum;

}
