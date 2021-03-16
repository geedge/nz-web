package com.nis.modules.alert.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.alert.dao.AlertNotifyLogDao;
import com.nis.modules.alert.entity.AlertNotifyLog;
import com.nis.modules.alert.service.AlertNotifyLogService;

@Service("alertNotifyLogService")
public class AlertNotifyLogServiceImpl extends ServiceImpl<AlertNotifyLogDao,AlertNotifyLog> implements AlertNotifyLogService{
	
}
