package com.nis.modules.alert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.entity.receive.AlertMessageReceiveDTO;
import com.nis.modules.metric.dto.MetricsDto;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 
 *
 * @author song
 * @email 
 * @date 2019-11-25 10:11:01
 */
public interface AlertMessageService extends IService<AlertMessageEntity> {

    PageUtils queryPage(Map<String, Object> params) throws Exception;

	void saveBatch(AlertMessageReceiveDTO[] receive) throws Exception;
	
	void removeByAssetIds(List<Integer> idList);

	void removeByModuleIds(List ids);

	void removeByProjectIds(List ids);

    void export(HttpServletResponse response, Map<String, Object> params);

    void updateAlertStateByIds(Map<String, Object> params);

	List<MetricsDto> queryAlertMessageStatisticsInfoByRule();
}

