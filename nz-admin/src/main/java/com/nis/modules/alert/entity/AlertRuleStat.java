package com.nis.modules.alert.entity;

import lombok.Data;

import java.util.List;

@Data
public class AlertRuleStat {
    /**
     * 分组统计Id
     */
    private Integer id;

    /**
     * 分组列对应名称 endpoint 为 id
     */
    private String name;

    /**
     * 告警消息计数
     */
    private Integer nums;

    private List<AlertRuleStat> module;

    private List<AlertRuleStat> endpoint;
}
