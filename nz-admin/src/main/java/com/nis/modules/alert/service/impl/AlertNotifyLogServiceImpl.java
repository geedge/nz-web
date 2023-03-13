package com.nis.modules.alert.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.alert.dao.AlertNotifyLogDao;
import com.nis.modules.alert.entity.AlertNotifyLog;
import com.nis.modules.alert.service.AlertNotifyLogService;
import org.springframework.stereotype.Service;

@Service("alertNotifyLogService")
public class AlertNotifyLogServiceImpl extends ServiceImpl<AlertNotifyLogDao,AlertNotifyLog> implements AlertNotifyLogService{
	
}
