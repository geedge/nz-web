package com.nis.modules.alert.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.alert.entity.AlertRuleSchedLog;

import java.util.List;

public interface AlertRuleSchedLogService extends IService<AlertRuleSchedLog>{

	List<AlertRuleSchedLog> queryListByRuleId(Integer id);
	
}
