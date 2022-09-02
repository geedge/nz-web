package com.nis.modules.alert.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("record_rule_eval_log")
public class RecordRuleEvalLog implements Serializable {
	private static final long serialVersionUID = 1L;

    private Integer id;

    private Integer ruleId;
    
    private Integer state;
    
    private String msg;
    
    private Long stts;
    
    private Long etts;
    
}
