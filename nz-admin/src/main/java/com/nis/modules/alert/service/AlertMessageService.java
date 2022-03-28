package com.nis.modules.alert.service;

import java.util.List;

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.alert.entity.AlertMessageEntity;

/**
 * 
 */
public interface AlertMessageService extends IService<AlertMessageEntity> {

	void removeByAssetIds(List<Integer> idList);

	void removeByModuleIds(List ids);

	void removeByProjectIds(List ids);

	void removeByDcIds(List ids);

	void removeByEndpointIds(List ids);

	void removeByRuleIds(List ids);

	void removeAllData();

	void leftPushAlertMessageToRedis(String messages);

	void cleanExpiredDataByTime(DateTime startCleanAlertMessageTime);
}

