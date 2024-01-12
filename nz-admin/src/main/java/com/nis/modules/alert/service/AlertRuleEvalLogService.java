package com.nis.modules.alert.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.alert.entity.AlertRuleEvalLog;

import java.util.List;

public interface AlertRuleEvalLogService extends IService<AlertRuleEvalLog>{
	
	List<AlertRuleEvalLog> queryListByRuleId(Integer id);

	void syncAlertRuleEvalLogCache(String ruleEvalLogJsonStr);

}
