package com.nis.modules.overview.entity;

import lombok.Data;

/**
 * overview module 统计返回实体
 */
@Data
public class ModuleStat {

    /**
     * id
     */
    private Integer id;

    /**
     * module 名称
     */
    private String name;

    /**
     * module 包含的 endpoint 数量
     */
    private int endpointNum;

    /**
     * module 包含的 alert message 数量
     */
    private int alertNum;

}
