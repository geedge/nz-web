package com.nis.modules.sys.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * MmMib实体
 */
@Data
public class MmMib implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 名称
     */
    private String name;

    /**
     * 数据类型
     */
    private String type;

    /**
     * oid
     */
    private String objectID;

    /**
     * index 序号
     */
    private List index;

    /**
     *类型关键字
     */
    private String syntax;

    /**
     *访问方式（权限）
     */
    private String access;

    /**
     * 状态
     */
    private String status;

    /**
     * 描述
     */
    private String description;

    /**
     * 父节点
     */
    private String pid;

    /**
     * 子集合
     */
    private List<MmMib> subTree;
}
