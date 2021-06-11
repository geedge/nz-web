package com.nis.modules.stat.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nis.modules.stat.entity.StatAnalysis;

@Mapper
public interface StatDao {
	
	List<StatAnalysis> selectAssetTypeAnalysis(@Param("params") Map<String, Object> params);
	
	List<StatAnalysis> selectAssetStateAnalysis(@Param("params") Map<String, Object> params);
	
	List<StatAnalysis> selectAssetModelAnalysis(@Param("params") Map<String, Object> params);
	
	List<StatAnalysis> selectAssetPingAnalysis(@Param("params") Map<String, Object> params);
	
	List<StatAnalysis> selectAssetFieldAnalysis(@Param("params") Map<String, Object> params);
	
	List<StatAnalysis> selectEndpointTypeAnalysis(@Param("params") Map<String, Object> params);
	
	List<StatAnalysis> selectEndpointStateAnalysis(@Param("params") Map<String, Object> params);
	
	List<StatAnalysis> selectAlertMessageStateAnalysis(@Param("params") Map<String, Object> params);
	
	List<StatAnalysis> selectAlertMessageSeverityAnalysis(@Param("params") Map<String, Object> params);
	
	List<StatAnalysis> selectAlertMessageTopNAnalysis(@Param("params") Map<String, Object> params);
	
	List<StatAnalysis> selectDcStateAnalysis();
	
	List<StatAnalysis> selectAgentStateAnalysis(@Param("params") Map<String, Object> params);
	
	List<StatAnalysis> selectAgentTypeAnalysis(@Param("params") Map<String, Object> params);
	
	StatAnalysis selectAssetTotal(@Param("params") Map<String, Object> params);
	
	List<StatAnalysis> selectAssetDc(@Param("params") Map<String, Object> params);
	
	List<StatAnalysis> selectAlertRuleTotal(@Param("params") Map<String, Object> params);
	
	StatAnalysis selectAlertMessageTotal(@Param("params") Map<String, Object> params);
	
	StatAnalysis selectAgentTotal(@Param("params") Map<String, Object> params);
	
	List<StatAnalysis> selectAssetTopN(@Param("params") Map<String, Object> params);
	
	List<StatAnalysis> selectModuleTopN(@Param("params") Map<String, Object> params);
	
	StatAnalysis selectEndpointTotal(@Param("params") Map<String, Object> params);
	
	StatAnalysis selectAssetUnhealthy(@Param("params") Map<String, Object> params);
	
	List<StatAnalysis> selectDcAssetUnhealthy();
	
	List<StatAnalysis> selectDcEndpointUnhealthy();
	
	List<StatAnalysis> selectDcAlertMessage();
	
	List<StatAnalysis> selectDcAgentUnhealthy();
	
	List<Integer> selectModuleDownEndpoint(@Param("params") Map<String, Object> params);
	
	List<StatAnalysis> selectModuleAlertEndpoint(@Param("params") Map<String, Object> params);
	
	List<StatAnalysis> selectModuleAlertNum(@Param("params") Map<String, Object> params);
	
	List<StatAnalysis> selectModuleAssetRelation(@Param("params") Map<String, Object> params);
}
