package com.nis.modules.alert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.alert.entity.AlertMessageEntity;

import java.util.List;

/**
 * 
 * @author song
 * @email 
 * @date 2019-11-25 10:11:01
 */
public interface AlertMessageService extends IService<AlertMessageEntity> {

	void removeByAssetIds(List<Integer> idList);

	void removeByModuleIds(List ids);

	void removeByProjectIds(List ids);

	void removeByDcIds(List ids);

	void removeByEndpointIds(List ids);

	void removeByRuleIds(List ids);

	Integer getAactiveAlertCountByModuleId(Integer moduleId);

	Integer getAactiveAlertCountByProjectId(Integer projectId);
}

