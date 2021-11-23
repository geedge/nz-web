package com.nis.modules.alert.entity;

import java.io.Serializable;
import java.util.List;

import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.alert.entity.receive.AlertMessageReceiveDTO;

import lombok.Data;

@Data
@TableName("alert_rule_eval_log")
public class AlertRuleEvalLog implements Serializable {
	private static final long serialVersionUID = 1L;

    private Integer id;

    private Integer ruleId;
    
    private Integer state;
    
    private String msg;
    
    private Long stts;
    
    private Long etts;
    
    private List<AlertMessageReceiveDTO> messages;
}
