package com.nis.modules.stat.service.impl;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.module.service.MonitorModuleService;
import com.nis.modules.project.entity.MonitorProject;
import com.nis.modules.project.service.MonitorProjectService;
import com.nis.modules.stat.dao.StatDao;
import com.nis.modules.stat.entity.StatAnalysis;
import com.nis.modules.stat.service.StatService;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysUserService;
import com.nis.modules.sys.shiro.ShiroUtils;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;


@Service
public class StatServiceImpl implements StatService{

	@Autowired
	private StatDao statDao;

	@Autowired
	private SysUserService sysUserService;
	
	@Autowired
	private MonitorProjectService monitorProjectService;
	
	@Autowired
	private MonitorModuleService monitorModuleService;
	
	@Autowired
	private DcService dcService;
	
	@Autowired
	private AssetAssetService assetAssetService;
	
	private Integer topNSize=10;
	
	@Override
	public List<StatAnalysis> queryAssetType(Map<String, Object> params) {		
		return statDao.selectAssetTypeAnalysis(params);
	}

	@Override
	public List<StatAnalysis> queryAssetState(Map<String, Object> params) {
		return statDao.selectAssetStateAnalysis(params);
	}

	@Override
	public List<StatAnalysis> queryAssetModel(Map<String, Object> params) {
		return statDao.selectAssetModelAnalysis(params);
	}

	@Override
	public List<StatAnalysis> queryAssetPing(Map<String, Object> params) {
		return statDao.selectAssetPingAnalysis(params);
	}

	@Override
	public List<StatAnalysis> queryAssetField(Map<String, Object> params) {
		return statDao.selectAssetFieldAnalysis(params);
	}

	@Override
	public List<StatAnalysis> queryEndpointType(Map<String, Object> params) {
		return statDao.selectEndpointTypeAnalysis(params);
	}

	@Override
	public List<StatAnalysis> queryEndpointState(Map<String, Object> params) {
		return statDao.selectEndpointStateAnalysis(params);
	}

	@Override
	public List<StatAnalysis> queryAlertMessageState(Map<String, Object> params) {
		return statDao.selectAlertMessageStateAnalysis(params);
	}

	@Override
	public List<StatAnalysis> queryAlertMessageSeverity(Map<String, Object> params) {
		return statDao.selectAlertMessageSeverityAnalysis(params);
	}

	@Override
	public List<StatAnalysis> queryAlertMessageTopN(Map<String, Object> params) {
		if(ObjectUtils.isEmpty(params.get("size"))) {
			params.put("size", topNSize);
		}
		return statDao.selectAlertMessageTopNAnalysis(params);
	}

	@Override
	public List<StatAnalysis> queryDcState() {
		return statDao.selectDcStateAnalysis();
	}

	@Override
	public List<StatAnalysis> queryAgentState(Map<String, Object> params) {
		return statDao.selectAgentStateAnalysis(params);
	}

	@Override
	public List<StatAnalysis> queryAgentType(Map<String, Object> params) {
		return statDao.selectAgentTypeAnalysis(params);
	}

	@Override
	public StatAnalysis queryUserTotal() {
		List<SysUserEntity> total = sysUserService.list();
		Map<String, SysUserEntity> tokens = ShiroUtils.getShiroSessionInfoMap();
		Integer online = 0;
		if(ObjectUtil.isNotEmpty(tokens)) {
			// 防止一个用户多token情况
			Set<Long> userIds = new HashSet<Long>();
			Collection<SysUserEntity> users = tokens.values();
			for(SysUserEntity user : users) {
				userIds.add(user.getId());
			}
			online = userIds.size();
		}
		StatAnalysis result = new StatAnalysis();
		result.setTotal(total.size());
		result.setOnline(online);
		return result;
	}

	@Override
	public StatAnalysis queryAssetTotal(Map<String, Object> params) {
		return statDao.selectAssetTotal(params);
	}

	@Override
	public List<StatAnalysis> queryAssetDc(Map<String, Object> params) {
		return statDao.selectAssetDc(params);
	}

	@Override
	public List<StatAnalysis> queryAssetTopN(Map<String, Object> params) {
		if(ObjectUtils.isEmpty(params.get("size"))) {
			params.put("size", topNSize);
		}
		return statDao.selectAssetTopN(params);
	}

	@Override
	public StatAnalysis queryProjectTotal() {
		List<MonitorProject> list = monitorProjectService.list();
		StatAnalysis result = new StatAnalysis();
		result.setTotal(list.size());
		return result;
	}

	@Override
	public StatAnalysis queryModuleTotal(Map<String,Object> params) {
		List<MonitorModule> list = monitorModuleService.list(new QueryWrapper<MonitorModule>().lambda().eq(ObjectUtil.isNotEmpty(params.get("projectId")),MonitorModule::getProjectId,params.get("projectId")));
		StatAnalysis result = new StatAnalysis();
		result.setTotal(list.size());
		return result;
	}

	@Override
	public List<StatAnalysis> queryModuleTopN(Map<String, Object> params) {
		if(ObjectUtils.isEmpty(params.get("size"))) {
			params.put("size", topNSize);
		}
		return statDao.selectModuleTopN(params);
	}

	@Override
	public StatAnalysis queryEndpointTotal(Map<String, Object> params) {
		return statDao.selectEndpointTotal(params);
	}

	@Override
	public List<StatAnalysis> queryAlertRuleTotal(Map<String, Object> params) {
		return statDao.selectAlertRuleTotal(params);
	}

	@Override
	public StatAnalysis queryAlertMessageTotal(Map<String, Object> params) {
		return statDao.selectAlertMessageTotal(params);
	}

	@Override
	public StatAnalysis queryDcTotal() {
		List<Dc> list = dcService.list();
		StatAnalysis result = new StatAnalysis();
		result.setTotal(list.size());
		return result;
	}

	@Override
	public StatAnalysis queryAgentTotal(Map<String, Object> params) {
		return statDao.selectAgentTotal(params);
	}

	@Override
	public StatAnalysis queryAssetHealthy(Map<String, Object> params) {
		List<AssetAsset> assets = assetAssetService.list(new QueryWrapper<AssetAsset>().lambda()
				.eq(ObjectUtil.isNotEmpty(params.get("dcId")),AssetAsset::getDcId,params.get("dcId"))
				.eq(ObjectUtil.isNotEmpty(params.get("typeId")),AssetAsset::getTypeId,params.get("typeId"))
				.eq(ObjectUtil.isNotEmpty(params.get("modelId")),AssetAsset::getModelId,params.get("modelId"))
				.eq(ObjectUtil.isNotEmpty(params.get("stateId")),AssetAsset::getStateId,params.get("stateId")));
		StatAnalysis statAnalysis = statDao.selectAssetUnhealthy(params);
		statAnalysis.setTotal(assets.size());
		statAnalysis.setOk(assets.size()-statAnalysis.getAlarm());
		return statAnalysis;
	}

	@Override
	public List<Map> queryOverviewMap() {
		List<Map> result= new ArrayList<Map>();
		List<Dc> dcs = dcService.list();
		// 查询dc下asset的数量信息
		List<AssetAsset> assets = assetAssetService.list();
		Map<Integer, List<AssetAsset>> dcAssets = assets.stream().collect(Collectors.groupingBy(AssetAsset::getDcId));
		// 查询dc下asset相关
		List<StatAnalysis> dcAssetUnhealthy = statDao.selectDcAssetUnhealthy();
		Map<Integer, Integer> unhealthyAssets = dcAssetUnhealthy.stream().collect(Collectors.toMap(StatAnalysis::getId,StatAnalysis::getAlarm));
		
		// 查询dc下endpoint相关
		List<StatAnalysis> dcEndpointAnalyse = statDao.selectDcEndpointUnhealthy();
		Map<Integer, StatAnalysis> dcEnpoint = dcEndpointAnalyse.stream().collect(Collectors.toMap(StatAnalysis::getId,Function.identity()));
		
		// 查询dc下告警级别对应的告警数量
		List<StatAnalysis> dcAlerts = statDao.selectDcAlertMessage();
		Map<Integer, List<StatAnalysis>> dcAlert = dcAlerts.stream().collect(Collectors.groupingBy(StatAnalysis::getId));
		
		for(Dc dc : dcs) {
			Map<String,Object> map = new HashMap();
			map.put("name", dc.getName());
			Integer color = 2;
			if(StrUtil.equalsIgnoreCase(dc.getState(), Constant.SWITCH_CLOSE)) {
				color = 3;
				map.put("color", color);
				result.add(map);
				continue;
			}
			
			List<AssetAsset> assetList = dcAssets.get(dc.getId());
			Integer assetTotal = ToolUtil.isEmpty(assetList)?0:assetList.size();
			Integer assetAlarm = ToolUtil.isEmpty(unhealthyAssets.get(dc.getId()))?0:unhealthyAssets.get(dc.getId());
			Integer assetOk = assetTotal - assetAlarm;
			Map asset = new HashMap();
			asset.put("total", assetTotal);
			asset.put("ok", assetOk);
			asset.put("alarm", assetAlarm);
			
			StatAnalysis statAnalysis = dcEnpoint.get(dc.getId());
			Integer endpointTotal = ToolUtil.isEmpty(statAnalysis)?0:statAnalysis.getTotal();
			Integer up = ToolUtil.isEmpty(statAnalysis)?0:statAnalysis.getOk();
			Integer down = endpointTotal - up; 
			Map agent = new HashMap();
			agent.put("up", up);
			agent.put("down", down);
			
			List<StatAnalysis> alerts = dcAlert.get(dc.getId());
			Map alert = new HashMap();
			if(ToolUtil.isNotEmpty(alerts)) {
				for(StatAnalysis data : alerts) {
					alert.put(data.getName(), data.getTotal());
				}
			}
			
			if(ToolUtil.isEmpty(assetAlarm) && assetAlarm==0 && ToolUtil.isEmpty(down) && down==0 && ToolUtil.isEmpty(alert)) {
				color = 1;
			}
			
			map.put("color", color);
			map.put("asset", asset);
			map.put("agent", agent);
			map.put("alert", alert);
			
			result.add(map);
		}
		return result;
	}
}
