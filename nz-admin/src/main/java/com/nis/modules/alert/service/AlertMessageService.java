package com.nis.modules.alert.service;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.metric.dto.MetricsDto;

/**
 * 
 *
 * @author song
 * @email 
 * @date 2019-11-25 10:11:01
 */
public interface AlertMessageService extends IService<AlertMessageEntity> {

    PageUtils queryPage(Map<String, Object> params);

//	void saveBatch(String receiveStr, HttpServletRequest request) throws Exception;
	
	void removeByAssetIds(List<Integer> idList);

	void removeByModuleIds(List ids);

	void removeByProjectIds(List ids);

	void removeByDcIds(List ids);

	void removeByEndpointIds(List ids);

    void export(HttpServletResponse response, Map<String, Object> params);

    // void updateAlertStateByIds(Map<String, Object> params);

	List<MetricsDto> queryAlertMessageStatisticsInfoByRule();

	Integer getAactiveAlertCountByModuleId(Integer moduleId);

	Integer getAactiveAlertCountByProjectId(Integer projectId);
}

