package com.nis.modules.integration.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("int_alert_rule")
public class IntAlertRuleEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @TableId
    private Integer id;

    /**
     * 集成模块id
     */
    private Integer intModuleId;
    /**
     * 名称
     */
    private String name;

    /**
     * 类型
     */
    private Integer type;

    /**
     * 执行周期
     */
    private Integer inr;

    /**
     * 自动失效超时时间
     */
    private Integer timeout;

    /**
     * 运算表达式
     */
    private String expr;

    /**
     * 条件
     */
    private String cond;

    /**
     * 扩展标签
     */
    private String extLabels;

    /**
     * 单位
     */
    private Integer unit;

    /**
     * 持续时间
     */
    private Integer last;

    /**
     * 告警摘要
     */
    private String summary;

    /**
     * 告警详细描述
     */
    private String description;

    /**
     * 通知用户
     */
    private String receiver;

    /**
     * 通知方式
     */
    private String method;

    /**
     * 告警规则状态
     * 默认：开启
     * 0：关闭
     * 1：开启
     */
    private Integer state;

    /**
     * 是否自动失效
     * 默认：1
     * 1：开启自动失效判断
     * 0：关闭自动失效判断
     */
    private String autoExpired;


    /**
     * 定时是否开启
     * 默认：0
     * 1：开启
     * 0：关闭
     */
    private String schedEnable;


    /**
     * 定时开启天
     * 默认："1,2,3,4,5,6,7"
     * schedEnable = 1 时，不能为空
     */
    private String schedDays;

    /**
     * 定时开启 开始时间
     */
    private String schedStime;

    /**
     * 定时开启 关闭时间
     */
    private String schedEtime;

    /**
     * active通知开关
     * 1：开启
     * 0：关闭
     */
    private String notifyActive;

    /**
     * expired通知开关
     * 1：开启
     * 0：关闭
     */
    private Integer notifyExpired;

    /**
     * trouble shooting
     */
    private String trbShot;

    /**
     * 是否内置
     */
    private Integer buildIn;

    @TableField(exist = false)
    private IntModuleEntity intModule;


}
