package com.nis.modules.alert.service;

import com.nis.common.utils.PageUtils;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.entity.AlertSilenceConf;
import com.nis.modules.alert.entity.receive.AlertMessageReceiveDTO;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 
 *
 */
public interface AlertHandlerService  {

	AlertMessageEntity transAlertMessageEntity(AlertMessageReceiveDTO dto);

	/**
	 * 判断是否命中 静默规则
	 * @param message
	 * @return
	 */
	List<AlertSilenceConf> judgeAlertMessageSilence(AlertMessageEntity message);

	/**
	 * 1、更新 silence || active message endat
	 * 2、更新 redis key 超时时间
	 * @param message
	 */
	void updateMessageEndAt(AlertMessageEntity message);
	
	List<AlertMessageEntity> selectMessagegtNow(String tableName,Integer limit);
	
	/**
	 * 查询失效的silence 配置
	 * @return
	 */
	List<AlertSilenceConf> selectActiveToExpiredSilence();
	
	/**
	 * 查询将要生效的 silence 配置
	 * @return
	 */
	List<AlertSilenceConf> selectPendingToActiveSilence();

	/**
	 * 批量更新 silence conf state
	 * @param state
	 * @param silencConfId
	 */
	void updateSilenceConfStateBatch(int state, Integer ... silencConfId);
	/**
	 * 处理 active 状态变更为 expired 的silence 配置
	 * 同时需要修改 alert message
	 * @param silenceList
	 */
	void handleSilenceConfActiveToExired(List<AlertSilenceConf> silenceList);
	/**
	 * 处理 pending 状态变更为 active 的slience 配置
	 * 同时判断  alert_message_active 表中有符合条件的告警
	 * @param silenceList
	 */
	void handleSilenceConfPendingToActive(List<AlertSilenceConf> silenceList);

	/**
	 * 分页查询 告警消息
	 * @param params
	 * @return
	 */
	PageUtils queryMessagePage(Map<String, Object> params);
	
	/**
	 * 根据查询条件 返回告警消息统计
	 * @param params
	 * @return
	 */
	Map<String,List> queryMessageGroupBy(Map<String,Object> params);

	void exportMessage(HttpServletResponse response, Map<String, Object> params) throws IOException;

	/**
	 * 删除告警消息
	 * @param state
	 * @param ids
	 */
	void removeMessageByIds(Integer state, Long ...ids);

	AlertMessageEntity getAlertMessageLinkObject(AlertMessageEntity message);

	/**
	 * 保存 active 消息
	 * @param msgList
	 */
	void handleMessageActiveBatch(List<AlertMessageEntity> msgList);

	/**
	 * 保存silence 消息
	 * @param msgList
	 */
	void handleMessageSilenceBatch(List<AlertMessageEntity> msgList);

	/**
	 * 保存到 expired 消息
	 * @param msgList
	 * @param fromState
	 */
	void handleMessageExpiredBatch(List<AlertMessageEntity> msgList, Integer fromState);

	/**
	 * 更新 auto expired = 0 的告警消息 endat
	 * @param endAt
	 */
	void updateCloseExpiredMessageEndAt(String tableName,Date endAt);

    AlertMessageEntity queryInfo(Long id);

	PageUtils queryAlertMessageRelList(Map params);
}

