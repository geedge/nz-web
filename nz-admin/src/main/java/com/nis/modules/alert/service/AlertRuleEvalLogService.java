package com.nis.modules.alert.service;


import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.alert.entity.AlertRuleEvalLog;

public interface AlertRuleEvalLogService extends IService<AlertRuleEvalLog>{
	
	List<AlertRuleEvalLog> queryListByRuleId(Integer id);
	
}
