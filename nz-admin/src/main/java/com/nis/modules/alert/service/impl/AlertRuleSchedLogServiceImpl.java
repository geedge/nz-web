package com.nis.modules.alert.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.alert.dao.AlertRuleSchedLogDao;
import com.nis.modules.alert.entity.AlertRuleSchedLog;
import com.nis.modules.alert.service.AlertRuleSchedLogService;

@Service("alertRuleSchedLogService")
public class AlertRuleSchedLogServiceImpl extends ServiceImpl<AlertRuleSchedLogDao,AlertRuleSchedLog> implements AlertRuleSchedLogService{
	
}
