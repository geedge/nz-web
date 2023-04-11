package com.nis.modules.stat.service;

import com.nis.common.utils.PageUtils;
import com.nis.modules.alert.entity.AlertSeverityConf;
import com.nis.modules.stat.entity.StatAnalysis;

import java.util.List;
import java.util.Map;

public interface StatService{

	StatAnalysis queryUserTotal();
	
	StatAnalysis queryAssetTotal(Map<String,Object> params);
	
	List<StatAnalysis> queryAssetDc(Map<String,Object> params);
	
	List<StatAnalysis> queryAssetType(Map<String,Object> params);
	
	List<StatAnalysis> queryAssetState(Map<String,Object> params);
	
	List<StatAnalysis> queryAssetModel(Map<String,Object> params);
	
	List<StatAnalysis> queryAssetPing(Map<String,Object> params);
	
	List<StatAnalysis> queryAssetField(Map<String,Object> params);
	
	List<StatAnalysis> queryAssetTopN(Map<String,Object> params);
	
	StatAnalysis queryProjectTotal();
	
	StatAnalysis queryModuleTotal(Map<String,Object> params);
	
	List<StatAnalysis> queryModuleTopN(Map<String,Object> params);
	
	StatAnalysis queryEndpointTotal(Map<String,Object> params);
	
	List<StatAnalysis> queryEndpointType(Map<String,Object> params);
	
	List<StatAnalysis> queryEndpointState(Map<String,Object> params);

	/**
	 * 告警规则总数统计接口
	 * 统计各个告警级别下告警规则总数
	 *
	 * @param params
	 * @return
	 */
	List<StatAnalysis> queryAlertRuleTotal(Map<String, Object> params);
	
	StatAnalysis queryAlertMessageTotal(Map<String,Object> params);
	
	List<StatAnalysis> queryAlertMessageState(Map<String,Object> params);
	
	List<StatAnalysis> queryAlertMessageSeverity(Map<String,Object> params);
	
	List<StatAnalysis> queryAlertMessageTopN(Map<String,Object> params);
	
	StatAnalysis queryDcTotal();
	
	List<StatAnalysis> queryDcState();
	
	StatAnalysis queryAgentTotal(Map<String,Object> params);
	
	List<StatAnalysis> queryAgentState(Map<String,Object> params);
	
	List<StatAnalysis> queryAgentType(Map<String,Object> params);
	
	StatAnalysis queryAssetHealthy(Map<String,Object> params);
	
	List<Map> queryOverviewMap();
	
	List<StatAnalysis> queryModuleAbnormal(Map<String,Object> params);
	
	Map<String,Object> queryAlertMessageTrend(Map<String,Object> params);
	
	Map<String,Object> queryAnalyse(Map<String,Object> params);
	
	/**
	 * 全局检索
	 * @param params
	 * @return
	 */
	PageUtils searchAll(Map<String, Object> params);
	/**
	 * 全局检索统计
	 * @param q
	 * @return
	 */
	List<Map<String, Object>> searchAllStatByType(Map<String, Object> params);
	
	/**
	 * 根据id 查询序号
	 * @param table
	 * @param id
	 * @param order
	 * @return
	 */
	Map<String, Object> queryRowNumById(String table, Integer id, String order, Integer pageSize);


	Map<String, Object> rel(String type, Integer id, Integer level, Integer limit);

	/**
	 * 根据 告警级别对应规则ID MAP & 告警ID对应告警消息数量  返回告警级别对应告警消息总数
	 *
	 * @param severityNameAndRuleIdsMap key = severityName , value = Mapping ruleIds   example:   P1:1,2,3,4  P2:5,6,7,8 ...
	 * @param ruleIdAndAlertCountMap    key = ruleId , value = msgCount                example: 1:10  2:5 ...
	 * @return List<Map < String, Object>> example: [{p1:55},{p2:22}...]
	 */
//	List<Map<String, Object>> getSeverityStatByRuleMsgCount(Map<String, List<Integer>> severityNameAndRuleIdsMap, Map<Integer, Long> ruleIdAndAlertCountMap);
//	List<Map<String, Object>> getSeverityStatByMsgCount(Map<Integer, Long> severityCountMap, Map<Integer, AlertSeverityConf> severityIdMap);
}
