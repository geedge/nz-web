package com.nis.modules.alert.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.entity.AlertSilenceMessageRel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 
 * 
 * @author song
 * @email 
 * @date 2019-11-25 10:11:01
 */
@Mapper
public interface AlertHandlerDao {
	/**
	 * 保存message 到不同的表
	 * @param message
	 */
	void saveAlertMessage(AlertMessageEntity message);
	
	void saveMessageBatch(@Param("list")List<AlertMessageEntity> messageList,@Param("tableName")String tableName);
	
	/**
	 * 根据id 查询
	 * @param message
	 * @return
	 */
	AlertMessageEntity selectMessageByIdAndTableName(@Param("messageId")Long messageId,@Param("tableName")String tableName);
	
	/**
	 * 查询所有 alert_message_active
	 * @return
	 */
	List<AlertMessageEntity> selectAllActiveMessage();
	
	
	/**
	 * 根据时间查找 超时未处理的告警消息
	 * @param message
	 * @return
	 */
	List<AlertMessageEntity> selectMessageEndAtltNow(@Param("now")Date now,@Param("tableName")String tableName,@Param("limit") Integer limit);
	
	/**
	 * 根据id 和表名 删除message
	 * @param tableName
	 * @param messageId
	 */
	void deleteMessageByIdsAndTableName(@Param("tableName")String tableName,@Param("array")Long ... ids);
	
	/**
	 * 更新 不同表中 message 的endAt 时间 
	 * @param message
	 */
	void updateMessageEndAt(AlertMessageEntity message);
	
	/**
	 * 保存 message id 与 silence conf id 关系
	 * 校验message 匹配 silence conf 时调用
	 * @param smRels
	 */
	void saveAlertSilenceMessageRel(List<AlertSilenceMessageRel> smRels);
	
	/**
	 * 根据 messageId 查找匹配的 silence conf id
	 * @param messageId
	 * @return
	 */
	List<AlertSilenceMessageRel> selectAlertSilenceIdsByMessageId(Long messageId);
	/**
	 * 根据 silence conf id 查找 对应的 message Id
	 * @param confId
	 * @return
	 */
	List<AlertSilenceMessageRel> selectAlertMessageIdsBySilencConfId(Integer confId);
	/**
	 * 根据messageid 删除 message id与 silence conf id 关于
	 * message  状态由 silence 变为 expired 时 调用
	 * @param messageId
	 */
	void deleteAlertSilenceMessageRelByMessageIds(@Param("array")Long ...messageId);
	/**
	 * 根据confid 删除message 与 silenceconf 关系
	 * 
	 * silence conf 配置 expired 时调用
	 * @param confId
	 */
	void deleteAlertSilenceMessageRelBySilenceConfIds(@Param("array")Integer ...confId);

	List<AlertSilenceMessageRel> selectAlertMessageIdsOneByOneSilencConfId(Integer id);

	/**
	 * 查询告警消息
	 * @param page
	 * @param params
	 * @return
	 */
	List<AlertMessageEntity> queryList(IPage page, Map<String, Object> params);

	List<Map<String, Object>> queryAlertMessageGroupBy(@Param("columnId") String columnId,@Param("columnName") String columnName,@Param("params") Map<String,Object> params);
	/**
	 * 根据 dc_id 查询 alert_message_silence 静默的 message ids
	 * @param dcIds
	 * @return
	 */
	List<Long> selectAlertSilencMessageIdByDcIds(@Param("ids") List dcIds);

	/**
	 * 根据 project_id 查询 alert_message_silence 静默的 message ids
	 * @param projectIdList
	 * @return
	 */
    List<Long> selectAlertSilencMessageIdByProjectIds(@Param("ids")List projectIdList);

	/**
	 * 根据 module_id 查询 alert_message_silence 静默的 message ids
	 * @param moduleIdList
	 * @return
	 */
	List<Long> selectAlertSilencMessageIdByModuleIds(@Param("ids")List moduleIdList);

	/**]
	 * 根据 asset_id 查询 alert_message_silence 静默的 message ids
	 * @param assetIdList
	 * @return
	 */
	List<Long> selectAlertSilencMessageIdByAssetIds(@Param("ids")List<Integer> assetIdList);

	/**
	 * 根据 endpoint_id 查询 alert_message_silence 静默的 message ids
	 * @param endpointIds
	 * @return
	 */
	List<Long> selectAlertSilencMessageIdByEndpointIds(@Param("ids")List endpointIds);

	/**
	 * 根据 rule_id 查询 alert_message_silence 静默的 message ids
	 * @param ruleIds
	 * @return
	 */
	List<Long> selectAlertSilencMessageIdByRuleIds(@Param("ids") List ruleIds);
	
	/**
	 * 更新 不同表中 auto expired = 0 的 message 的endAt 时间 
	 * @param message
	 */
	void updateCloseExpiredMessageEndAt(@Param("tableName")String tableName,@Param("endAt")Date endAt);
}
