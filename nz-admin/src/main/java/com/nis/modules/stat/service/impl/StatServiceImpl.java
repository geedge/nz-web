package com.nis.modules.stat.service.impl;


import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Pair;
import com.nis.modules.alert.dao.AlertMessageDao;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.common.exception.NZException;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.common.utils.StringUtils;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.alert.entity.AlertSeverityConf;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.alert.service.AlertSeverityService;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetPing;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetPingService;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
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
import com.nis.modules.sys.entity.Timezone;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserService;
import com.nis.modules.sys.service.TimezoneService;
import com.nis.modules.sys.shiro.ShiroUtils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.log.Log;


@Service
public class StatServiceImpl implements StatService{
	
	private Log logger = Log.get();
	
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
	
	@Autowired
	private SysConfigService sysConfigService;
	
	@Autowired
	private AlertSeverityService alertSeverityConfService;

	@Autowired
	private AlertMessageDao alertMessageDao;

	@Autowired
	private AlertRuleService alertRuleService;

	@Autowired
	private TimezoneService timezoneService;
	
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
		// 按条件查询 asset
		List<AssetAsset> assets = assetAssetService.list(new QueryWrapper<AssetAsset>().lambda()
				.in(ObjectUtil.isNotEmpty(params.get("dcId")), AssetAsset::getDcId, ObjectUtil.isNotEmpty(params.get("dcId")) ? Arrays.asList(params.get("dcId").toString().split(",")) : null)
				.in(ObjectUtil.isNotEmpty(params.get("typeId")), AssetAsset::getTypeId, ObjectUtil.isNotEmpty(params.get("typeId")) ? Arrays.asList(params.get("typeId").toString().split(",")) : null)
				.in(ObjectUtil.isNotEmpty(params.get("modelId")), AssetAsset::getModelId, ObjectUtil.isNotEmpty(params.get("modelId")) ? Arrays.asList(params.get("modelId").toString().split(",")) : null)
				.in(ObjectUtil.isNotEmpty(params.get("stateId")), AssetAsset::getStateId, ObjectUtil.isNotEmpty(params.get("stateId")) ? Arrays.asList(params.get("stateId").toString().split(",")) : null));
		String childrenFlag = ObjectUtil.isNotEmpty(params.get("children")) ? params.get("children").toString() : "1";
		// 总数: 按条件查询出的 asset 数量
		int total = assets.size();
		int alerm, ok;
		// ping 不通 或 有告警的设备，所有 asset 包含子资产的 告警
		List<StatAnalysis> statAnalysis = statDao.selectAssetUnhealthy();
		Set<Integer> alermAssetIdSet = statAnalysis.stream().map(StatAnalysis::getAsset).collect(Collectors.toSet());
		// children = 1 , 如果 子资产存在告警 则 关联父资产也 标识 告警
		if (StrUtil.equals("1", childrenFlag)) {
			// 子资产存在告警的父资产 id set
			Set<Integer> alermAssetPidSet = statAnalysis.stream().map(StatAnalysis::getPid).filter(tempPid -> tempPid != -1).collect(Collectors.toSet());
			alermAssetIdSet.addAll(alermAssetPidSet);
			alerm = (int) assets.stream().filter(asset -> alermAssetIdSet.contains(asset.getId())).count();
			ok = (int) assets.stream().filter(asset -> !alermAssetIdSet.contains(asset.getId())).count();
		} else {
			// 不包括子资产告警
			List<Integer> assetIdList = assets.stream().map(AssetAsset::getId).collect(Collectors.toList());
			alerm = (int) alermAssetIdSet.stream().filter(integer -> assetIdList.contains(integer)).count();
			ok = total - alerm;
		}

		StatAnalysis reslut = new StatAnalysis();
		reslut.setTotal(total);
		reslut.setOk(ok);
		reslut.setAlarm(alerm);
		return reslut;
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

	@Override
	public Map<String,Object> queryAlertMessageTrend(Map<String, Object> params) {
		Map<String,Object> result = new HashMap<String,Object>(); 
		
		// 处理时区问题
		Timezone sysTimezone = timezoneService.getOne(new LambdaQueryWrapper<Timezone>().eq(Timezone::getName, sysConfigService.getValue("timezone")));		
		params.put("timezone", ToolUtil.isEmpty(sysTimezone)? 0 : sysTimezone.getOffset());
		
		if(ToolUtil.isEmpty(params.get("stime"))) {
			params.put("stime", DateUtil.offsetDay(DateUtil.offsetMillisecond(new Date(), ToolUtil.isEmpty(sysTimezone)? 0 : sysTimezone.getOffset()) , -7));
		}else {
			Long startTime = (Long) params.get("stime");
			params.put("stime",DateUtil.offsetMillisecond(new Date(startTime*1000), ToolUtil.isEmpty(sysTimezone)? 0 : sysTimezone.getOffset()));
		}
		
		if(ToolUtil.isEmpty(params.get("etime"))) {
			params.put("etime",DateUtil.offsetMillisecond(new Date(), ToolUtil.isEmpty(sysTimezone)? 0 : sysTimezone.getOffset()));
		}else {
			Long endTime = (Long) params.get("etime");
			params.put("etime",DateUtil.offsetMillisecond(new Date(endTime*1000), ToolUtil.isEmpty(sysTimezone)? 0 : sysTimezone.getOffset()));
		}
		
		if(ToolUtil.isEmpty(params.get("type"))) {
			params.put("type","new");
		}
		
		String formatStep = "%Y-%m-%d %H:00:00";
		if(ToolUtil.isNotEmpty(params.get("step"))) {
			String step = (String)params.get("step");
			if(StringUtils.isEquals(step, "d")) {
				formatStep = "%Y-%m-%d";
			}
		}else {
			params.put("step","h");
		}
		
		params.put("formatStep", formatStep);
		
		List<StatAnalysis> datas = null;
		if(StrUtil.equals("new",(String)params.get("type"))) {
			datas = statDao.selectAlertMessageNewTrend(params);
		}else {
			datas = statDao.selectAlertMessageTotalTrend(params);
		}
		
		Map<Integer,String> map = null;
		if(ToolUtil.isNotEmpty(params.get("dimension"))) {
			String dimension = (String)params.get("dimension");
			if(StrUtil.equals(dimension, "priority")) {
				List<AlertSeverityConf> severitys = alertSeverityConfService.list();
				map = severitys.stream().collect(Collectors.toMap(AlertSeverityConf::getId, AlertSeverityConf::getName));
			}else if (StrUtil.equals(dimension, "datacenter")) {
				List<Dc> dcs = dcService.list();
				map = dcs.stream().collect(Collectors.toMap(Dc::getId, Dc::getName));
			}else if (StrUtil.equals(dimension, "project")){
				List<MonitorProject> projects = monitorProjectService.list();
				map = projects.stream().collect(Collectors.toMap(MonitorProject::getId, MonitorProject::getName));
			}
		}
		
		if(ToolUtil.isNotEmpty(datas)) {
			List<Object> values =new ArrayList<Object>();
			Map<Integer, List<StatAnalysis>> handleDatas = null;
			if(ToolUtil.isNotEmpty(params.get("dimension"))) {
				String dimension = (String)params.get("dimension");
				if(StrUtil.equals(dimension, "priority")) {
					handleDatas = datas.stream().collect(Collectors.groupingBy(StatAnalysis::getSeverityId));
				}else if (StrUtil.equals(dimension, "datacenter")) {
					handleDatas = datas.stream().collect(Collectors.groupingBy(StatAnalysis::getDcId));
				}else if (StrUtil.equals(dimension, "project")){
					handleDatas = datas.stream().collect(Collectors.groupingBy(StatAnalysis::getProjectId));
				}
				
				for(Integer key : map.keySet()) {
					String name = map.get(key);
					List<StatAnalysis> stats = handleDatas.get(key);
					Map<String,String> metric = new HashMap<String,String>();
					metric.put(dimension, name);
					List<Object> vals = new ArrayList<Object>();
					
					if(ToolUtil.isEmpty(stats)) {
						List<Object> val = new ArrayList<Object>();
						val.add(0);
						val.add(0);
						vals.add(val);
					}else {
						for(StatAnalysis stat: stats) {
							List<Object> val = new ArrayList<Object>();
							val.add(DateUtil.parse(stat.getTime()).getTime());
							val.add(stat.getTotal());
							vals.add(val);
						}
					}
					
					Map<Object,Object> value = new HashMap<Object,Object>();
					value.put("metric", metric);
					value.put("values", vals);
					values.add(value);
				}
			}else {
				for(StatAnalysis data : datas) {
					List<Object> value = new ArrayList<Object>();
					value.add(DateUtil.parse(data.getTime()).getTime());
					value.add(data.getNum());
					values.add(value);
				}
			}			
			
			Map<String,List<Object>> handleData = new HashMap<String,List<Object>>();
			handleData.put("values", values);
			List<Map<String,List<Object>>> handleMapData = new ArrayList<Map<String,List<Object>>>();
			handleMapData.add(handleData);
			result.put("resultType", "matrix");
			result.put("result", handleMapData);
		}
		
		return result;
	}
	
	@Override
	public Map<String, Object> queryAnalyse(Map<String,Object> params) {
		String q = (String) params.get("q");
		if(ToolUtil.isEmpty(q)) {
			throw new NZException(RCode.SYSSTAT_Q_IS_NULL);
		}
		q = URLUtil.decode(q);
		String resultType = (String) params.get("resultType");
		// 处理查询结果 resultType 默认为matrix
		if(StrUtil.isBlank(resultType)) {
			resultType = "matrix";
		}
		Map<String,Object> data = new HashMap<String,Object>();
		LinkedHashMap parseObject = null;
		try {
			parseObject = JSONObject.parseObject(q, LinkedHashMap.class);
		}catch(Exception e) {
			throw new NZException(RCode.JSON_PARAM_INVALIDED);
		}
		
		String type = (String) parseObject.get("type");
		if(StrUtil.isBlank(type)) {
			throw new NZException(RCode.SYSSTAT_TYPE_IS_NULL);
		}
		
		String statQueryJson = sysConfigService.getValue("stat_query_json");
		List<Map<String,Object>> allStatMap = JSONArray.parseObject(statQueryJson,List.class);
		List<String> types = allStatMap.stream().map(o-> (String)o.get("name")).collect(Collectors.toList());
		if(!types.contains(type)){
		    throw new NZException(RCode.SYSSTAT_TYPE_INVALIDED);
		}
		Map<String, Map<String, Object>> typeMap = allStatMap.stream().collect(Collectors.toMap(o-> (String)o.get("name"), Function.identity()));
		Object groupParam = parseObject.get("group");		

		String groupName = "";
		String selectStr = "";
		String limitStr = "";
		String metricName ="";
		
		Map<String, Object> sqlParam = typeMap.get(type);
		
		JSONArray select = (JSONArray) parseObject.get("select");	
		if(ObjectUtil.isEmpty(select)) {
			throw new NZException(RCode.SYSSTAT_SELECT_IS_NULL);
		}
		if(ObjectUtil.isNotEmpty(select)) {
			JSONArray selectJson = (JSONArray) sqlParam.get("select");
			Map<String,String> nameColumn = new HashMap<String,String>();
			for(int i = 0;i<selectJson.size();i++) {
				JSONObject g = selectJson.getJSONObject(i);
				String name = g.getString("name");
				String expr = g.getString("expr");
				nameColumn.put(name, expr);
			}
			for(int i = 0;i<select.size();i++) {
				JSONObject s = select.getJSONObject(i);
				String name = (String) s.get("name");
				metricName = name;
				if(i==0) {
					selectStr = StrUtil.concat(false, selectStr,nameColumn.get(name)," as ","'",name,"'");
				}else {
					selectStr = StrUtil.concat(false,selectStr," , ",nameColumn.get(name)," as ","'",name,"'");
				}
			}
		}
		if(ObjectUtil.isNotEmpty(groupParam)) {
			JSONArray group = (JSONArray) parseObject.get("group");	
			JSONArray groupJson = (JSONArray) sqlParam.get("group");
			Map<String,String> nameColumn = new HashMap<String,String>();
			for(int i = 0;i<groupJson.size();i++) {
				JSONObject g = groupJson.getJSONObject(i);
				String name = g.getString("name");
				String expr = g.getString("expr");
				nameColumn.put(name, expr);
			}
			for(int i = 0;i<group.size();i++) {
				JSONObject m = group.getJSONObject(i);
				String name = (String) m.get("name");
				String resultName = name;
				if(StrUtil.isNotBlank(name)) {
					name = nameColumn.get(name);
				}else {
					throw new NZException(RCode.SYSSTAT_NAME_IS_NULL);
				}
				if(i==0) {
					groupName = StrUtil.concat(false, groupName," group by ",name);
				}else {
					groupName = StrUtil.concat(false, groupName,",",name);
				}
				selectStr = StrUtil.concat(false, selectStr,",",name ," as ", "'",resultName,"'");
			}
		}
		Integer limit = (Integer) parseObject.get("limit");
		String sort = (String) parseObject.get("sort");

		// filter 用以拼接在 dataSql 后
		String filterSqlStr = "";
		List<Map<String, Object>> filterParam = (List<Map<String, Object>>) parseObject.get("filter");
		if (ObjectUtil.isNotEmpty(filterParam)) {
			List<String> filterParamNameList = filterParam.stream().map(m -> m.get("name").toString()).collect(Collectors.toList());
			List<Map<String, Object>> filterJson = (List<Map<String, Object>>) sqlParam.get("filter");
			Map<String, Object> filterNameAndObjMap = filterJson.stream().collect(Collectors.toMap(o -> o.get("name").toString(), Function.identity()));

			long nameNotExistCount = filterParamNameList.stream().filter(o -> ObjectUtil.isEmpty(filterNameAndObjMap.get(o))).count();
			if (nameNotExistCount > 0) {
				throw new NZException(RCode.FILTER_NAME_NOTFOUND);
			}

			for (int i = 0; i < filterParam.size(); i++) {
				Map<String, Object> pojo = filterParam.get(i);
				String name = pojo.get("name").toString();
				String value = pojo.get("value").toString();
				String oper = pojo.get("oper").toString();

				Map<String, Object> fieldObjInfoMap = (Map<String, Object>) filterNameAndObjMap.get(name);
				List<Map<String, String>> ruleRegexAndTipMapList = (List<Map<String, String>>) fieldObjInfoMap.get("rule");
				for (Map<String, String> ruleRegexAndTipMap : ruleRegexAndTipMapList) {
					if (!Tool.ReUtil.isMatch(ruleRegexAndTipMap.get("regex"), value)) {
						throw new NZException(RCode.valueOf(ruleRegexAndTipMap.get("tip").toUpperCase()));
					}
				}

				List<Map<String, String>> operAndExprMapList = (List<Map<String, String>>) fieldObjInfoMap.get("opers");
				Map<String, String> operAndExprMap = operAndExprMapList.stream().filter(m -> oper.equals(m.get("name"))).findFirst().orElse(null);
				if (ObjectUtil.isEmpty(operAndExprMap)) {
					throw new NZException(RCode.FILTER_OPER_INVALIDED);
				}

				String expr = Tool.NzExpressionUtil.renderExpression(operAndExprMap.get("expr"), pojo);
				filterSqlStr += StrUtil.concat(false, expr, (i == filterParam.size() - 1 ? "" : " and "));
			}
		}

		// 组装sql
		String dataSql = (String) sqlParam.get("datasql");
		if(StrUtil.isNotBlank(sort)) {
			sort = StrUtil.concat(false,  " order by ", metricName," ", sort);
		}
		if(limit > 0 ) {
			limitStr = StrUtil.concat(false, " limit ",limit.toString());
		}
		if (StrUtil.isNotBlank(filterSqlStr)) {
			dataSql = StrUtil.concat(false, dataSql, " where ", filterSqlStr);
		}
		String sql = StrUtil.concat(false, "select ",selectStr ," from ", "(" , dataSql , ") t" , groupName , sort , limitStr);

		logger.info(String.format("queryAnalyse sql : %s", sql));
		
		List<Map<String,Object>> queryInfo = statDao.selectAnalyseInfo(sql);
		
		List<Map<String,Object>> result = new ArrayList<Map<String,Object>>();
		if(ObjectUtil.isNotEmpty(queryInfo)) {
			if(StrUtil.equals(resultType, "matrix")) {
				for(Map<String,Object> r : queryInfo) {
					Map val = new HashMap();
					List<Object> values = new ArrayList<Object>();
					Map metric = new HashMap();
					for(String key : r.keySet()) {
						if(StrUtil.equals(key, metricName)) {
							List<Object> l = new ArrayList<Object>();
							l.add(new Date().getTime());
							l.add(r.get(key));
							values.add(l);
							metric.put("__name__",key);
						}else {
							metric.put(key, r.get(key));
						}
					}
					val.put("values", values);
					val.put("metric", metric);
					result.add(val);
				}
			}else {
				result = queryInfo;
			}
		}
		data.put("resultType", resultType);
		data.put("result", result);
		return data;
	}

	@Override
	public PageUtils searchAll(Map<String, Object> params) {
		IPage page = new Query(Map.class).getPage(params);
		List<Map<String,Object>> list = statDao.searchAll(page,params);
		page.setRecords(list);
        return new PageUtils(page);
	}

	@Override
	public List<Map<String,Object>> searchAllStatByType(Map<String, Object> params) {
		List<Map<String,Object>> list = statDao.searchAllStatByType(params);
        return list;
	}
	
	@Override
	public Map<String,Object> queryRowNumById(String table, Integer id, String order, Integer pageSize) {
		Map<String,Object> result = Tool.MapUtil.newHashMap();
		Integer num = statDao.queryRowNumById(table, id, order);
		num = (num == null) ? 1 : num;
		result.put("rownum", num);
		result.put("pageNo", Tool.NumberUtil.ceilDiv(num, pageSize));
		result.put("pageSize", pageSize);
		return result;
	}

	/**
	 * @param type  asset datacenter project module endpoint
	 * @param id
	 * @param level 1 default
	 * @param limit
	 */
	@Override
	public Map<String, Object> rel(String type, Integer id, Integer level, Integer limit) {
		List nodesList = new ArrayList();
		switch (type.toLowerCase()) {
			case "asset": {
				Map<Integer, String> assetRelLevelMapping = Tool.MapUtil.of(
						Pair.of(1, "asset,datacenter,endpoint"),
						Pair.of(2, "asset,datacenter,endpoint,module"),
						Pair.of(3, "asset,datacenter,endpoint,module,project")
				);
				String statRelObjects = Tool.ObjectUtil.isEmpty(assetRelLevelMapping.get(level)) ? assetRelLevelMapping.get(3) : assetRelLevelMapping.get(level);
				nodesList = this.queryAssetRel(id, statRelObjects);
				break;
			}
			case "datacenter": {
				Map<Integer, String> dcRelLevelMapping = Tool.MapUtil.of(
						Pair.of(1, "datacenter,asset"),
						Pair.of(2, "datacenter,asset,endpoint"),
						Pair.of(3, "datacenter,asset,endpoint,module"),
						Pair.of(4, "datacenter,asset,endpoint,module,project")
				);
				String statRelObjects = Tool.ObjectUtil.isEmpty(dcRelLevelMapping.get(level)) ? dcRelLevelMapping.get(4) : dcRelLevelMapping.get(level);
				nodesList = this.queryDcRel(id, statRelObjects);
				break;
			}
			case "project": {
				Map<Integer, String> projectRelLevelMapping = Tool.MapUtil.of(
						Pair.of(1, "project,module"),
						Pair.of(2, "project,module,endpoint"),
						Pair.of(3, "project,module,endpoint,asset"),
						Pair.of(4, "project,module,endpoint,asset,datacenter")
				);
				String statRelObjects = Tool.ObjectUtil.isEmpty(projectRelLevelMapping.get(level)) ? projectRelLevelMapping.get(4) : projectRelLevelMapping.get(level);
				nodesList = this.queryProjectRel(id, statRelObjects);
				break;
			}
			case "module": {
				Map<Integer, String> moduletRelLevelMapping = Tool.MapUtil.of(
						Pair.of(1, "module,project,endpoint"),
						Pair.of(2, "module,project,endpoint,asset"),
						Pair.of(3, "module,project,endpoint,asset,datacenter")
				);
				String statRelObjects = Tool.ObjectUtil.isEmpty(moduletRelLevelMapping.get(level)) ? moduletRelLevelMapping.get(3) : moduletRelLevelMapping.get(level);
				nodesList = this.queryModuleRel(id, statRelObjects);
				break;
			}
			case "endpoint": {
				Map<Integer, String> endpointRelLevelMapping = Tool.MapUtil.of(
						Pair.of(1, "endpoint,module,asset"),
						Pair.of(2, "endpoint,module,asset,project,datacenter"),
						Pair.of(2, "endpoint,module,asset,project,datacenter")
				);
				String statRelObjects = Tool.ObjectUtil.isEmpty(endpointRelLevelMapping.get(level)) ? endpointRelLevelMapping.get(2) : endpointRelLevelMapping.get(level);
				nodesList = this.queryEndpointRel(id, statRelObjects);
				break;
			}
		}

		if (Tool.ObjectUtil.isNotNull(limit) && nodesList.size() > limit)
			nodesList = Tool.ListUtil.sub(nodesList, 0, limit.intValue());

		List links = this.handelRelLinksInfo(nodesList, type);

		Map map = new LinkedHashMap(4);
		map.put("nodes", nodesList);
		map.put("links", links);
		return map;
	}

	private List queryEndpointRel(Integer id, String statRelObjects) {
		MonitorEndpoint endpoint = monitorEndpointService.getById(id);
		if (Tool.ObjectUtil.isNull(endpoint)) throw new NZException(RCode.ENDPOINT_NOTFOUND);

		List<AlertMessageEntity> alertList = alertMessageDao.getAlertsStatColumn();
		Map<String, List<Integer>> severityNameAndRuleIdsMap = alertRuleService.getSeverityNameAndRuleIdsMap();

		AssetAsset asset = assetAssetService.getById(endpoint.getAssetId());
		MonitorModule module = monitorModuleService.getById(endpoint.getModuleId());

		List nodesList = new ArrayList();
		for (String statRelObjectStr : Tool.StrUtil.split(statRelObjects, ',')) {
			switch (statRelObjectStr) {
				case "asset": {
					Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> asset.getId().equals(alertmsg.getAssetId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
					Map<String, Object> node = this.getStatNodeInfo("asset", asset.getId(), asset.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
					nodesList.add(node);
					break;
				}
				case "datacenter": {
					Dc dc = dcService.getById(asset.getDcId());
					Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> dc.getId().equals(alertmsg.getDcId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
					Map<String, Object> node = this.getStatNodeInfo("datacenter", dc.getId(), dc.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
					nodesList.add(node);
					break;
				}
				case "project": {
					MonitorProject project = monitorProjectService.getById(module.getProjectId());
					Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> project.getId().equals(alertmsg.getProjectId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
					Map<String, Object> node = this.getStatNodeInfo("project", project.getId(), project.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
					nodesList.add(node);
					break;
				}
				case "module": {
					Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> module.getId().equals(alertmsg.getModuleId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
					Map<String, Object> node = this.getStatNodeInfo("module", module.getId(), module.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
					nodesList.add(node);
					break;
				}
				case "endpoint": {
					Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> endpoint.getId().equals(alertmsg.getEndpointId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
					Map<String, Object> node = this.getStatNodeInfo("endpoint", endpoint.getId(), endpoint.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
					nodesList.add(node);
					break;
				}
			}
		}
		return nodesList;
	}

	private List queryModuleRel(Integer id, String statRelObjects) {
		MonitorModule module = monitorModuleService.getById(id);
		if (Tool.ObjectUtil.isNull(module)) throw new NZException(RCode.MODULE_NOTFOUND);

		List<AlertMessageEntity> alertList = alertMessageDao.getAlertsStatColumn();
		Map<String, List<Integer>> severityNameAndRuleIdsMap = alertRuleService.getSeverityNameAndRuleIdsMap();

		List nodesList = new ArrayList();
		for (String statRelObjectStr : Tool.StrUtil.split(statRelObjects, ',')) {
			switch (statRelObjectStr) {
				case "asset": {
					List<AssetAsset> assetList = monitorModuleService.getAssetListByModuleId(module.getId());
					for (AssetAsset asset : assetList) {
						Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> asset.getId().equals(alertmsg.getAssetId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
						Map<String, Object> node = this.getStatNodeInfo("asset", asset.getId(), asset.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
						nodesList.add(node);
					}
					break;
				}
				case "datacenter": {
					List<Dc> dcList = monitorModuleService.getDcListByModuleId(module.getId());
					for (Dc dc : dcList) {
						Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> dc.getId().equals(alertmsg.getDcId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
						Map<String, Object> node = this.getStatNodeInfo("datacenter", dc.getId(), dc.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
						nodesList.add(node);
					}
					break;
				}
				case "project": {
					MonitorProject project = monitorProjectService.getOne(new LambdaQueryWrapper<MonitorProject>().eq(MonitorProject::getId, module.getProjectId()));
					Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> project.getId().equals(alertmsg.getProjectId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
					Map<String, Object> node = this.getStatNodeInfo("project", project.getId(), project.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
					nodesList.add(node);
					break;
				}
				case "module": {
					Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> module.getId().equals(alertmsg.getModuleId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
					Map<String, Object> node = this.getStatNodeInfo("module", module.getId(), module.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
					nodesList.add(node);
					break;
				}
				case "endpoint": {
					List<MonitorEndpoint> endpointList = monitorEndpointService.list(new LambdaQueryWrapper<MonitorEndpoint>().eq(MonitorEndpoint::getModuleId, module.getId()));
					for (MonitorEndpoint endpoint : endpointList) {
						Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> endpoint.getId().equals(alertmsg.getEndpointId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
						Map<String, Object> node = this.getStatNodeInfo("endpoint", endpoint.getId(), endpoint.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
						nodesList.add(node);
					}
					break;
				}
			}
		}
		return nodesList;
	}

	private List queryProjectRel(Integer id, String statRelObjects) {
		MonitorProject project = monitorProjectService.getById(id);
		if (Tool.ObjectUtil.isNull(project)) throw new NZException(RCode.PROJECT_NOTFOUND);

		List<AlertMessageEntity> alertList = alertMessageDao.getAlertsStatColumn();
		Map<String, List<Integer>> severityNameAndRuleIdsMap = alertRuleService.getSeverityNameAndRuleIdsMap();

		List nodesList = new ArrayList();
		for (String statRelObjectStr : Tool.StrUtil.split(statRelObjects, ',')) {
			switch (statRelObjectStr) {
				case "asset": {
					List<AssetAsset> assetList = monitorProjectService.getAssetListByProjectId(project.getId());
					for (AssetAsset asset : assetList) {
						Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> asset.getId().equals(alertmsg.getAssetId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
						Map<String, Object> node = this.getStatNodeInfo("asset", asset.getId(), asset.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
						nodesList.add(node);
					}
					break;
				}
				case "datacenter": {
					List<Dc> dcList = monitorProjectService.getDcListByProjectId(project.getId());
					for (Dc dc : dcList) {
						Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> dc.getId().equals(alertmsg.getDcId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
						Map<String, Object> node = this.getStatNodeInfo("datacenter", dc.getId(), dc.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
						nodesList.add(node);
					}
					break;
				}
				case "project": {
					Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> project.getId().equals(alertmsg.getProjectId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
					Map<String, Object> node = this.getStatNodeInfo("project", project.getId(), project.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
					nodesList.add(node);
					break;
				}
				case "module": {
					List<MonitorModule> moduleList = monitorProjectService.getModuleListByProjectId(project.getId());
					for (MonitorModule module : moduleList) {
						Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> module.getId().equals(alertmsg.getModuleId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
						Map<String, Object> node = this.getStatNodeInfo("module", module.getId(), module.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
						nodesList.add(node);
					}
					break;
				}
				case "endpoint": {
					List<MonitorEndpoint> endpointList = monitorProjectService.getEndpointListByProjectId(project.getId());
					for (MonitorEndpoint endpoint : endpointList) {
						Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> endpoint.getId().equals(alertmsg.getEndpointId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
						Map<String, Object> node = this.getStatNodeInfo("endpoint", endpoint.getId(), endpoint.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
						nodesList.add(node);
					}
					break;
				}
			}
		}
		return nodesList;
	}

	private List queryDcRel(Integer id, String statRelObjects) {
		Dc dc = dcService.getById(id);
		if (Tool.ObjectUtil.isNull(dc)) throw new NZException(RCode.DC_NOT_EXISTS);

		List<AlertMessageEntity> alertList = alertMessageDao.getAlertsStatColumn();
		Map<String, List<Integer>> severityNameAndRuleIdsMap = alertRuleService.getSeverityNameAndRuleIdsMap();

		List nodesList = new ArrayList();
		for (String statRelObjectStr : Tool.StrUtil.split(statRelObjects, ',')) {
			switch (statRelObjectStr) {
				case "asset": {
					List<AssetAsset> assetList = dcService.getAssetListByDcId(dc.getId());
					for (AssetAsset asset : assetList) {
						Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> asset.getId().equals(alertmsg.getAssetId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
						Map<String, Object> node = this.getStatNodeInfo("asset", asset.getId(), asset.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
						nodesList.add(node);
					}
					break;
				}
				case "datacenter": {
					Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> dc.getId().equals(alertmsg.getDcId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
					Map<String, Object> node = this.getStatNodeInfo("datacenter", dc.getId(), dc.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
					nodesList.add(node);
					break;
				}
				case "project": {
					List<MonitorProject> projectList = dcService.getProjectListByDcId(dc.getId());
					for (MonitorProject project : projectList) {
						Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> project.getId().equals(alertmsg.getProjectId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
						Map<String, Object> node = this.getStatNodeInfo("project", project.getId(), project.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
						nodesList.add(node);
					}
					break;
				}
				case "module": {
					List<MonitorModule> moduleList = dcService.getModuleListByDcId(dc.getId());
					for (MonitorModule module : moduleList) {
						Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> module.getId().equals(alertmsg.getModuleId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
						Map<String, Object> node = this.getStatNodeInfo("module", module.getId(), module.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
						nodesList.add(node);
					}
					break;
				}
				case "endpoint": {
					List<MonitorEndpoint> endpointList = dcService.getEndpointListByDcId(dc.getId());
					for (MonitorEndpoint endpoint : endpointList) {
						Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> endpoint.getId().equals(alertmsg.getEndpointId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
						Map<String, Object> node = this.getStatNodeInfo("endpoint", endpoint.getId(), endpoint.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
						nodesList.add(node);
					}
					break;
				}
			}
		}
		return nodesList;
	}

	private List queryAssetRel(Integer id, String statRelObjects) {
		AssetAsset asset = assetAssetService.getById(id);
		if (Tool.ObjectUtil.isNull(asset)) throw new NZException(RCode.ASSET_NOT_EXIST);

		List<AlertMessageEntity> alertList = alertMessageDao.getAlertsStatColumn();
		Map<String, List<Integer>> severityNameAndRuleIdsMap = alertRuleService.getSeverityNameAndRuleIdsMap();

		List<Map<String, Object>> nodesList = new ArrayList();
		for (String statRelObjectStr : Tool.StrUtil.split(statRelObjects, ',')) {
			switch (statRelObjectStr) {
				case "asset": {
					Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> asset.getId().equals(alertmsg.getAssetId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
					Map<String, Object> node = this.getStatNodeInfo("asset", asset.getId(), asset.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
					nodesList.add(node);
					break;
				}
				case "datacenter": {
					Dc dc = dcService.getById(asset.getDcId());
					Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> dc.getId().equals(alertmsg.getDcId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
					Map<String, Object> node = this.getStatNodeInfo("datacenter", dc.getId(), dc.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
					nodesList.add(node);
					break;
				}
				case "project": {
					List<MonitorProject> projectListByAssetId = assetAssetService.getProjectListByAssetId(asset.getId());
					for (MonitorProject project : projectListByAssetId) {
						Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> project.getId().equals(alertmsg.getProjectId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
						Map<String, Object> node = this.getStatNodeInfo("project", project.getId(), project.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
						nodesList.add(node);
					}
					break;
				}
				case "module": {
					List<MonitorModule> moduleListByAssetId = assetAssetService.getModuleListByAssetId(asset.getId());
					for (MonitorModule module : moduleListByAssetId) {
						Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> module.getId().equals(alertmsg.getModuleId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
						Map<String, Object> node = this.getStatNodeInfo("module", module.getId(), module.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
						nodesList.add(node);
					}
					break;
				}
				case "endpoint": {
					List<MonitorEndpoint> endpointListByAssetId = assetAssetService.getEndpointListByAssetId(asset.getId());
					for (MonitorEndpoint endpoint : endpointListByAssetId) {
						Map<Integer, Long> ruleIdAndAlertCountMap = alertList.parallelStream().filter(alertmsg -> endpoint.getId().equals(alertmsg.getEndpointId())).collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
						Map<String, Object> node = this.getStatNodeInfo("endpoint", endpoint.getId(), endpoint.getName(), ruleIdAndAlertCountMap, severityNameAndRuleIdsMap);
						nodesList.add(node);
					}
					break;
				}
			}
		}
		return nodesList;
	}

	private Map<String, Object> getStatNodeInfo(String type, Integer id, String name, Map<Integer, Long> ruleIdAndAlertCountMap, Map<String, List<Integer>> severityNameAndRuleIdsMap) {
		List<Map<String, Object>> alert = this.getSeverityStatByRuleMsgCount(severityNameAndRuleIdsMap, ruleIdAndAlertCountMap);

		Map<String, Object> statMap = new HashMap<>(8);
		statMap.put("id", String.format("%s-%s", type, id));
		statMap.put("type", type);
		statMap.put("name", name);
		statMap.put("alert", alert);
		return statMap;
	}


	/**
	 * 根据 告警级别对应规则ID MAP & 告警ID对应告警消息数量  返回告警级别对应告警消息总数
	 *
	 * @param severityNameAndRuleIdsMap key = severityName , value = Mapping ruleIds   example:   P1:1,2,3,4  P2:5,6,7,8 ...
	 * @param ruleIdAndAlertCountMap    key = ruleId , value = msgCount                example: 1:10  2:5 ...
	 * @return List<Map < String, Object>> example: [{p1:55},{p2:22}...]
	 */
	@Override
	public List<Map<String, Object>> getSeverityStatByRuleMsgCount(Map<String, List<Integer>> severityNameAndRuleIdsMap, Map<Integer, Long> ruleIdAndAlertCountMap) {
		// 如果 severityNameAndRuleIdsMap 为null 则查库补充
		if (Tool.MapUtil.isEmpty(severityNameAndRuleIdsMap)) {
			severityNameAndRuleIdsMap = alertRuleService.getSeverityNameAndRuleIdsMap();
		}
		List<Map<String, Object>> alert = new ArrayList<>();
		for (Map.Entry<String, List<Integer>> entry : severityNameAndRuleIdsMap.entrySet()) {
			Map<String, Object> severityNameAndRuleNumMap = new HashMap<>(4);
			String severityName = entry.getKey();
			List<Integer> ruleIds = entry.getValue();
			Long ruleNumBySeverityName = 0L;
			for (Integer ruleId : ruleIds) {
				ruleNumBySeverityName += Tool.ObjectUtil.isNull(ruleIdAndAlertCountMap.get(ruleId)) ? 0L : ruleIdAndAlertCountMap.get(ruleId);
			}
			severityNameAndRuleNumMap.put("priority", severityName);
			severityNameAndRuleNumMap.put("num", ruleNumBySeverityName);
			alert.add(severityNameAndRuleNumMap);
		}
		return alert;
	}

	private List handelRelLinksInfo(List<Map<String, Object>> nodesList, String type) {
		// links
		List l = new ArrayList();
		String source = "";
		for (Map<String, Object> n : nodesList) {
			String nodeType = n.get("type").toString();
			if (type.equals(nodeType)) {
				source = n.get("id").toString();
				continue;
			}

			Map t = new HashMap(4);
			t.put("source", source);
			t.put("target", n.get("id"));
			l.add(t);
		}
		return l;
	}
}
