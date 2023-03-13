package com.nis.modules.alert.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("alert_rule_sched_log")
public class AlertRuleSchedLog implements Serializable {
	private static final long serialVersionUID = 1L;

    private Integer id;

    private Integer ruleId;
    
    private Integer ruleState;
    
    private Integer state;
    
    private String msg;
    
    private Long ts;
    
}
