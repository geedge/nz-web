package com.nis.modules.alert.service;

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.alert.entity.AlertMessageEntity;

import java.util.List;
import java.util.Map;

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
	
	/**
     * 根据告警级别统计符合条件的激活状态告警数量
     * @param type 可选值：asset, dc, project, module, endpoint, rule
     * @param id
     * @return List<Map<String,Long>>
     */
    List<Map<String,Object>> getAlertsStatBySeverity(String type,Integer id);

}

