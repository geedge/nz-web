package com.nis.modules.stat.service;

import java.util.List;
import java.util.Map;
import com.nis.modules.stat.entity.StatAnalysis;

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
	
	List<StatAnalysis> queryAlertRuleTotal(Map<String,Object> params);
	
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
	
}
