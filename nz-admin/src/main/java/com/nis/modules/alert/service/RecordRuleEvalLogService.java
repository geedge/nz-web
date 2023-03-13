package com.nis.modules.alert.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.alert.entity.RecordRuleEvalLog;

import java.util.List;

public interface RecordRuleEvalLogService extends IService<RecordRuleEvalLog>{
	
	List<RecordRuleEvalLog> queryListByRuleId(Integer id);
	
}
