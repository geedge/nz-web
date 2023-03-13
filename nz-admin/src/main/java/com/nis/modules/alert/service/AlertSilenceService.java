package com.nis.modules.alert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.alert.entity.AlertSilenceConf;

import java.util.Map;

public interface AlertSilenceService extends IService<AlertSilenceConf>{

	PageUtils queryPage(Map<String, Object> params);
	
	AlertSilenceConf queryAlertSilence(Integer id);

	void saveAlertSilenceConf(AlertSilenceConf conf);

	void updateAlertSilenceConf(AlertSilenceConf conf);
	
	void updateAlertSilenceConfExpiredBatch(Integer ...ids);

	PageUtils querySilenceMsgRelObj(Integer silenceId, Map<String, Object> params);
}
