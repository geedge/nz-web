package com.nis.modules.stat.service.impl;


import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetPing;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetPingService;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.entity.MonitorEndpointState;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.endpoint.service.MonitorEndpointStateService;
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
	
	@Autowired
	private AssetPingService assetPingService;
	
	@Autowired
	private MonitorEndpointService monitorEndpointService;
	
	@Autowired
	private MonitorEndpointStateService monitorEndpointStateService;
	
	@Autowired
	private AlertMessageService alertMessageService;
	
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
		}else {
			String size = (String) params.get("size");
			params.put("size", Integer.valueOf(size));
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
		}else {
			String size = (String) params.get("size");
			params.put("size", Integer.valueOf(size));
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
		}else {
			String size = (String) params.get("size");
			params.put("size", Integer.valueOf(size));
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
				.in(ObjectUtil.isNotEmpty(params.get("dcId")), AssetAsset::getDcId, ObjectUtil.isNotEmpty(params.get("dcId")) ? Arrays.asList(params.get("dcId").toString().split(",")) : null)
				.in(ObjectUtil.isNotEmpty(params.get("typeId")),AssetAsset::getTypeId,ObjectUtil.isNotEmpty(params.get("typeId")) ? Arrays.asList(params.get("typeId").toString().split(",")) : null)
				.in(ObjectUtil.isNotEmpty(params.get("modelId")),AssetAsset::getModelId,ObjectUtil.isNotEmpty(params.get("modelId")) ? Arrays.asList(params.get("modelId").toString().split(",")) : null)
				.in(ObjectUtil.isNotEmpty(params.get("stateId")),AssetAsset::getStateId,ObjectUtil.isNotEmpty(params.get("stateId")) ? Arrays.asList(params.get("stateId").toString().split(",")) : null));
		String childrenFlag = ObjectUtil.isNotEmpty(params.get("children")) ? params.get("children").toString() : "1";
		if (StrUtil.equals("0", childrenFlag)) {
			// 不包括子资产, pid = -1 为 顶级资产
			assets = assets.stream().filter(asset -> asset.getPid() == -1).collect(Collectors.toList());
		}
		StatAnalysis statAnalysis = statDao.selectAssetUnhealthy(params);
		statAnalysis.setTotal(assets.size());
		statAnalysis.setOk(assets.size()-statAnalysis.getAlarm());
		return statAnalysis;
	}

	@Override
	public List<Map> queryOverviewMap() {
		List<Map> result= new ArrayList<Map>();
		List<Dc> dcs = dcService.list();
		if(Tool.CollectionUtil.isEmpty(dcs)) {
			return result;
		}
		// 查询dc下asset的数量信息
		List<AssetAsset> assets = assetAssetService.list();
		Map<Integer, List<AssetAsset>> dcAssets = assets.stream().collect(Collectors.groupingBy(AssetAsset::getDcId));
		// 查询dc下asset相关
		List<StatAnalysis> dcAssetUnhealthy = statDao.selectDcAssetUnhealthy();
		Map<Integer, Integer> unhealthyAssets = dcAssetUnhealthy.stream().collect(Collectors.toMap(StatAnalysis::getId,StatAnalysis::getAlarm));
		
		// 查询dc下endpoint相关
//		List<StatAnalysis> dcEndpointAnalyse = statDao.selectDcEndpointUnhealthy();
//		Map<Integer, StatAnalysis> dcEnpoint = dcEndpointAnalyse.stream().collect(Collectors.toMap(StatAnalysis::getId,Function.identity()));
		
		// 查询dc下agent相关
		List<StatAnalysis> dcAgentAnalyse = statDao.selectDcAgentUnhealthy();
		Map<Integer, StatAnalysis> dcAgent = dcAgentAnalyse.stream().collect(Collectors.toMap(StatAnalysis::getId,Function.identity()));

		// 查询dc下告警级别对应的告警数量
		List<StatAnalysis> dcAlerts = statDao.selectDcAlertMessage();
		Map<Integer, List<StatAnalysis>> dcAlert = dcAlerts.stream().collect(Collectors.groupingBy(StatAnalysis::getId));
		
		for(Dc dc : dcs) {
			Map<String,Object> map = new HashMap<String,Object>();
			map.put("id", dc.getId());
			map.put("name", dc.getName());
			map.put("longitude", dc.getLongitude());
			map.put("latitude", dc.getLatitude());
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
			
//			StatAnalysis statAnalysis = dcEnpoint.get(dc.getId());
//			Integer endpointTotal = ToolUtil.isEmpty(statAnalysis)?0:statAnalysis.getTotal();
//			Integer up = ToolUtil.isEmpty(statAnalysis)?0:statAnalysis.getOk();
//			Integer down = endpointTotal - up; 
//			Map endpoint = new HashMap();
//			endpoint.put("up", up);
//			endpoint.put("down", down);
			
			StatAnalysis agentAnalysis = dcAgent.get(dc.getId());
			Integer agentTotal = ToolUtil.isEmpty(agentAnalysis)?0:agentAnalysis.getTotal();
			Integer agentUp = ToolUtil.isEmpty(agentAnalysis)?0:agentAnalysis.getOk();
			Integer agentDown = agentTotal - agentUp; 
			Map agent = new HashMap();
			agent.put("up", agentUp);
			agent.put("down", agentDown);
			
			
			List<StatAnalysis> alerts = dcAlert.get(dc.getId());
			Map alert = new HashMap();
			if(ToolUtil.isNotEmpty(alerts)) {
				for(StatAnalysis data : alerts) {
					alert.put(data.getName(), data.getTotal());
				}
			}
			
			if(agentDown==0 && assetAlarm==0 && ToolUtil.isEmpty(alert)) {
				color = 1;
			}
			
			map.put("color", color);
			map.put("asset", asset);
//			map.put("endpoint", endpoint);
			map.put("agent", agent);
			map.put("alert", alert);
			
			result.add(map);
		}
		return result;
	}

	@Override
	public List<StatAnalysis> queryModuleAbnormal(Map<String, Object> params) {
		String projectId = (String) params.get("projectId");
		String moduleId = (String) params.get("moduleId");
		List<MonitorModule> modules = monitorModuleService.list(new QueryWrapper<MonitorModule>().lambda()
				.eq(ObjectUtil.isNotEmpty(projectId), MonitorModule::getProjectId,projectId)
				.eq(ObjectUtil.isNotEmpty(moduleId), MonitorModule::getId,moduleId));
		
		List<MonitorEndpoint> endpoints = monitorEndpointService.list();
		Map<Integer, Integer> idAndModuleId = endpoints.stream().collect(Collectors.toMap(MonitorEndpoint::getId, MonitorEndpoint::getModuleId));
		
		// 查询module下的alert信息
		List<StatAnalysis> alerts = statDao.selectModuleAlertNum(params);
		Map<Integer, Integer> moduleAndAlertCount = alerts.stream().collect(Collectors.toMap(StatAnalysis::getId, StatAnalysis::getAlert));
		
		Map<Integer,Integer> moduleAndEndpointCount = new HashMap<Integer,Integer>();
		// 查询module下down掉的endpoint信息
		List<Integer> downEndpoints = statDao.selectModuleDownEndpoint(params);
		
		// 查询module下endpoint关联的告警信息
		List<StatAnalysis> alertInfos = statDao.selectModuleAlertEndpoint(params);
		List<Integer> alertEndpoints = alertInfos.stream().map(StatAnalysis::getEndpoint).collect(Collectors.toList());
		
		downEndpoints.addAll(alertEndpoints);
		
		// 有异常的endpoint id信息
		List<Integer> endpointIds = downEndpoints.stream().distinct().collect(Collectors.toList());
		
		// 分组endpoint信息
		if(ObjectUtil.isNotEmpty(endpointIds)) {
			for(Integer id : endpointIds) {
				Integer moduleIdInfo = idAndModuleId.get(id);
				Integer count = moduleAndEndpointCount.get(moduleIdInfo);
				if(ObjectUtil.isEmpty(count)) {
					count = 1;
				}else {
					count += 1;
				}
				moduleAndEndpointCount.put(moduleIdInfo, count);
			}
		}
		
		Map<Integer,Integer> moduleAndAssetCount = new HashMap<Integer,Integer>();
		// 获取asset异常数量  包含ping不通的asset以及有告警的asset信息
		List<Integer> alertAssets = alertInfos.stream().map(StatAnalysis::getAsset).collect(Collectors.toList());
		List<AssetPing> assetPings = assetPingService.list(new QueryWrapper<AssetPing>().lambda().eq(AssetPing::getStatus, 0));
		List<Integer> assetPingErrorIds = assetPings.stream().map(AssetPing::getAssetId).collect(Collectors.toList());
		alertAssets.addAll(assetPingErrorIds);
		// 有异常的asset id信息
		List<Integer> assetIds = alertAssets.stream().distinct().collect(Collectors.toList());
		
		// 获取module和asset的关系
		List<StatAnalysis> moduleAndAsset = statDao.selectModuleAssetRelation(params);
		Map<Integer, List<StatAnalysis>> assetAndModules = moduleAndAsset.stream().collect(Collectors.groupingBy(StatAnalysis::getAsset));
		
		if(ObjectUtil.isNotEmpty(assetIds)) {
			for(Integer assetId : assetIds) {
				List<StatAnalysis> moduleInfos = assetAndModules.get(assetId);
				if(ObjectUtil.isNotEmpty(moduleInfos)) {
					for(StatAnalysis moduleInfo : moduleInfos) {
						Integer count = moduleAndAssetCount.get(moduleInfo.getId());
						if(ObjectUtil.isEmpty(count)) {
							count = 1;
						}else {
							count += 1;
						}
						moduleAndAssetCount.put(moduleInfo.getId(), count);
					}
				}
			}
		}
		
		
		List<StatAnalysis> result = new ArrayList<StatAnalysis>();
		if(ObjectUtil.isNotEmpty(modules)) {
			for(MonitorModule module : modules) {
				Integer alert = moduleAndAlertCount.get(module.getId());
				Integer endpoint = moduleAndEndpointCount.get(module.getId());
				Integer asset = moduleAndAssetCount.get(module.getId());
				Integer state = 1;
				if(ObjectUtil.isNotEmpty(alert) || ObjectUtil.isNotEmpty(endpoint) || ObjectUtil.isNotEmpty(asset)) {
					state = 0;
				}
				
				StatAnalysis data = new StatAnalysis();
				data.setId(module.getId());
				data.setName(module.getName());
				data.setState(state);
				data.setAlert(ObjectUtil.isEmpty(alert)?0:alert);
				data.setEndpoint(ObjectUtil.isEmpty(endpoint)?0:endpoint);
				data.setAsset(ObjectUtil.isEmpty(asset)?0:asset);
				result.add(data);
			}
		}
		return result;
	}
}
