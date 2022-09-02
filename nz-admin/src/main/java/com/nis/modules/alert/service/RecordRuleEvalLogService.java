package com.nis.modules.alert.service;


import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.alert.entity.RecordRuleEvalLog;

public interface RecordRuleEvalLogService extends IService<RecordRuleEvalLog>{
	
	List<RecordRuleEvalLog> queryListByRuleId(Integer id);
	
}
