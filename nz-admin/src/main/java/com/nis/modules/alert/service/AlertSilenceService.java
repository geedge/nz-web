package com.nis.modules.alert.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.entity.AlertSilenceConf;

public interface AlertSilenceService extends IService<AlertSilenceConf>{

	PageUtils queryPage(Map<String, Object> params);
	
	void saveOrUpdateAlertSilence(AlertSilenceConf alertSilenceConf);
	
	void removeByIds(List<Integer> ids);
	
	AlertSilenceConf queryAlertSilence(Integer id);
	
	boolean checkMatchSilence(AlertMessageEntity alertMessageEntity);
	
	void modifySilenceState();
}
