package com.nis.modules.dashboard.service.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.NamedThreadFactory;
import cn.hutool.core.util.*;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.common.utils.Constant.ChartDatasourceType;
import com.nis.modules.agent.service.LokiApiService;
import com.nis.modules.agent.service.PromApiService;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetModel;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetModelService;
import com.nis.modules.dashboard.dao.VisualDashboardDao;
import com.nis.modules.dashboard.entity.*;
import com.nis.modules.dashboard.service.VisualChartService;
import com.nis.modules.dashboard.service.VisualDashboardService;
import com.nis.modules.dashboard.service.VisualPlaylistService;
import com.nis.modules.dashboard.service.VisualService;
import com.nis.modules.dashboard.utils.DashboardConstant;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.module.service.MonitorModuleService;
import com.nis.modules.report.job.ReportJobManager;
import com.nis.modules.stat.service.StatService;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.entity.SysUserStarredEntity;
import com.nis.modules.sys.service.*;
import com.nis.modules.sys.shiro.ShiroUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.util.TempFile;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service("visualDashboardService")
public class VisualDashboardServiceImpl extends ServiceImpl<VisualDashboardDao, VisualDashboard> implements VisualDashboardService {

	private Log log = Log.get();
	/**
	 * dashboard snapshot 查询超时时间，单位：ms
	 */
	@Value("${dashboard.snapshot.timeout:300000}")
	private Integer timeout;

	@Value("${snapshot.concurrency:10}")
	private Integer snapshotConcurrency;

	@Value("${snapshot.corePoolSize:5}")
	private Integer snapshotCorePoolSize;

	@Value("${snapshot.keepAliveTime:30}")
	private Long snapshotKeepAliveTime;

	@Value("${snapshot.capacity:10000}")
	private Integer snapshotCapacity;

	// snapshot 执行线程池，避免大量请求访问 prometheus 服务
	private ExecutorService snapshotThreadPool;

	@Autowired
	private VisualService visualService;

	@Autowired
	private VisualChartService visualChartService;

	@Autowired
	private VisualPlaylistService playlistService;

	@Autowired
	private DataSourceTransactionManager dataSourceTransactionManager;

	@Autowired
	private BasicImportAndExportServices basicImportAndExportServices;

	@Autowired
	private PromApiService promApiService;

	@Autowired
	private LokiApiService lokiApiService;

	@Autowired
	private AssetModelService assetModelService;

	@Autowired
	private AssetAssetService assetAssetService;

	@Autowired
	private MonitorModuleService monitorModuleService;

	@Autowired
	private MonitorEndpointService monitorEndpointService;

	@Autowired
	private ReportJobManager reportJobManager;

	@Autowired
	private SysI18nService sysI18nService;

	@Autowired
	private SysDictService sysDictService;

	@Autowired
	private SysConfService sysConfService;

	@Autowired
	private SysUserStarredService starredService;

	@Autowired
	private StatService statService;

	@Autowired
	private SysService sysService;

	@Override
	public VisualDashboard queryInfo(Integer id) {
		VisualDashboard dashboard = this.baseMapper.queryInfo(id);
		if (ToolUtil.isNotEmpty(dashboard.getParam()))
			dashboard.setParam(JSONObject.parseObject(String.valueOf(dashboard.getParam()), Map.class));

        // 收藏标识 0：未收藏 1：已收藏
        List<SysUserStarredEntity> tUserStatList = starredService.list(new LambdaQueryWrapper<SysUserStarredEntity>()
                .select(SysUserStarredEntity::getId)
                .eq(SysUserStarredEntity::getTid, dashboard.getId())
                .eq(SysUserStarredEntity::getType, "dashboard")
                .eq(SysUserStarredEntity::getUid, ShiroUtils.getUserId())
                .last("limit 1")
        );
        dashboard.setStarred(CollUtil.isEmpty(tUserStatList) ? 0 : 1);
		return dashboard;
	}

	@Override
	public PageUtils queryList(Map<String, Object> params) {
		String ids = (String) params.get("ids");
		if (StrUtil.isNotBlank(ids)) {
			List<String> listIds = Arrays.asList(ids.split(","));
			params.put("ids", listIds);
		}
		// 获取当前登录用户的id
		Long currentLoginUserID = ShiroUtils.getUserId();
		params.put("currentLoginUserID", currentLoginUserID);
		// 1. 查询时 type 为空则默认 dashboard 类型
		String type = (String) params.get("type");
		if (StringUtils.isEmpty(type)) {
			type = DashboardConstant.Type.DASHBOARD.getValue();
			params.put("type", type);
		}
		boolean isDashboard = StrUtil.equals(DashboardConstant.Type.DASHBOARD.getValue(), type);
		if (isDashboard && StrUtil.isBlank(ids)) params.put("pid", "0");

		IPage page = new Query(VisualDashboard.class).getPage(params);
		List<VisualDashboard> dashboardList = this.baseMapper.queryList(page, params);
		for (VisualDashboard visualDashboard : dashboardList) {
			if (ToolUtil.isNotEmpty(visualDashboard.getParam()))
				visualDashboard.setParam(JSONObject.parseObject(String.valueOf(visualDashboard.getParam()), Map.class));

			if (isDashboard) {
				// 2. dashboard 类型 需要树形结构
				List<VisualDashboard> childrenList = this.list(new LambdaQueryWrapper<VisualDashboard>().eq(VisualDashboard::getPid, visualDashboard.getId()));
				if (CollectionUtils.isNotEmpty(childrenList)) {
					for (VisualDashboard childrenDashboard : childrenList) {
						if (ToolUtil.isNotEmpty(childrenDashboard.getParam()))
							childrenDashboard.setParam(JSONObject.parseObject(String.valueOf(childrenDashboard.getParam()), Map.class));

						// 收藏标识 0：未收藏 1：已收藏
						List<SysUserStarredEntity> tUserStatList = starredService.list(new LambdaQueryWrapper<SysUserStarredEntity>()
								.select(SysUserStarredEntity::getId)
								.eq(SysUserStarredEntity::getTid, childrenDashboard.getId())
								.eq(SysUserStarredEntity::getType, "dashboard")
								.eq(SysUserStarredEntity::getUid, currentLoginUserID.intValue())
								.last("limit 1")
						);
						childrenDashboard.setStarred(CollUtil.isEmpty(tUserStatList) ? 0 : 1);
						// chartNum
						long chartNum = visualChartService.count(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getDashboardId, childrenDashboard.getId()));
						childrenDashboard.setChartNum(Long.valueOf(chartNum).intValue());
					}
					visualDashboard.setChildren(childrenList);
				}
			}
		}
		page.setRecords(dashboardList);
		return new PageUtils(page);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delDashboardsAndChartInfoByDashboardIds(Set<Integer> dashboardIds) {
		if (CollectionUtils.isNotEmpty(dashboardIds)) {
			// dashboard
			this.removeByIds(dashboardIds);

			// chart
			visualChartService.remove(new LambdaUpdateWrapper<VisualChart>().in(VisualChart::getDashboardId, dashboardIds));
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delDashboards(Integer... ids) {
		if (ToolUtil.isEmpty(ids)) {
			return;
		}

		List<VisualDashboard> dashboards = this.list();

		// 1. 校验内置dashboard不能被删除
		this.validateBuildInDashboards(dashboards, ids);

		// 获取这些节点的所有子节点
		List<Integer> idList = Arrays.stream(ids).collect(Collectors.toList());
		List<VisualDashboard> dashboardList = this.list();
		Set<Integer> allDashboardIds = new HashSet<>();
		for (Integer id : idList) {
			allDashboardIds.add(id);
			allDashboardIds.addAll(this.getChildrenIds(dashboardList, id));
		}

		// 2. 删除 dashboard ,如果包含子节点，也一并删除
		this.removeByIds(allDashboardIds);

		// 删除对应的收藏记录
		starredService.remove(new LambdaQueryWrapper<SysUserStarredEntity>().eq(SysUserStarredEntity::getType, "dashboard")
				.in(SysUserStarredEntity::getTid, allDashboardIds));

		// 3. 删除关联的 chart
		visualChartService.remove(new LambdaUpdateWrapper<VisualChart>().in(VisualChart::getDashboardId, allDashboardIds));

		// 4. template dashboard
		// dashboard & asset model & monitor module
		this.update(new LambdaUpdateWrapper<VisualDashboard>()
				.set(VisualDashboard::getTmplId, -1)
				.in(VisualDashboard::getTmplId, allDashboardIds));

		assetModelService.update(new LambdaUpdateWrapper<AssetModel>()
				.set(AssetModel::getDashboardId, null)
				.in(AssetModel::getDashboardId, allDashboardIds));

		monitorModuleService.update(new LambdaUpdateWrapper<MonitorModule>()
				.set(MonitorModule::getDashboardId, null)
				.in(MonitorModule::getDashboardId, allDashboardIds));

		// Playlist
		List<VisualPlaylist> playlistList = playlistService.list();
		for (VisualPlaylist playlist : playlistList) {
			String dashboardIdsJsonArrStr = Tool.StrUtil.toString(playlist.getDashboardIds());
			List<Integer> dashboardIdList = JSONObject.parseArray(dashboardIdsJsonArrStr, Integer.class);
			Collection<Integer> intersection = Tool.CollUtil.intersection(dashboardIdList, allDashboardIds);
			if (Tool.CollUtil.isNotEmpty(intersection)) {
				dashboardIdList.removeAll(allDashboardIds);
				playlist.setDashboardIds(StrUtil.toString(dashboardIdList));
				playlist.setUts(System.currentTimeMillis());
				playlistService.updateById(playlist);
			}
		}

		try {
			// 删除 Report job
			for (Integer dashboardId : allDashboardIds) {
				reportJobManager.delJobByName(dashboardId.toString());
			}
		} catch (SchedulerException e) {
			log.error("Delete dashboard report job error", e);
		}
	}

	/**
	 * 查询所有子节点 ID
	 *
	 * @param dashboardList
	 * @param parentId
	 * @return
	 */
	private List<Integer> getChildrenIds(List<VisualDashboard> dashboardList, Integer parentId) {
		List<Integer> resultList = new ArrayList<>();
		for (VisualDashboard dashboard : dashboardList) {
			Integer id = dashboard.getId();
			Integer pid = dashboard.getPid();
			if (parentId.equals(pid)) {
				resultList.add(id);
				resultList.addAll(this.getChildrenIds(dashboardList, id));
			}
		}
		return resultList;
	}

	/**
	 * 校验内置 dashboard 不可删除
	 *
	 * @param dashboards
	 * @param removeIds
	 */
	private void validateBuildInDashboards(List<VisualDashboard> dashboards, Integer... removeIds) {
		// 内置 dashboard 不能删除
		List<Integer> buildInDashboards = dashboards.stream().filter(dashboard -> dashboard.getBuildIn() == 1).map(VisualDashboard::getId)
				.collect(Collectors.toList());
		List<Integer> removeIdList = Arrays.stream(removeIds).collect(Collectors.toList());
		removeIdList.retainAll(buildInDashboards);
		if (CollectionUtils.isNotEmpty(removeIdList)) {
			List<String> dashboardNames = this.listByIds(removeIdList).stream().map(VisualDashboard::getName)
					.collect(Collectors.toList());
			throw new NZException(
					"These dashboard is built-in and cannot be deleted, dashboards info: " + dashboardNames.toString(),
					RCode.DASHBOARD_BUILDIN_CAN_NOT_REMOVE.getCode());
		}
	}

	/**
	 * 校验 dashboard 相关参数是否正确
	 *
	 * @param dashboard
	 */
	public void validateDashboard(VisualDashboard dashboard) {
		String name = dashboard.getName();
		ValidateUtils.is(name).notNull(RCode.DASHBOARD_NAME_ISNULL);

		if (name.contains("/")) {
			throw new NZException(RCode.DASHBOARD_NAME_FORMAT_ERROR);
		}

		Integer pid = dashboard.getPid();
		pid = (pid == null) ? 0 : pid;
		// 校验name是否重复
		List<VisualDashboard> list = this.list(new QueryWrapper<VisualDashboard>().lambda().eq(VisualDashboard::getPid, pid)
				.eq(VisualDashboard::getType, dashboard.getType()).eq(VisualDashboard::getName, dashboard.getName()));
		boolean flag = false;
		// 当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
		if ((list == null || list.size() == 0)
				|| (list != null && list.size() == 1 && list.get(0).getId().equals(dashboard.getId()))) {
			flag = true;
		}

		if (!flag) {
			throw new NZException(RCode.DASHBOARD_NAME_DUPLICATE);
		}

		// 校验 type 与 link、 varType
		String type = dashboard.getType();
		if (StrUtil.isEmpty(type)) {
			if(ToolUtil.isNotEmpty(dashboard.getLink())){
				throw new NZException(RCode.DASHBOARD_TYPE_ISNULL);
			}
			dashboard.setType(DashboardConstant.Type.DASHBOARD.getValue());
		} else if(StrUtil.equals(type, DashboardConstant.Type.TEMPLATE.getValue())){
			// 校验 varType
			Integer varType = dashboard.getVarType();
			ValidateUtils.is(varType).notNull(RCode.DASHBOARD_VARTYPE_ISNULL);
			if(!Arrays.asList(new Integer[]{0, 1, 2}).contains(varType)){
				throw new NZException(RCode.DASHBOARD_VARTYPE_INVALIDE);
			}
		} else {
			List<DashboardConstant.Type> dashboardTypes = Arrays.asList(DashboardConstant.Type.values());
			List<String> types = dashboardTypes.stream().map(DashboardConstant.Type::getValue).filter(val -> !val.equals(DashboardConstant.Type.DASHBOARD.getValue())).collect(Collectors.toList());
			if(types.contains(type)){
				ValidateUtils.is(dashboard.getLink()).notNull(RCode.DASHBOARD_LINK_ISNULL);
			} else if(!StrUtil.equals(type, DashboardConstant.Type.DASHBOARD.getValue())){
				throw new NZException(RCode.DASHBOARD_TYPE_INVALIDE);
			}
		}

		// pid 是否存在
		if (dashboard.getPid() != null && dashboard.getPid() != 0) {
			VisualDashboard entity = this.getById(dashboard.getPid());
			ValidateUtils.is(entity).notNull(RCode.DASHBOARD_NOT_EXIST);
		}

		// Dashboard param 校验
		RCode paramRCode = this.validateDashboardParam(dashboard);
		if (paramRCode != null) {
			throw new NZException(paramRCode);
		}
	}

	public RCode validateDashboardParam(VisualDashboard dashboard){
		if (ToolUtil.isNotEmpty(dashboard.getParam())) {
			Map param = JSONObject.parseObject(JSONObject.toJSONString(dashboard.getParam()), Map.class);
			if (ToolUtil.isNotEmpty(param.get("report"))) {
				Map report = JSONObject.parseObject(JSONObject.toJSONString(param.get("report")), Map.class);
				// enable 为 true 时
				if (ToolUtil.isNotEmpty(report.get("enable"))
						&& ToolUtil.equals("true", report.get("enable").toString())) {
					if(ToolUtil.isEmpty(report.get("range"))){
						return RCode.DASHBOARD_PARAM_REPORTRANGE_ISNULL;
					}
					if(ToolUtil.isEmpty(report.get("schedule"))){
						return RCode.DASHBOARD_PARAM_REPORTSCHEDULE_ISNULL;
					}
					if(ToolUtil.isEmpty(report.get("receivers"))){
						return RCode.DASHBOARD_PARAM_REPORTRECEIVERS_ISNULL;
					}
					Map range = JSONObject.parseObject(JSONObject.toJSONString(report.get("range")), Map.class);
					if(ToolUtil.isEmpty(range.get("type"))){
						return RCode.DASHBOARD_PARAM_RANGETYPE_ISNULL;
					}
					if(ToolUtil.isEmpty(range.get("interval"))){
						return RCode.DASHBOARD_PARAM_RANGEINTERVAL_ISNULL;
					}
					if(ToolUtil.isEmpty(range.get("unit"))){
						return RCode.DASHBOARD_PARAM_RANGEUNIT_ISNULL;
					}
					if (!Arrays.asList(new String[] { "previous", "last" }).contains(range.get("type"))) {
						return RCode.DASHBOARD_PARAM_RANGETYPE_INVALIDE;
					} else if (!Arrays.asList(new String[] { "hour", "day", "week", "month" })
							.contains(range.get("unit"))) {
						return RCode.DASHBOARD_PARAM_RANGEUNIT_INVALIDE;
					}
					Map schedule = JSONObject.parseObject(JSONObject.toJSONString(report.get("schedule")), Map.class);
					if(ToolUtil.isEmpty(schedule.get("type"))){
						return RCode.DASHBOARD_PARAM_SCHEDULETYPE_ISNULL;
					}
					if(ToolUtil.isEmpty(schedule.get("stime"))){
						return RCode.DASHBOARD_PARAM_SCHEDULESTIME_ISNULL;
					}
					if (!Arrays.asList(new Integer[] { 1, 2, 3, 4 }).contains(schedule.get("type"))) {
						return RCode.DASHBOARD_PARAM_SCHEDULETYPE_INVALIDE;
					}
					if (ToolUtil.equals(schedule.get("type").toString(), "2")) {
						if(ToolUtil.isEmpty(schedule.get("repeat"))){
							return RCode.DASHBOARD_PARAM_SCHEDULEREPEAT_ISNULL;
						}
					} else if (ToolUtil.equals(schedule.get("type").toString(), "3")) {
						if(ToolUtil.isEmpty(schedule.get("repeat"))){
							return RCode.DASHBOARD_PARAM_SCHEDULEREPEAT_ISNULL;
						}
						if(ToolUtil.isEmpty(schedule.get("nums"))){
							return RCode.DASHBOARD_PARAM_SCHEDULENUMS_ISNULL;
						}
						if (!Arrays.asList(new Integer[] { 1, 2, 3, 4, 5, 6, 7 }).containsAll(
								JSONArray.parseObject(JSONObject.toJSONString(schedule.get("nums")), List.class))) {
							return RCode.DASHBOARD_PARAM_SCHEDULENUMS_INVALIDE;
						}
					} else if (ToolUtil.equals(schedule.get("type").toString(), "4")) {
						if(ToolUtil.isEmpty(schedule.get("nums"))){
							return RCode.DASHBOARD_PARAM_SCHEDULENUMS_ISNULL;
						}
						List<Integer> monthlyList = new ArrayList<>();
						for (int i = 1; i < 32; i++) {
							monthlyList.add(i);
						}
						monthlyList.add(-1);
						if (!monthlyList.containsAll(
								JSONArray.parseObject(JSONObject.toJSONString(schedule.get("nums")), List.class))) {
							return RCode.DASHBOARD_PARAM_SCHEDULENUMS_INVALIDE;
						}
					}
				} else {
					if (ToolUtil.isEmpty(report.get("enable"))) {
						report.put("enable", false);
						param.put("report", report);
					}
				}
			}
			if (ToolUtil.isNotEmpty(param.get("variables"))) {
				List<String> nameList = (List<String>) JSONPath.read(JSONObject.toJSONString(param.get("variables")), "name");
				for (String name : nameList) {
					boolean match = ReUtil.isMatch("[a-zA-Z_][a-zA-Z0-9_]*", name);
					if (BooleanUtil.negate(match)) {
						return RCode.DASHBOARD_PARAM_VARIABLE_NAME_FORMAT_ERROR;
					}
				}
				List<String> typeList = (List<String>) JSONPath.read(JSONObject.toJSONString(param.get("variables")), "type");
				List<String> varTypeEnumList = ListUtil.of("query", "custom");
				for (String type : typeList) {
					if (BooleanUtil.negate(varTypeEnumList.contains(type))) {
						return RCode.DASHBOARD_PARAM_VARIABLE_TYPE_INVALIDE;
					}
				}
			}
			if (ToolUtil.isNotEmpty(param.get("chartShare"))) {
				if (!Arrays.asList(new String[] { "none", "crosshair", "tooltip" }).contains(param.get("chartShare"))) {
					return RCode.DASHBOARD_PARAM_REPORTCHARTSHARE_INVALIDE;
				}
			} else {
				param.put("chartShare", "none");
			}
			dashboard.setParam(JSONObject.toJSONString(param));
		}
		return null;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveVisualDashboard(VisualDashboard dashboard) {
		dashboard.setName(StrUtil.trim(dashboard.getName()));
		this.validateDashboard(dashboard);

		Long userId = ShiroUtils.getUserId();
		dashboard.setCreateBy(userId.intValue());

		dashboard.setUts(System.currentTimeMillis());
		if (ToolUtil.isNotEmpty(dashboard.getParam())) {
			dashboard.setParam(String.valueOf(dashboard.getParam()));
		}

		Integer pid = dashboard.getPid();
		pid = (pid == null) ? 0 : pid;
		// 查询该父级节点下是否存在 dashboard 节点
		List<VisualDashboard> list = this.list(new QueryWrapper<VisualDashboard>().lambda()
				.eq(VisualDashboard::getType, dashboard.getType()).eq(VisualDashboard::getPid, pid));
		if (CollectionUtils.isEmpty(list)) {
			// 父级节点下暂无其他节点，直接保存
			this.save(dashboard);
		} else {
			// 找到当前末尾的 dashboard ， 加到其后
			VisualDashboard endDashboard = list.stream().max(Comparator.comparingInt(VisualDashboard::getWeight)).get();
			Integer weight = endDashboard.getWeight();
			dashboard.setWeight(++weight);
			this.save(dashboard);
		}

		// add assetInfo or endpointInfo chart
		this.addLinkObjectInfoChart(dashboard);

		// add Report Config In Cache
		this.addReportConfigInCache(dashboard);
	}

	/**
	 * add Link Object Info Chart
	 *
	 * @param dashboard
	 */
	@Override
	public void addLinkObjectInfoChart(VisualDashboard dashboard) {
		String type = dashboard.getType();
		if (StrUtil.equals(DashboardConstant.Type.TEMPLATE.getValue(), type)) {
			Integer varType = dashboard.getVarType();
			log.info("[addLinkObjectInfoChart] [add chart] [varType: {}]", varType);

			VisualChart chartTemplate = null;
			if (ObjectUtil.equal(DashboardConstant.VariableType.ASSET.getValue(), varType)) {
				String configValue = sysConfService.getValue(Constant.SYSCONFIG_KEY_ASSET_CHART_TPL);
				chartTemplate = JSONObject.parseObject(configValue, VisualChart.class);
			}

			if (ObjectUtil.equal(DashboardConstant.VariableType.ENDPOINT.getValue(), varType)) {
				String configValue = sysConfService.getValue(Constant.SYSCONFIG_KEY_ENDPOINT_CHART_TPL);
				chartTemplate = JSONObject.parseObject(configValue, VisualChart.class);
			}

			if (ObjectUtil.isNotNull(chartTemplate)) {
				chartTemplate.setDashboardId(dashboard.getId());
				chartTemplate.setGroupId(0);
				chartTemplate.setDatasource(ChartDatasourceType.SYSTEM.getValue());
				chartTemplate.setUpdateBy(dashboard.getCreateBy());
				chartTemplate.setUpdateAt(new Date());
				visualChartService.save(chartTemplate);
			}
		}
	}

	/**
	 * add Report Config In Cache
	 *
	 * @param dashboard
	 */
	@Override
	public void addReportConfigInCache(VisualDashboard dashboard) {
		String type = dashboard.getType();
		if (StrUtil.equals(DashboardConstant.Type.TEMPLATE.getValue(), type)) {
			log.warn("[addReportConfigInCache] [template dashboard does not support report config] [dashboardId: {}] [type: {}]", dashboard.getId(), type);
			return;
		}

		String reportEnable = null;
		if (ToolUtil.isNotEmpty(dashboard.getParam())) {
			reportEnable = JSONPath.read(dashboard.getParam().toString(), "report.enable").toString();
		}

		log.info("[addReportConfigInCache] [publish config to nz-web component] [enable: {}]", reportEnable);
		// publish report config when reportEnable is true
		if (Tool.StrUtil.equalsIgnoreCase("true", StrUtil.nullToDefault(reportEnable, "false"))) {
			Map publishMap = new HashMap(4);
			publishMap.put("dashboardId", dashboard.getId());
			publishMap.putAll(JSONObject.parseObject(dashboard.getParam().toString(), Map.class));

			// notify message
			sysService.notifySysMessageByComponentName(Constant.NZ_WEB_COMPONENT_NAME, Constant.SysTopicNameEnum.DASHBOARD_REPORT.getName(), JSONObject.toJSONString(publishMap));
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateVisualDashboard(VisualDashboard dashboard) {
		dashboard.setName(StrUtil.trim(dashboard.getName()));
		this.validateDashboard(dashboard);

		VisualDashboard oldDashboard = this.getById(dashboard.getId());

		if (oldDashboard == null) {
			throw new NZException(RCode.DASHBOARD_NOT_EXIST);
		}
		dashboard.setUts(System.currentTimeMillis());
		if (ToolUtil.isNotEmpty(dashboard.getParam()))
			dashboard.setParam(String.valueOf(dashboard.getParam()));

		// 改变 pid 父级节点时，将该 Dashboard 加入到该 pid 下末尾位置
		if (!oldDashboard.getPid().equals(dashboard.getPid())) {
			VisualDashboard endDashboard = this.getOne(new LambdaQueryWrapper<VisualDashboard>()
					.eq(VisualDashboard::getPid, dashboard.getPid()).orderByDesc(VisualDashboard::getWeight).last("limit 0,1"));
			// 该 pid 下存在节点
			if (endDashboard != null) {
				Integer weight = endDashboard.getWeight();
				dashboard.setWeight(++weight);
			} else {
				dashboard.setWeight(0);
			}
		}

		// 修改
		this.updateById(dashboard);

		// update Report Config In Cache
		this.updateReportConfigInCache(oldDashboard, dashboard);
	}

	/**
	 * update Report Config In Cache
	 *
	 * @param oldDashboard
	 * @param newDashboard
	 */
	@Override
	public void updateReportConfigInCache(VisualDashboard oldDashboard, VisualDashboard newDashboard) {
		String type = newDashboard.getType();
		if (StrUtil.equals(DashboardConstant.Type.TEMPLATE.getValue(), type)) {
			log.warn("[updateReportConfigInCache] [template dashboard does not support report config] [DashboardId: {}] [type: {}]", newDashboard.getId(), type);
			return;
		}

		// report
		String oldParamStr = ToolUtil.isEmpty(oldDashboard.getParam()) ? JSONObject.toJSONString(Tool.MapUtil.empty())
				: oldDashboard.getParam().toString();
		Map oldParamMap = JSONObject.parseObject(oldParamStr, LinkedHashMap.class);
		String newParamStr = ToolUtil.isEmpty(newDashboard.getParam()) ? JSONObject.toJSONString(Tool.MapUtil.empty())
				: newDashboard.getParam().toString();
		Map newParamMap = JSONObject.parseObject(newParamStr, LinkedHashMap.class);

		boolean isUpdate = !oldParamMap.equals(newParamMap);

		log.info("[updateReportConfigInCache] [publish config to nz-web component] [isUpdate: {}]", isUpdate);
		if (isUpdate) {
			Map publishMap = new HashMap(4);
			publishMap.put("dashboardId", newDashboard.getId());
			publishMap.putAll(newParamMap);

			// notify message
			sysService.notifySysMessageByComponentName(Constant.NZ_WEB_COMPONENT_NAME, Constant.SysTopicNameEnum.DASHBOARD_REPORT.getName(), JSONObject.toJSONString(publishMap));
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void modify(List<VisualDashboard> dashboardList) {
		if (CollectionUtils.isEmpty(dashboardList))
			throw new NZException(RCode.DASHBOARD_ID_ISNULL);
		for (VisualDashboard dashboard : dashboardList) {
			if (dashboard.getId() == null)
				throw new NZException(RCode.DASHBOARD_ID_ISNULL);
			if (dashboard.getPid() == null)
				throw new NZException(RCode.DASHBOARD_PID_ISNULL);
			if (dashboard.getWeight() == null)
				throw new NZException(RCode.DASHBOARD_WEIGHT_ISNULL);
		}

		this.getBaseMapper().updateWeightBatch(dashboardList);
	}

	@Override
	public void getTemplateByLanguage(HttpServletResponse response, String format, String type) throws IOException {
		// 按类型获取表头模板信息
		String configKey = this.getChartTemplateConfigKeyByType(type);
		String sysHeaderJsonStr = sysConfService.getValue(configKey);
		String fileName = "DashboardChartTemplate";
		basicImportAndExportServices.getTemplate(sysHeaderJsonStr, format, fileName, response);
	}

	@Override
	public Map importChartData(MultipartFile multipartFile, Map<String, Object> params) throws IOException {
		List<LinkedHashMap> importDataList = ListUtil.list(true);
		try {
			String str = IoUtil.read(multipartFile.getInputStream(), CharsetUtil.charset("UTF-8"));
			importDataList = JSONObject.parseArray(str, LinkedHashMap.class);
		} catch (Exception e) {
			log.error(e, "[importChartData] [Import file parsing failed] [file: {}]", multipartFile.getOriginalFilename());
			throw new NZException(RCode.IMPORT_FILE_PARSE_ERROR);
		}

		// 表头信息 国际化之后的
		String dashboardHeaderJsonStr = sysConfService.getValue(Constant.SYSCONFIG_KEY_DASHBOARD_EXPORT_HEADER);
		List<String> dashboardHeaderInfos = new ArrayList<>(basicImportAndExportServices.getI18nHeaderMap(dashboardHeaderJsonStr).keySet());
		String chartHeaderJsonStr = sysConfService.getValue(Constant.SYSCONFIG_KEY_CHART_EXPORT_HEADER);
		List<String> chartHeaderInfos = new ArrayList<>(basicImportAndExportServices.getI18nHeaderMap(chartHeaderJsonStr).keySet());

		// 返回统计信息
		Map statMap = this.saveDashboardsFromImportDataList(importDataList, dashboardHeaderInfos, chartHeaderInfos, params);
		return statMap;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void duplicate(Integer id) {
		VisualDashboard dashboard = this.getById(id);
		ValidateUtils.is(dashboard).notNull(RCode.DASHBOARD_NOT_EXIST);

		dashboard.setName("copy from " + dashboard.getName());
		if (dashboard.getName().length() > 64) {
			throw new NZException(RCode.DASHBOARD_NAME_TOO_LONG);
		}
		Long userId = ShiroUtils.getUserId();
		dashboard.setCreateBy(userId.intValue());
		dashboard.setId(null);
		dashboard.setUts(System.currentTimeMillis());
		String reportEnable = null;
		if (ToolUtil.isNotEmpty(dashboard.getParam()) && ToolUtil.contains(dashboard.getParam(), "enable")) {
			reportEnable = JSONPath.read(dashboard.getParam().toString(), "report.enable").toString();
			dashboard.setParam(String.valueOf(dashboard.getParam()));
		}

		Integer pid = dashboard.getPid();
		// 查询该父级节点下是否存在 dashboard 节点
		List<VisualDashboard> list = this.list(new QueryWrapper<VisualDashboard>().lambda()
				.eq(VisualDashboard::getType, dashboard.getType()).eq(VisualDashboard::getPid, pid == null ? 0 : pid));
		// 找到当前末尾的 dashboard ， 加到其后
		VisualDashboard endDashboard = list.stream().max(Comparator.comparingInt(VisualDashboard::getWeight)).get();
		Integer weight = endDashboard.getWeight();
		dashboard.setWeight(++weight);
		this.save(dashboard);

		// publish report config when reportEnable is true
		if (Tool.StrUtil.equalsIgnoreCase("true", StrUtil.nullToDefault(reportEnable, "false"))) {
			Map publishMap = new HashMap(4);
			publishMap.put("dashboardId", dashboard.getId());
			publishMap.putAll(JSONObject.parseObject(dashboard.getParam().toString(), Map.class));

			// notify message
			sysService.notifySysMessageByComponentName(Constant.NZ_WEB_COMPONENT_NAME, Constant.SysTopicNameEnum.DASHBOARD_REPORT.getName(), JSONObject.toJSONString(publishMap));
		}

		// 复制dashboard 下的 charts
		List<VisualChart> chartsGroupList = visualChartService.list(new QueryWrapper<VisualChart>().lambda()
				.eq(VisualChart::getDashboardId, id).orderByAsc(VisualChart::getGroupId));
		Map<Integer, Integer> chartsGroupMap = new HashMap<>();
		Integer chartId;
		if (!CollectionUtils.isEmpty(chartsGroupList)) {
			for (VisualChart chart : chartsGroupList) {
				chartId = chart.getId();
				chart.setId(null);
				chart.setUpdateAt(new Date());
				chart.setUpdateBy(userId.intValue());
				chart.setDashboardId(dashboard.getId());
				if (ToolUtil.equals(chart.getType(), "group")) {
					visualChartService.save(chart);
					chartsGroupMap.put(chartId, chart.getId());
				} else {
					if (!ToolUtil.equals(chart.getGroupId(), 0))
						chart.setGroupId(chartsGroupMap.get(chart.getGroupId()));
					visualChartService.save(chart);
				}
			}
		}
	}

	@Override
	public void snapshot(HttpServletResponse response, String format, Integer dashboardId, Long start, Long end,
			String language, String vars) throws IOException {
		// 生成模板
		String snapshotStr = this.genSnapshotByTemplate(dashboardId, start, end, language, vars);
		// 目前下载名称由前端控制
		// String downFileName = String.format("%s-%s.%s", "dashboard", dashboard.getName(),
		// StrUtil.emptyToDefault(format, "html"));
		ResponseUtil.downloadFile(response, "dashboard.html", snapshotStr.getBytes());
	}

	@Override
	public String genSnapshotByTemplate(Integer dashboardId, Long start, Long end, String language, String vars) {
		VisualDashboard dashboard = this.getById(dashboardId);
		if (dashboard == null) {
			throw new NZException(RCode.DASHBOARD_NOT_EXIST);
		}
		// 计算步长
		String step = visualService.getTimeStep(start, end);
		// 封装返回结果
		Map<String, Object> resultMap = visualService.snapshotResultMap(start, end, language);
		resultMap.put("step", step);
		// charts 查询
		Map param = new HashMap<>();
		param.put("pageSize", "-1");
		param.put("dashboardId", dashboard.getId());
		param.put("groupId", 0);
		PageUtils chartPageUtils = visualChartService.queryPage(param);
		List<VisualChart> chartsList = (List<VisualChart>) chartPageUtils.getList();
		if (Tool.CollUtil.isEmpty(chartsList)) {
			throw new NZException(RCode.CHART_NOTEXSITS);
		} else {
			List<Map> varList = StrUtil.isNotEmpty(vars) ? JSONObject.parseArray(vars, Map.class)
					: Tool.ListUtil.empty();

			// 将chartsData获取任务装入futureList
			List<CompletableFuture<Map>> futureList = this.recursionChartsData(dashboard.getName(), chartsList, resultMap, varList);
			Map chartsDataMap = new HashMap();
			try {
				CompletableFuture completableFuture = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));
				completableFuture.get(timeout,TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				log.error(e, "snapshot query timeout");
			}
			futureList.forEach(future -> {
				Map<String, Object> result = future.getNow(null);
				if (Tool.MapUtil.isNotEmpty(result)) {
					chartsDataMap.put(StrUtil.concat(true, StrUtil.toString(result.get("chartId")), "_",
							StrUtil.toString(result.get("eleIndex"))), result);
				}
			});
			resultMap.put("chartsData", chartsDataMap);
			resultMap.remove("step");
			resultMap.put("dashboard", R.ok(dashboard));
			resultMap.put("charts", R.ok(chartPageUtils));
		}
		return visualService.replaceSnapshotTemplate(resultMap);
	}

	@Override
	public List<VisualDashboard> getTemplateSyncDashboard(Integer dashboardId, Integer tmplId, Integer modelId, Integer moduleId) {
		if (ObjectUtil.isNotEmpty(dashboardId)) {
			VisualDashboard dashboard = this.getOne(new LambdaQueryWrapper<VisualDashboard>().eq(VisualDashboard::getId, dashboardId).ne(VisualDashboard::getTmplId, -1));
			return ObjectUtil.isNotEmpty(dashboard) ? ListUtil.of(dashboard) : ListUtil.empty();
		}

		if (ObjectUtil.isNotEmpty(tmplId)) {
			List<VisualDashboard> dashboardList = this.list(new LambdaQueryWrapper<VisualDashboard>().eq(VisualDashboard::getTmplId, tmplId));
			return CollUtil.defaultIfEmpty(dashboardList, ListUtil.empty());
		}

		if (ObjectUtil.isNotEmpty(modelId)) {
			AssetModel model = assetModelService.getOne(new LambdaQueryWrapper<AssetModel>().eq(AssetModel::getId, modelId).isNotNull(AssetModel::getDashboardId));
			if (ObjectUtil.isEmpty(model)) return ListUtil.empty();

			List<AssetAsset> assetList = assetAssetService.list(new LambdaUpdateWrapper<AssetAsset>().eq(AssetAsset::getModelId, model.getId()));
			if (CollUtil.isEmpty(assetList)) return ListUtil.empty();

			List<Integer> assetIdList = assetList.stream().map(AssetAsset::getId).collect(Collectors.toList());
			List<VisualDashboard> dashboardList = this.list(new LambdaQueryWrapper<VisualDashboard>()
					.eq(VisualDashboard::getType, DashboardConstant.Type.ASSET.getValue())
					.in(VisualDashboard::getLink, assetIdList));
			dashboardList.forEach(dashboard -> dashboard.setTmplId(model.getDashboardId()));
			return CollUtil.defaultIfEmpty(dashboardList, ListUtil.empty());
		}

		if (ObjectUtil.isNotEmpty(moduleId)) {
			MonitorModule module = monitorModuleService.getOne(new LambdaQueryWrapper<MonitorModule>().eq(MonitorModule::getId, moduleId).isNotNull(MonitorModule::getDashboardId));
			if (ObjectUtil.isEmpty(module)) return ListUtil.empty();

			List<MonitorEndpoint> endpointList = monitorEndpointService.list(new LambdaUpdateWrapper<MonitorEndpoint>().eq(MonitorEndpoint::getModuleId, module.getId()));
			if (CollUtil.isEmpty(endpointList)) return ListUtil.empty();

			List<Integer> endpointIdList = endpointList.stream().map(MonitorEndpoint::getId).collect(Collectors.toList());
			List<VisualDashboard> dashboardList = this.list(new LambdaQueryWrapper<VisualDashboard>()
					.eq(VisualDashboard::getType, DashboardConstant.Type.ENDPOINT.getValue())
					.in(VisualDashboard::getLink, endpointIdList));
			dashboardList.forEach(dashboard -> dashboard.setTmplId(module.getDashboardId()));
			return CollUtil.defaultIfEmpty(dashboardList, ListUtil.empty());
		}
		return ListUtil.empty();
	}

	@Override
	public VisualDashboard queryDashboardIncludeChartInfo(Integer dashboardId) {
		VisualDashboard dashboard = this.getById(dashboardId);
		ValidateUtils.is(dashboard).notNull(RCode.DASHBOARD_NOT_EXIST);

		// chart list
		List<VisualChart> chartList = visualChartService.list(new LambdaUpdateWrapper<VisualChart>().eq(VisualChart::getDashboardId, dashboardId));
		dashboard.setCharts(chartList);
		return dashboard;
	}

	/**
	 * 根据 vars 渲染 element 表达式
	 *
	 * @param expr
	 * @param varList
	 */
	private String replaceExprByVars(String expr, List<Map> varList) {
		if (Tool.CollectionUtil.isEmpty(varList))
			return expr;
		// 将 expr $xxx 变量渲染
		for (Map<String, String> map : varList) {
			String name = map.get("name");
			String value = map.get("value");
			expr = expr.replaceAll("\\$" + name, value);
		}
		return expr;
	}

	private List<CompletableFuture<Map>> recursionChartsData(String dashboardName, List<VisualChart> chartsList, Map<String, Object> basicQueryParam, List<Map> varList) {
		String start = StrUtil.toString(basicQueryParam.get("start"));
		String end = StrUtil.toString(basicQueryParam.get("end"));
		String step = StrUtil.toString(basicQueryParam.get("step"));
		List<CompletableFuture<Map>> futureList = new ArrayList<>();

		for (VisualChart chart : chartsList) {
			String chartName = this.replaceExprByVars(chart.getName(), varList);
			chart.setName(chartName);
			String datasource = chart.getDatasource();
			if (StrUtil.equals("misc", datasource)) {
				if (Tool.CollUtil.isNotEmpty(chart.getChildren())) {
					List<CompletableFuture<Map>> callables = this.recursionChartsData(dashboardName, chart.getChildren(), basicQueryParam, varList);
					if (Tool.CollUtil.isNotEmpty(callables))
						futureList.addAll(callables);
				}
				continue;
			}
			Map<String, Object> chartParam = (HashMap) chart.getParam();
			if (StrUtil.equals("system", datasource)) {
				futureList.add(CompletableFuture.supplyAsync(() -> {
					VisualDashboard visualDashboard = this.getById(chart.getDashboardId());
					R ok = R.ok();
					if (StrUtil.equals("assetInfo", chart.getType())) {
						ok.put("data", assetAssetService.queryAssetWithFeatureInfo(visualDashboard.getLink(), 1));
					} else if (StrUtil.equals("endpointInfo", chart.getType())) {
						ok.put("data", monitorEndpointService.queryEndpointEntity(visualDashboard.getLink()));
					} else {
						cn.hutool.json.JSONObject dataSource = JSONUtil.parseArray(chartParam.get("datasource"))
								.getJSONObject(0);
						dataSource.set("select", JSONUtil.createArray().put(dataSource.get("select")));
						ok.putAllData(statService.queryAnalyse(
								JSONUtil.toBean(JSONUtil.createObj().set("q", dataSource.toString()), Map.class)));
					}
					ok.put("eleIndex", "0");
					ok.put("chartId", chart.getId());
					return ok;
				}, this.getSnapshotThreadPool()));
			} else {
				if (Tool.CollUtil.isNotEmpty(chart.getElements())) {
					Integer eleIndex = 0;
					String limit = chartParam.get("limit") == null ? "100" : chartParam.get("limit").toString();
					for (Map element : chart.getElements()) {
						String indexStr = StrUtil.toString(eleIndex++);
						// 将表达式进行变量渲染
						String expr = this.replaceExprByVars(Tool.MapUtil.getStr(element, "expression", ""), varList);
						futureList.add(CompletableFuture.supplyAsync(() -> {
							JSONObject jsonObject = null;
							try {
								Map<String, Object> globalVariablesValue = visualService.getGlobalVariablesValue(dashboardName, chartName, Long.valueOf(start), Long.valueOf(end));
								if (StrUtil.equals("metrics", datasource)) {
									String queryExpr = visualService.globalVariablesReplace(globalVariablesValue, expr);
									String queryType = Tool.MapUtil.getStr(element, "queryType", "1");
									if (StrUtil.equals("1", queryType)) {
										jsonObject = promApiService.query_range(queryExpr, start, end, step, ToolUtil.toStr(chartParam.get("nullType"), null), null);
									} else {
										jsonObject = promApiService.query_instant(queryExpr, end, null);
									}
								} else if (StrUtil.equals("logs", datasource)) {
									jsonObject = lokiApiService.query_range(visualService.globalVariablesReplace(globalVariablesValue, expr), start, end, limit, step, null,
											"backward", "1", null);
								}
								jsonObject.put("eleIndex", indexStr);
								jsonObject.put("chartId", chart.getId());
							} catch (NZException e) {
								log.error(e);
							}
							return jsonObject;
						}, this.getSnapshotThreadPool()));
					}
				} else {
					futureList.add(CompletableFuture.supplyAsync(() -> JSONUtil
							.toBean(JSONUtil.createObj().set("eleIndex", 0).set("chartId", chart.getId()), Map.class),
							this.getSnapshotThreadPool()));
				}
			}
		}
		return futureList;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void cancelImport(String seq) {
		// remove dashboard
		this.remove(new LambdaUpdateWrapper<VisualDashboard>().eq(VisualDashboard::getSeq, seq));
		// remove chart
		visualChartService.remove(new LambdaUpdateWrapper<VisualChart>().eq(VisualChart::getSeq, seq));
	}

	@Override
	public void export(HttpServletResponse response, Map<String, Object> params) throws IOException {
		String maxLine = sysConfService.getValue(Constant.SYSCONFIG_KEY_EXPORT_MAX_LINE);
		long exportMaxLine = Integer.parseInt(StrUtil.emptyToDefault(maxLine, "10000"));
		// 导出最大条数限制
		Object limit = params.get(Constant.LIMIT) == null ? Constant.PAGESIZE : params.get(Constant.LIMIT);
		Long pageSize = Long.parseLong(limit.toString());
		if (pageSize == -1) {
			// 导出所有直接限制 pageSize
			params.put(Constant.PAGE, "1");
			params.put(Constant.LIMIT, Long.toString(exportMaxLine));
		} else if (pageSize > exportMaxLine) {
			// 分页超过最大限制 则设置最大限制为 系统配置值，和 pageSize == -1 判断区别在于 pageNo
			params.put(Constant.LIMIT, Long.toString(exportMaxLine));
		}
		// chart type 导出转换为 国际化之后的名称
		List<SysDictEntity> chartTypeList = sysDictService
				.list(new LambdaQueryWrapper<SysDictEntity>().eq(SysDictEntity::getType, Constant.DICT_CHART_TYPE));
		Map<String, String> chartTypeAndI18nMap = chartTypeList.stream()
				.collect(Collectors.toMap(SysDictEntity::getValue, SysDictEntity::getI18nCode));
		// 导出查询与列表查询一致
		List<VisualDashboard> dashboards = (List<VisualDashboard>) this.queryList(params).getList();
		List<Map<String, Object>> exportDataList = this.handleDashboardExportDataList(dashboards, chartTypeAndI18nMap);

		String fileName = "Dashboard.json";
		File file = FileUtil.writeUtf8String(JSONObject.toJSONString(exportDataList, SerializerFeature.PrettyFormat), Constant.TMP_ROOT_PATH + fileName);
		try {
			Tool.ResponseUtil.downloadFile(response, fileName, FileUtil.readBytes(file));
		} finally {
			if (file != null && file.exists()) file.delete();
		}
	}

	private String getChartTemplateConfigKeyByType(String type) {
		// 默认 dashboard 类型
		type = StringUtils.isEmpty(type) ? DashboardConstant.Type.DASHBOARD.getValue() : type;
		String chartTemplateTyep;
		switch (type) {
		case "dashboard": {
			chartTemplateTyep = Constant.SYSCONFIG_KEY_CHART_EXPORT_HEADER;
			break;
		}
		/*
		 * case "model": { chartTemplateTyep =
		 * Constant.SYSCONFIG_KEY_MODEL_CHART_EXPORT_HEADER; break; }
		 */
		case "asset":
		case "template":
		case "endpoint": {
			chartTemplateTyep = Constant.SYSCONFIG_KEY_ASSET_CHART_EXPORT_HEADER;
			break;
		}
		default:
			throw new NZException(RCode.DASHBOARD_CHART_TEMPLATE_TYPE_INCORRECT);
		}
		return chartTemplateTyep;
	}

	private String getUnitDescrByVal(Integer unitVal) {
		String unitDescr = null;
		for (Map.Entry<String, Integer> entry : Constant.ALERT_RULE_UNIT_MAP.entrySet()) {
			if (entry.getValue().equals(unitVal)) {
				unitDescr = entry.getKey();
				break;
			}
		}
		return unitDescr;
	}

	private Map saveDashboardsFromImportDataList(List<LinkedHashMap> importDataList, List<String> headerInfos, List<String> chartHeaderInfos, Map<String, Object> params) {
		String seq = StrUtil.uuid();
		params.put("seq", seq);
		// 已存在的内容处理方式，可选值：0：错误 error  1：覆盖 override   2：忽略 ignore
		String existed = Tool.ObjectUtil.defaultIfEmpty((String) params.get("existed"), "1");

		// 创建导入校验对象
		ImportValidateUtil validateUtil = ImportValidateUtil.builder()
				.existed(existed).headerInfos(headerInfos)
				.build();

		Integer addNum = 0, updateNum = 0;
		String type = ToolUtil.toStr(params.get("type"));
		// 记录导入失败内容
		List<Map<String, Object>> failContentList = ListUtil.list(true);

		Integer dashboardMaxWeight;
		List<VisualDashboard> dashboardTypeDashboardList = this.list(new LambdaQueryWrapper<VisualDashboard>()
				.eq(VisualDashboard::getType, type));
		if (CollectionUtils.isEmpty(dashboardTypeDashboardList)) {
			dashboardMaxWeight = 0;
		} else {
			VisualDashboard endDashboard = dashboardTypeDashboardList.stream().max(Comparator.comparingInt(VisualDashboard::getWeight))
					.get();
			dashboardMaxWeight = endDashboard.getWeight();
		}

		// 查询 chart datasource类型防止频繁查库
		List<SysDictEntity> chartDatasources = sysDictService.list(
				new QueryWrapper<SysDictEntity>().lambda().eq(SysDictEntity::getType, Constant.DICT_CHART_DATASOURCE));
		List<String> datasources = chartDatasources.stream().map(SysDictEntity::getValue).collect(Collectors.toList());

		// chart 导入 Type 是国际化后的，这里统一获取 chart type 可读名称 & 真实 type 值
		Map<String, String> typeReadableNameMap = this.getChartTypeReadableNameMap();

		for (int i = 0; i < importDataList.size(); i++) {
			Map<String, Object> dataMap = importDataList.get(i);
			Integer lineNum = i + 1;
			// name， 以 name作为索引 标识该条记录是否存在
			String dashboardName = ToolUtil.toStr(dataMap.get(DashboardImportEnum.NAME.getName()));
			validateUtil.validateRequired(dashboardName, lineNum, DashboardImportEnum.NAME.getIndex());
			boolean indexDuplicate = false;
			// 同类型下 name 不可重复
			VisualDashboard oldDashboard = this.getOne(new LambdaQueryWrapper<VisualDashboard>().eq(VisualDashboard::getType, type).eq(VisualDashboard::getName, dashboardName));
			if(oldDashboard != null) {
				indexDuplicate = true;
				if(!StrUtil.equals("1", existed)) {
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_DUPLICATE.setParam(DashboardImportEnum.NAME.getIndex()));
				}
			}
			// 索引列重复 & 忽略标识，提示忽略本行
			if (BooleanUtil.and(indexDuplicate, StrUtil.equals("2", existed))) {
				validateUtil.ignoreThisRow(lineNum, DashboardImportEnum.NAME.getIndex());
				failContentList.add(dataMap);
				continue;
			}
			VisualDashboard dashboard = new VisualDashboard();
			String dashboardType = ToolUtil.toStr(dataMap.get(DashboardImportEnum.TYPE.getName()));
			validateUtil.validateRequired(dashboardType, lineNum, DashboardImportEnum.TYPE.getIndex());
			validateUtil.validateEnumValArr(dashboardType, ListUtil.of(type), lineNum, DashboardImportEnum.TYPE.getIndex());

			String varType = ToolUtil.toStr(dataMap.get(DashboardImportEnum.VARTYPE.getName()));
			validateUtil.validateRequired(varType, lineNum, DashboardImportEnum.VARTYPE.getIndex());
			if(StrUtil.equalsIgnoreCase(ToolUtil.toStr(type), DashboardConstant.Type.DASHBOARD.getValue())){
				validateUtil.validateEnumValArr(varType, ListUtil.of("0"), lineNum, DashboardImportEnum.VARTYPE.getIndex());
			} else {
				List<DashboardConstant.VariableType> variableTypes = Arrays.asList(DashboardConstant.VariableType.values());
				List<String> variableTypeValues = variableTypes.stream().map(variableType -> StrUtil.toString(variableType.getValue())).collect(Collectors.toList());
				validateUtil.validateEnumValArr(varType, variableTypeValues, lineNum, DashboardImportEnum.VARTYPE.getIndex());
			}

			Object dashboardParam = dataMap.get(DashboardImportEnum.PARAM.getName());
			if(ToolUtil.isNotEmpty(dashboardParam)){
				dashboard.setParam(dashboardParam);
				RCode rCode = this.validateDashboardParam(dashboard);
				if(rCode != null){
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_SUPPORT.setParam(DashboardImportEnum.PARAM.getIndex()));
					log.error("Dashboard " + dashboardName + " import param error: " + rCode.getMsg());
				}
			}

			String remark = ToolUtil.toStr(dataMap.get(DashboardImportEnum.REMARK.getName()));

			String charts = ToolUtil.toStr(dataMap.get(DashboardImportEnum.CHARTS.getName()));
			List<VisualChart> chartList = null;
			if(StrUtil.isNotEmpty(charts)){
				boolean isJsonArray = validateUtil.validateJsonArray(charts, lineNum, DashboardImportEnum.CHARTS.getIndex());
				if(isJsonArray){
					List<String> groupNames;
					if(oldDashboard == null){
						groupNames = ListUtil.list(false);
					} else {
						List<VisualChart> visualCharts = visualChartService.list(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getDashboardId, oldDashboard.getId()).eq(VisualChart::getType, Constant.CHART_GROUP_TYPE));
						groupNames = visualCharts.stream().map(VisualChart::getName).collect(Collectors.toList());
					}
					chartList = this.validateDashboardCharts(charts, dashboardName, dashboardType, groupNames, typeReadableNameMap, lineNum, chartHeaderInfos, datasources, validateUtil, existed);
				} else {
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(DashboardImportEnum.CHARTS.getIndex()));
				}
			}

			String children = ToolUtil.toStr(dataMap.get(DashboardImportEnum.CHILDREN.getName()));
			List<VisualDashboard> childrenList = null;
			if(StrUtil.isNotEmpty(children)){
				boolean isJsonArray = validateUtil.validateJsonArray(children, lineNum, DashboardImportEnum.CHILDREN.getIndex());
				if(isJsonArray){
					childrenList = this.validateDashboardChildren(children, dashboardName, type, dashboardMaxWeight, typeReadableNameMap, lineNum, headerInfos, chartHeaderInfos, datasources, validateUtil, existed);
				} else {
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(DashboardImportEnum.CHILDREN.getIndex()));
				}
			}

			// 遇到错误是否继续导入，可选值： 0：不继续    1：继续    默认值：1
			String ignoreError = Tool.ObjectUtil.defaultIfEmpty((String) params.get("ignoreError"), "1");

			List<Map<String, String>> errMsgForThisLine = validateUtil.getImportErrMsgForThisLine(lineNum);
			if (BooleanUtil.and(Tool.StrUtil.equals("0", ignoreError), Tool.CollUtil.isNotEmpty(errMsgForThisLine))) {
				// 结束本次导入，自导入行开始 到结尾数据行，提示错误信息
				for (int j = i; j < importDataList.size(); j++) {
					validateUtil.recordImportAttrErr(j + 1, RCode.IMPORT_ERROR_STOP);
					failContentList.add(importDataList.get(j));
				}
				break;
			}

			if (Tool.CollUtil.isEmpty(errMsgForThisLine)) {
				dashboard.setName(dashboardName);
				dashboard.setType(dashboardType);
				dashboard.setVarType(Integer.valueOf(varType));
				dashboard.setRemark(remark);
				dashboard.setWeight(++dashboardMaxWeight);
				dashboard.setUts(System.currentTimeMillis());
				dashboard.setCreateBy(ShiroUtils.getUserId().intValue());
				dashboard.setSeq(seq);
				dashboard.setPid(0);
				dashboard.setChildren(childrenList);
				dashboard.setCharts(chartList);
				TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(new DefaultTransactionDefinition());
				try {
					this.saveOrUpdateRowData(dashboard, params);
					dataSourceTransactionManager.commit(transactionStatus);
					// save or update
					Object saveFlag = params.get("save");
					if (ObjectUtil.isNotNull(saveFlag)) {
						addNum++;
					} else {
						updateNum++;
					}
				} catch (Exception e) {
					failContentList.add(dataMap);
					dataSourceTransactionManager.rollback(transactionStatus);
					validateUtil.recordImportAttrErr(lineNum, RCode.ERROR);
					log.error("Dashboard import roll back the transaction failure", e);
				}
			} else {
				failContentList.add(dataMap);
			}
		}

		String failedContentFileBase64Str = null;
		File tmplFile = null;
		try {
			tmplFile = TempFile.createTempFile("json-template", ".json");
			FileUtil.writeUtf8String(JSONObject.toJSONString(failContentList, SerializerFeature.PrettyFormat), tmplFile);
			failedContentFileBase64Str = Base64.encode(tmplFile);
		} catch (IOException e) {
			log.error(e, "Could not create temporary file: " + tmplFile);
		} finally {
			if(tmplFile != null && !tmplFile.delete()) {
				log.error(new IOException("Could not delete temporary file after processing: " + tmplFile));
			}
		}

		//  返回统计信息
		Map<Object, Object> statMap = Tool.MapUtil.builder()
				.put("seq", seq)
				.put("totalNum", importDataList.size())
				.put("successNum", addNum + updateNum)
				.put("failNum", failContentList.size())
				.put("addNum", addNum)
				.put("updateNum", updateNum)
				.put("failDetail", validateUtil.getImportErrMsg())
				.put("failContent", failedContentFileBase64Str)
				.build();
		return statMap;
	}

	public List<VisualDashboard> validateDashboardChildren(String children, String parentName, String type, Integer dashboardMaxWeight, Map<String, String> typeReadableNameMap, int lineNum,
							List<String> headerInfos, List<String> chartHeaderInfos, List<String> datasources, ImportValidateUtil validateUtil, String existed){
		cn.hutool.json.JSONArray dashboardArray = JSONUtil.parseArray(children);
		if(CollectionUtils.isEmpty(dashboardArray)){
			return null;
		}
		List<VisualDashboard> dashboardList = new ArrayList<>();
		for (int i = 0; i < dashboardArray.size(); i++) {
			Map dataMap = dashboardArray.getBean(i, Map.class);
			// name， 以 name作为索引 标识该条记录是否存在
			String dashboardName = ToolUtil.toStr(dataMap.get(DashboardImportEnum.NAME.getName()));
			String validaName = parentName + "/" + dashboardName + " ";
			if(StrUtil.isEmpty(dashboardName)){
				validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_REQUIRED.setParam(validaName + headerInfos.get(DashboardImportEnum.NAME.getIndex())));
			}
			// 同类型下 name 不可重复
			VisualDashboard oldDashboard = this.getOne(new LambdaQueryWrapper<VisualDashboard>().eq(VisualDashboard::getType, type).eq(VisualDashboard::getName, dashboardName));
			if (oldDashboard != null && !StrUtil.equals("1", existed)) {
				validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_DUPLICATE.setParam(validaName + headerInfos.get(DashboardImportEnum.NAME.getIndex())));
			}
			VisualDashboard dashboard = new VisualDashboard();
			String dashboardType = ToolUtil.toStr(dataMap.get(DashboardImportEnum.TYPE.getName()));
			if(StrUtil.isEmpty(dashboardType)){
				validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_REQUIRED.setParam(validaName + headerInfos.get(DashboardImportEnum.TYPE.getIndex())));
			}
			if(!StrUtil.equals(dashboardType, type)){
				validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_WITHIN_REASON.setParam(validaName + headerInfos.get(DashboardImportEnum.TYPE.getIndex())));
			}

			String varType = ToolUtil.toStr(dataMap.get(DashboardImportEnum.VARTYPE.getName()));
			if(StrUtil.isEmpty(varType)){
				validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_REQUIRED.setParam(validaName + headerInfos.get(DashboardImportEnum.VARTYPE.getIndex())));
			}
			if (StrUtil.equalsIgnoreCase(ToolUtil.toStr(type), DashboardConstant.Type.DASHBOARD.getValue())) {
				if(!StrUtil.equals(varType, "0")){
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_WITHIN_REASON.setParam(validaName + headerInfos.get(DashboardImportEnum.VARTYPE.getIndex())));
				}
			} else if(!ListUtil.of("1", "2").contains(varType)){
				validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_WITHIN_REASON.setParam(validaName + headerInfos.get(DashboardImportEnum.VARTYPE.getIndex())));
			}

			Object dashboardParam = dataMap.get(DashboardImportEnum.PARAM.getName());
			if (ToolUtil.isNotEmpty(dashboardParam)) {
				dashboard.setParam(dashboardParam);
				RCode rCode = this.validateDashboardParam(dashboard);
				if (rCode != null) {
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_SUPPORT.setParam(validaName + headerInfos.get(DashboardImportEnum.PARAM.getIndex())));
					log.error("Dashboard child " + validaName + " param import error: " + rCode.getMsg());
				}
			}

			String remark = ToolUtil.toStr(dataMap.get(DashboardImportEnum.REMARK.getName()));

			String charts = ToolUtil.toStr(dataMap.get(DashboardImportEnum.CHARTS.getName()));
			List<VisualChart> chartList = null;
			if (StrUtil.isNotEmpty(charts)) {
				boolean isJsonArray = validateUtil.validateJsonArray(charts, lineNum, DashboardImportEnum.CHARTS.getIndex());
				if (isJsonArray) {
					List<String> groupNames;
					if (oldDashboard == null) {
						groupNames = ListUtil.list(false);
					} else {
						List<VisualChart> visualCharts = visualChartService.list(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getDashboardId, oldDashboard.getId()).eq(VisualChart::getType, Constant.CHART_GROUP_TYPE));
						groupNames = visualCharts.stream().map(VisualChart::getName).collect(Collectors.toList());
					}
					chartList = this.validateDashboardCharts(charts, parentName + "/" + dashboardName, dashboardType, groupNames, typeReadableNameMap, lineNum, chartHeaderInfos, datasources, validateUtil, existed);
				} else {
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(validaName + headerInfos.get(DashboardImportEnum.CHARTS.getIndex())));
				}
			}
			dashboard.setName(dashboardName);
			dashboard.setType(dashboardType);
			dashboard.setVarType(Integer.valueOf(varType));
			dashboard.setRemark(remark);
			dashboard.setWeight(++dashboardMaxWeight);
			dashboard.setUts(System.currentTimeMillis());
			dashboard.setCreateBy(ShiroUtils.getUserId().intValue());
			dashboard.setCharts(chartList);
			dashboardList.add(dashboard);
		}
		return dashboardList;
	}

	public List<VisualChart> validateDashboardCharts(String charts, String parentName, String dashboardType, List<String> groupNames, Map<String, String> typeReadableNameMap, int lineNum,
												 List<String> chartHeaderInfos, List<String> datasources, ImportValidateUtil validateUtil, String existed){
		cn.hutool.json.JSONArray chartsArray = JSONUtil.parseArray(charts);
		List<VisualChart> chartList = new ArrayList<>();
		for (int i = 0; i < chartsArray.size(); i++) {
			Map chartMap = chartsArray.getBean(i, Map.class);
			VisualChart visualChart = new VisualChart();
			boolean typeFlag = true;

			String name = ToolUtil.toStr(chartMap.get(ChartsImportEnum.NAME.getName()));
			String validaName = parentName + "/" + name + " ";
			if(StrUtil.isEmpty(name)){
				validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_REQUIRED.setParam(parentName + "/ " + chartHeaderInfos.get(ChartsImportEnum.NAME.getIndex())));
			} else if (name.length() > 64) {
				validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_WITHIN_REASON.setParam(validaName + chartHeaderInfos.get(ChartsImportEnum.NAME.getIndex())));
			}
			String span = ToolUtil.toStr(chartMap.get(ChartsImportEnum.SPAN.getName()));
			if(StrUtil.isEmpty(span)){
				validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_REQUIRED.setParam(validaName + chartHeaderInfos.get(ChartsImportEnum.SPAN.getIndex())));
			} else {
				Double width = null;
				try {
					width = Double.valueOf(span);
					visualChart.setSpan(width);
				} catch (NumberFormatException e) {
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(validaName + chartHeaderInfos.get(ChartsImportEnum.SPAN.getIndex())));
				}
				if (width != null && (width < 1 || width > 12)) {
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_WITHIN_REASON.setParam(validaName + chartHeaderInfos.get(ChartsImportEnum.SPAN.getIndex())));
				}
			}
			String height = ToolUtil.toStr(chartMap.get(ChartsImportEnum.HEIGHT.getName()));
			if(StrUtil.isEmpty(height)){
				validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_REQUIRED.setParam(validaName + chartHeaderInfos.get(ChartsImportEnum.HEIGHT.getIndex())));
			} else {
				Double heightIntVal = null;
				try {
					heightIntVal = Double.valueOf(height);
					visualChart.setHeight(heightIntVal);
				} catch (NumberFormatException e) {
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(validaName + chartHeaderInfos.get(ChartsImportEnum.HEIGHT.getIndex())));
				}
				if (heightIntVal != null && (heightIntVal < 1 || heightIntVal > 12)) {
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_WITHIN_REASON.setParam(validaName + chartHeaderInfos.get(ChartsImportEnum.HEIGHT.getIndex())));
				}
			}
			String type = ToolUtil.toStr(chartMap.get(ChartsImportEnum.TYPE.getName()));
			if(StrUtil.isEmpty(type)){
				typeFlag = false;
				validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_REQUIRED.setParam(validaName + chartHeaderInfos.get(ChartsImportEnum.TYPE.getIndex())));
			} else {
				type = typeReadableNameMap.get(type);
				if(StrUtil.isEmpty(type)){
					typeFlag = false;
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_WITHIN_REASON.setParam(validaName + chartHeaderInfos.get(ChartsImportEnum.TYPE.getIndex())));
				} else if(StrUtil.equals(type, Constant.CHART_GROUP_TYPE)){
					if (groupNames.contains(name)) {
						// chart group 不允许重复
						if(!StrUtil.equals("1", existed)) {
							validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_DUPLICATE.setParam(validaName + chartHeaderInfos.get(ChartsImportEnum.NAME.getIndex())));
						}
					} else {
						groupNames.add(name);
					}

					String children = ToolUtil.toStr(chartMap.get(ChartsImportEnum.CHILDREN.getName()));
					if(StrUtil.isNotEmpty(children)){
						boolean isJsonArray = validateUtil.validateJsonArray(children, lineNum, DashboardImportEnum.CHARTS.getIndex());
						if(isJsonArray){
							List<VisualChart> childrenList = this.validateDashboardCharts(children, parentName + "/" + name, dashboardType, groupNames, typeReadableNameMap, lineNum, chartHeaderInfos, datasources, validateUtil, existed);
							visualChart.setChildren(childrenList);
						} else {
							validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(validaName + chartHeaderInfos.get(ChartsImportEnum.CHILDREN.getIndex())));
						}
					}
				}
			}
			String unit = ToolUtil.toStr(chartMap.get(ChartsImportEnum.UNIT.getName()));
			if (StringUtils.isNotEmpty(unit)) {
				Integer unitInt = Constant.ALERT_RULE_UNIT_MAP.get(unit);
				if (ToolUtil.isEmpty(unitInt)) {
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_WITHIN_REASON.setParam(validaName  + chartHeaderInfos.get(ChartsImportEnum.UNIT.getIndex())));
				}
				visualChart.setUnit(unitInt);
			} else {
				visualChart.setUnit(Constant.ALERT_RULE_UNIT_MAP.get("short"));
			}
			String weight = ToolUtil.toStr(chartMap.get(ChartsImportEnum.WEIGHT.getName()));
			if(StrUtil.isEmpty(weight)){
				validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_REQUIRED.setParam(validaName + chartHeaderInfos.get(ChartsImportEnum.WEIGHT.getIndex())));
			} else {
				try {
					Integer weightIntVal = Integer.valueOf(weight);
					visualChart.setWeight(weightIntVal);
				} catch (NumberFormatException e) {
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(validaName + chartHeaderInfos.get(ChartsImportEnum.WEIGHT.getIndex())));
				}
			}
			String param = ToolUtil.toStr(chartMap.get(ChartsImportEnum.PARAM.getName()));
			String remark = ToolUtil.toStr(chartMap.get(ChartsImportEnum.REMARK.getName()));
			String x = ToolUtil.toStr(chartMap.get(ChartsImportEnum.X.getName()));
			if (StringUtils.isEmpty(x)) {
				validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_REQUIRED.setParam(validaName + chartHeaderInfos.get(ChartsImportEnum.X.getIndex())));
			} else {
				Double xIntVal = null;
				try {
					xIntVal = Double.valueOf(x);
					visualChart.setX(xIntVal);
				} catch (NumberFormatException e) {
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(validaName  + chartHeaderInfos.get(ChartsImportEnum.X.getIndex())));
				}
				if (xIntVal != null && (xIntVal < 0 || xIntVal > 12)) {
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_WITHIN_REASON.setParam(validaName  + chartHeaderInfos.get(ChartsImportEnum.X.getIndex())));
				}
			}

			// x和width之和不能大于12
			if (StringUtils.isNotEmpty(x) && StringUtils.isNotEmpty(span)) {
				Double xVal = Double.valueOf(x);
				Double widthVal = Double.valueOf(span);
				if (xVal + widthVal > 12) {
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_WITHIN_REASON.setParam(validaName  + chartHeaderInfos.get(ChartsImportEnum.SPAN.getIndex()) + ","  + chartHeaderInfos.get(ChartsImportEnum.X.getIndex())));
				}
			}

			String y = ToolUtil.toStr(chartMap.get(ChartsImportEnum.Y.getName()));
			if (StringUtils.isEmpty(y)) {
				validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(validaName  + chartHeaderInfos.get(ChartsImportEnum.Y.getIndex())));
			} else {
				try {
					Double yIntVal = Double.valueOf(y);
					visualChart.setY(yIntVal);
				} catch (NumberFormatException e) {
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_WITHIN_REASON.setParam(validaName  + chartHeaderInfos.get(ChartsImportEnum.Y.getIndex())));
				}
			}

			String datasource = ToolUtil.toStr(chartMap.get(ChartsImportEnum.DATASOURCE.getName()));
			if (StringUtils.isEmpty(datasource)) {
				validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_REQUIRED.setParam(validaName + chartHeaderInfos.get(ChartsImportEnum.DATASOURCE.getIndex())));
			} else {
				if (!datasources.contains(datasource)) {
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_WITHIN_REASON.setParam(validaName  + chartHeaderInfos.get(ChartsImportEnum.DATASOURCE.getIndex())));
				}
			}

			String elements = ToolUtil.toStr(chartMap.get(ChartsImportEnum.ELEMENTS.getName()));
			if (typeFlag) {
				String msg = this.validateChartElementForImport(datasource, elements);
				if (StrUtil.isNotEmpty(msg)) {
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_SUPPORT.setParam(validaName + chartHeaderInfos.get(ChartsImportEnum.ELEMENTS.getIndex())));
					log.error("Dashboard chart " + validaName + " elements import error: " + msg);
				} else {
					List<Map> elementsList = JSONArray.parseArray(elements, Map.class);
					visualChart.setElementArr(JSONUtil.toJsonStr(elementsList));
				}
			}
			visualChart.setName(name);
			visualChart.setType(type);
			visualChart.setParam(param);
			visualChart.setRemark(remark);
			visualChart.setDatasource(datasource);
			visualChart.setUpdateBy((ShiroUtils.getUserId()).intValue());
			visualChart.setUpdateAt(new Date());
			chartList.add(visualChart);
		}
		return chartList;
	}

	private String validateChartElementForImport(String datasource, String elements) {
		// datasource类型为metrics logs的需要校验elements
		if (datasource.equals(Constant.ChartDatasourceType.METRICS.getValue())
				|| datasource.equals(Constant.ChartDatasourceType.LOGS.getValue())) {
			if (StringUtils.isEmpty(elements)) {
				return RCode.CHART_ELEMENT_ISNULL.getMsg();
			}
			List<Map> elementsList = null;
			try {
				elementsList = JSONArray.parseArray(elements, Map.class);
			} catch (JSONException e) {
				return RCode.CHART_ELEMENTS_FORMAT.getMsg();
			}

			if (CollectionUtils.isEmpty(elementsList)) {
				return RCode.CHART_ELEMENT_ISNULL.getMsg();
			}

			for (Map element : elementsList) {
				if (ToolUtil.isEmpty(Tool.MapUtil.getStr(element, "expression", ""))) {
					return RCode.CHARTELEMENT_EXPRESSION_ISNULL.getMsg();
				}
				if (ToolUtil.isEmpty(Tool.MapUtil.getStr(element, "name", ""))) {
					return RCode.CHARTELEMENT_NAME_ISNULL.getMsg();
				}
			}
		}
		return "";
	}

	private void saveOrUpdateRowData(VisualDashboard dashboard, Map<String, Object> params) {
		MapUtil.removeAny(params, "save", "update");
		String seq = params.get("seq").toString();

		// 已存在的内容处理方式，可选值：0：错误 error  1：覆盖 override   2：忽略 ignore   在此只需要处理更新即可
		String existed = Tool.ObjectUtil.defaultIfEmpty((String) params.get("existed"), "1");
		VisualDashboard oldDashboard = this.getOne(new LambdaQueryWrapper<VisualDashboard>().eq(VisualDashboard::getType, dashboard.getType()).eq(VisualDashboard::getName, dashboard.getName()));

		if (StrUtil.equals("1", existed) && oldDashboard != null) {
			params.put("update", true);
			// update
			dashboard.setId(oldDashboard.getId());
			dashboard.setWeight(oldDashboard.getWeight());
			this.updateById(dashboard);

			// update Report Config In Cache
			this.updateReportConfigInCache(oldDashboard, dashboard);
		} else {
			params.put("save", true);
			// insert
			this.save(dashboard);

			// add Report Config In Cache
			this.addReportConfigInCache(dashboard);

			// add assetInfo or endpointInfo chart
			this.addLinkObjectInfoChart(dashboard);
		}

		this.importSaveCharts(dashboard.getCharts(), 0, dashboard.getId(), seq);
		this.importSaveChildPane(dashboard.getChildren(), dashboard.getId(), seq, existed);
	}

	private void importSaveChildPane(List<VisualDashboard> childDashboardList, Integer dashboardId, String seq, String existed){
		if(CollectionUtils.isEmpty(childDashboardList) || dashboardId == null) {
			return;
		}
		for (VisualDashboard dashboard : childDashboardList) {
			dashboard.setSeq(seq);
			dashboard.setPid(dashboardId);
			VisualDashboard oldDashboard = this.getOne(new LambdaQueryWrapper<VisualDashboard>().eq(VisualDashboard::getType, dashboard.getType()).eq(VisualDashboard::getName, dashboard.getName()));
			if (StrUtil.equals("1", existed) && oldDashboard != null) {
				dashboard.setId(oldDashboard.getId());
				dashboard.setWeight(oldDashboard.getWeight());
				this.updateById(dashboard);
			} else {
				this.save(dashboard);
			}
			this.importSaveCharts(dashboard.getCharts(), 0, dashboard.getId(), seq);
		}
	}

	private void importSaveCharts(List<VisualChart> visualChart, Integer groupId, Integer dashboardId, String seq){
		if(CollectionUtils.isEmpty(visualChart) || dashboardId == null) {
			return;
		}
		for (VisualChart chart : visualChart) {
			boolean isGroup = false;
			chart.setSeq(seq);
			chart.setDashboardId(dashboardId);
			chart.setUpdateAt(new Date());
			chart.setUpdateBy((ShiroUtils.getUserId()).intValue());
			chart.setGroupId(groupId);
			if(StrUtil.equals(chart.getType(), Constant.CHART_GROUP_TYPE)){
				isGroup = true;
				VisualChart oldChart = visualChartService.getOne(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getDashboardId, dashboardId)
						.eq(VisualChart::getType, chart.getType()).eq(VisualChart::getName, chart.getName()));
				if(oldChart != null){
					chart.setId(oldChart.getId());
					visualChartService.updateById(chart);
				}
			}
			if(chart.getId() == null){
				visualChartService.save(chart);
			}
			if(isGroup){
				this.importSaveCharts(chart.getChildren(), chart.getId(), dashboardId, seq);
			}
		}
	}
	/**
	 * chart 导入 Type 是国际化后的，这里统一获取 chart type 可读名称 & 真实 type 值
	 *
	 * @return
	 */
	private Map<String, String> getChartTypeReadableNameMap() {
		List<SysDictEntity> chartTypeList = sysDictService.list(new LambdaQueryWrapper<SysDictEntity>().eq(SysDictEntity::getType, Constant.DICT_CHART_TYPE));
		Map<String, String> chartTypeI18nCodeAndValueMap = chartTypeList.stream()
				.filter(sysDict -> StrUtil.isNotEmpty(sysDict.getI18nCode()))
				.collect(Collectors.toMap(SysDictEntity::getI18nCode, SysDictEntity::getValue));

		Map<String, String> map = Tool.MapUtil.newHashMap();

		for (Map.Entry<String, String> entry : chartTypeI18nCodeAndValueMap.entrySet()) {
			// chart type i18n code
			String i18nCode = entry.getKey();
			// chart type real
			String type = entry.getValue();

			String chartReadableName = sysI18nService.queryValue(i18nCode);
			map.put(chartReadableName, type);
		}
		return map;
	}

	private List<Map<String, Object>> handleDashboardExportDataList(List<VisualDashboard> dashboardList, Map<String, String> chartTypeAndI18nMap) {
		if(CollectionUtils.isEmpty(dashboardList)){
			return ListUtil.empty();
		}
		List<Map<String, Object>> exportDataList = new ArrayList<>();
		Map<String, Object> tempMap;
		for (VisualDashboard dashboard : dashboardList) {
			if (dashboard == null) continue;
			tempMap = new LinkedHashMap<>();
			tempMap.put(DashboardImportEnum.NAME.getName(), dashboard.getName());
			tempMap.put(DashboardImportEnum.TYPE.getName(), dashboard.getType());
			tempMap.put(DashboardImportEnum.VARTYPE.getName(), dashboard.getVarType());
			tempMap.put(DashboardImportEnum.PARAM.getName(), dashboard.getParam());
			tempMap.put(DashboardImportEnum.REMARK.getName(), dashboard.getRemark());
			// chartParams 不可复用
			Map<String, Object> chartParams = Tool.MapUtil.newHashMap();
			chartParams.put("returnChildren", "1");
			chartParams.put("groupId", 0);
			chartParams.put("pageSize", "-1");
			chartParams.put("dashboardId", dashboard.getId());
			List<VisualChart> childChartList = (List<VisualChart>) visualChartService.queryPage(chartParams).getList();
			tempMap.put(DashboardImportEnum.CHARTS.getName(), this.handleChartsExportDataList(childChartList, chartTypeAndI18nMap));
			tempMap.put(DashboardImportEnum.CHILDREN.getName(), this.handleDashboardExportDataList(dashboard.getChildren(), chartTypeAndI18nMap));
			exportDataList.add(tempMap);
		}
		return exportDataList;
	}

	private List<Map> handleChartsExportDataList(List<VisualChart> charts, Map<String, String> chartTypeAndI18nMap) {
		if (CollectionUtils.isEmpty(charts)) {
			return null;
		}
		List<Map> chartsMap = Tool.ListUtil.list(false);
		Map tempMap;
		for (VisualChart pChart : charts) {
			// do not export type assetInfo & endpointInfo chart
			if (StrUtil.equalsAny(pChart.getType(), Constant.CHART_ASSETINFO_TYPE, Constant.CHART_ENDPOINTINFO_TYPE)) {
				continue;
			}
			tempMap = new LinkedHashMap<>();
			tempMap.put(ChartsImportEnum.NAME.getName(), pChart.getName());
			tempMap.put(ChartsImportEnum.SPAN.getName(), ObjectUtil.defaultIfNull(pChart.getSpan(), "").toString());
			tempMap.put(ChartsImportEnum.HEIGHT.getName(), ObjectUtil.defaultIfNull(pChart.getHeight(), "").toString());
			String chartType = pChart.getType();
			String typeI18nCode = chartTypeAndI18nMap.get(chartType);
			tempMap.put(ChartsImportEnum.TYPE.getName(), sysI18nService.queryValue(typeI18nCode));
			if (StrUtil.equals(Constant.CHART_TEXT_TYPE, chartType)) {
				tempMap.put(ChartsImportEnum.UNIT.getName(), "");
			} else {
				tempMap.put(ChartsImportEnum.UNIT.getName(), this.getUnitDescrByVal(pChart.getUnit()));
			}
			tempMap.put(ChartsImportEnum.WEIGHT.getName(), pChart.getWeight());
			tempMap.put(ChartsImportEnum.PARAM.getName(), pChart.getParam());
			tempMap.put(ChartsImportEnum.REMARK.getName(), pChart.getRemark());
			tempMap.put(ChartsImportEnum.X.getName(), ObjectUtil.defaultIfNull(pChart.getX(), "").toString());
			tempMap.put(ChartsImportEnum.Y.getName(), ObjectUtil.defaultIfNull(pChart.getY(), "").toString());
			tempMap.put(ChartsImportEnum.ELEMENTS.getName(), pChart.getElements());
			tempMap.put(ChartsImportEnum.DATASOURCE.getName(), pChart.getDatasource());
			tempMap.put(ChartsImportEnum.CHILDREN.getName(), this.handleChartsExportDataList(pChart.getChildren(), chartTypeAndI18nMap));
			chartsMap.add(tempMap);
		}
		return chartsMap;
	}

	private synchronized ExecutorService getSnapshotThreadPool() {
		if (snapshotThreadPool == null) {
			snapshotThreadPool = new ThreadPoolExecutor(snapshotCorePoolSize, snapshotConcurrency,
					snapshotKeepAliveTime, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(snapshotCapacity),
					new NamedThreadFactory("SNAPSHOT-", true));
		}
		return snapshotThreadPool;
	}

}
