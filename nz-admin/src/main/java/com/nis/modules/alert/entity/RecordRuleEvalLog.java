package com.nis.modules.alert.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

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
