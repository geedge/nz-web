package com.nis.modules.stat.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.metadata.IPage;
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

	List<StatAnalysis> selectAssetUnhealthy();
	
	List<StatAnalysis> selectDcAssetUnhealthy();
	
	List<StatAnalysis> selectDcEndpointUnhealthy();
	
	List<StatAnalysis> selectDcAlertMessage();
	
	List<StatAnalysis> selectDcAgentUnhealthy();
	
	List<Integer> selectModuleDownEndpoint(@Param("params") Map<String, Object> params);
	
	List<StatAnalysis> selectModuleAlertEndpoint(@Param("params") Map<String, Object> params);
	
	List<StatAnalysis> selectModuleAlertNum(@Param("params") Map<String, Object> params);
	
	List<StatAnalysis> selectModuleAssetRelation(@Param("params") Map<String, Object> params);
	
	List<StatAnalysis> selectAlertMessageNewTrend(@Param("params") Map<String, Object> params);
	
	List<StatAnalysis> selectAlertMessageTotalTrend(@Param("params") Map<String, Object> params);
	
	List<Map<String,Object>> selectAnalyseInfo(@Param("sql") String sql);

	/**
	 * 全局搜索
	 * @param params
	 * @return
	 */
	List<Map<String,Object>> searchAll(IPage page,@Param("params") Map<String, Object> params);
	/**
	 * 全局搜索统计
	 * @param params
	 * @return
	 */
	List<Map<String,Object>> searchAllStatByType(@Param("params") Map<String, Object> params);

	/**
	 * 根据id 获取所在表 序号
	 * @param table
	 * @param id
	 * @param order
	 * @return
	 */
	Integer queryRowNumById(@Param("table") String table, @Param("id") Integer id,@Param("order") String order);

}
