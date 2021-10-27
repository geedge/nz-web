package com.nis.modules.alert.service;


import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.alert.entity.AlertRuleSchedLog;

public interface AlertRuleSchedLogService extends IService<AlertRuleSchedLog>{

	List<AlertRuleSchedLog> queryListByRuleId(Integer id);
	
}
