package com.nis.modules.panel.service.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.NamedThreadFactory;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.agent.service.LokiApiService;
import com.nis.modules.agent.service.PromApiService;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.panel.dao.VisualPanelDao;
import com.nis.modules.panel.entity.*;
import com.nis.modules.panel.service.VisualChartElementService;
import com.nis.modules.panel.service.VisualChartService;
import com.nis.modules.panel.service.VisualPanelService;
import com.nis.modules.panel.service.VisualService;
import com.nis.modules.report.job.ReportJobManager;
import com.nis.modules.stat.service.impl.StatServiceImpl;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.entity.SysUserStarredEntity;
import com.nis.modules.sys.service.*;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.util.TempFile;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service("visualPanelService")
public class VisualPanelServiceImpl extends ServiceImpl<VisualPanelDao, VisualPanel> implements VisualPanelService {

	private Log log = Log.get();
	/**
	 * dashboard snapshot 查询超时时间，单位：ms
	 */
	@Value("${dashboard.snapshot.timeout:300000}")
	private Integer timeout;

	@Autowired
	private VisualChartService visualChartService;

	@Autowired
	private SysConfigService sysConfigService;

	@Autowired
	private VisualChartElementService chartElementService;

	@Autowired
	private DataSourceTransactionManager dataSourceTransactionManager;

	@Autowired
	private TransactionDefinition transactionDefinition;

	@Autowired
	private SysDictService sysDictService;

	@Autowired
	private BasicImportAndExportServices basicImportAndExportServices;

	@Autowired
	private StatServiceImpl statServiceImpl;

	@Autowired
	private PromApiService promApiService;

	@Autowired
	private LokiApiService lokiApiService;

	@Autowired
	private SysUserStarredService starredService;

	@Autowired
	private AssetAssetService assetAssetService;

	@Autowired
	private MonitorEndpointService monitorEndpointService;

	@Autowired
	private RedisTemplate redisTemplate;

	@Autowired
	private ReportJobManager reportJobManager;

	@Autowired
	private SysI18nService sysI18nService;

	@Autowired
	private VisualService visualService;

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

	@Override
	public VisualPanel queryInfo(Integer id) {
		VisualPanel visualPanel = this.baseMapper.queryInfo(id);
		if (ToolUtil.isNotEmpty(visualPanel.getParam()))
			visualPanel.setParam(JSONObject.parseObject(String.valueOf(visualPanel.getParam()), Map.class));

		// 当前用户收藏状态 1：收藏 0：未收藏
		int count = starredService.count(new LambdaQueryWrapper<SysUserStarredEntity>()
				.eq(SysUserStarredEntity::getTid, visualPanel.getId()).eq(SysUserStarredEntity::getType, "panel")
				.eq(SysUserStarredEntity::getUid, ShiroUtils.getUserId().intValue()));

		visualPanel.setStarred(count == 0 ? 0 : 1);
		return visualPanel;
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
			type = Constant.PanelType.DASHBOARD.getValue();
			params.put("type", type);
		}
		boolean isDashboard = StrUtil.equals(Constant.PanelType.DASHBOARD.getValue(), type);
		if (isDashboard && StrUtil.isBlank(ids)) params.put("pid", "0");

		IPage page = new Query(VisualPanel.class).getPage(params);
		List<VisualPanel> panelList = this.baseMapper.queryList(page, params);
		for (VisualPanel visualPanel : panelList) {
			if (ToolUtil.isNotEmpty(visualPanel.getParam()))
				visualPanel.setParam(JSONObject.parseObject(String.valueOf(visualPanel.getParam()), Map.class));

			if (isDashboard) {
				// 2. dashboard 类型 需要树形结构
				List<VisualPanel> childrenList = this.list(new LambdaQueryWrapper<VisualPanel>().eq(VisualPanel::getPid, visualPanel.getId()));
				if (CollectionUtils.isNotEmpty(childrenList)) {
					for (VisualPanel childrenPanel : childrenList) {
						if (ToolUtil.isNotEmpty(childrenPanel.getParam()))
							childrenPanel.setParam(JSONObject.parseObject(String.valueOf(childrenPanel.getParam()), Map.class));

						// 当前用户收藏状态 1：收藏 0：未收藏
						int count = starredService.count(new LambdaQueryWrapper<SysUserStarredEntity>()
								.eq(SysUserStarredEntity::getTid, childrenPanel.getId()).eq(SysUserStarredEntity::getType, "panel")
								.eq(SysUserStarredEntity::getUid, currentLoginUserID.intValue()));
						childrenPanel.setStarred(count == 0 ? 0 : 1);
					}
					visualPanel.setChildren(childrenList);
				}
			}
		}
		page.setRecords(panelList);
		return new PageUtils(page);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delPanelsAndChartInfoByPanelIds(Set<Integer> panelIds) {
		if (CollectionUtils.isNotEmpty(panelIds)) {
			// 删除 panel
			this.removeByIds(panelIds);

			// 删除 chart info
			Integer[] removePanelIds = panelIds.stream().toArray(n -> new Integer[n]);
			visualChartService.delVisualChartsByPanelId(removePanelIds);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delPanels(Integer... ids) {
		if (ToolUtil.isEmpty(ids)) {
			return;
		}

		List<VisualPanel> panels = this.list();

		// 1. 校验内置panel不能被删除
		this.validateBuildInPanels(panels, ids);

		// 获取这些节点的所有子节点
		List<Integer> idList = Arrays.stream(ids).collect(Collectors.toList());
		List<VisualPanel> panelList = this.list();
		Set<Integer> allPanelIds = new HashSet<>();
		for (Integer id : idList) {
			allPanelIds.add(id);
			allPanelIds.addAll(this.getChildrenIds(panelList, id));
		}

		// 2. 删除 panel ,如果包含子节点，也一并删除
		this.removeByIds(allPanelIds);

		// 删除对应的收藏记录
		starredService.remove(new LambdaQueryWrapper<SysUserStarredEntity>().eq(SysUserStarredEntity::getType, "panel")
				.in(SysUserStarredEntity::getTid, allPanelIds));

		// 3. 删除关联的 chart chartElement
		Integer[] removePanelIds = allPanelIds.stream().toArray(n -> new Integer[n]);
		visualChartService.delVisualChartsByPanelId(removePanelIds);

		try {
			// 删除 Report job
			for (Integer panelId : allPanelIds) {
				reportJobManager.delJobByName(panelId.toString());
			}
		} catch (SchedulerException e) {
			log.error("Delete dashboard report job error", e);
		}
	}

	/**
	 * 查询所有子节点 ID
	 * 
	 * @param panelList
	 * @param parentId
	 * @return
	 */
	private List<Integer> getChildrenIds(List<VisualPanel> panelList, Integer parentId) {
		List<Integer> resultList = new ArrayList<>();
		for (VisualPanel panel : panelList) {
			Integer id = panel.getId();
			Integer pid = panel.getPid();
			if (parentId.equals(pid)) {
				resultList.add(id);
				resultList.addAll(this.getChildrenIds(panelList, id));
			}
		}
		return resultList;
	}

	/**
	 * 校验内置 panel 不可删除
	 * 
	 * @param panels
	 * @param removeIds
	 */
	private void validateBuildInPanels(List<VisualPanel> panels, Integer... removeIds) {
		// 内置 panel 不能删除
		List<Integer> buildInPanels = panels.stream().filter(panel -> panel.getBuildIn() == 1).map(VisualPanel::getId)
				.collect(Collectors.toList());
		List<Integer> removeIdList = Arrays.stream(removeIds).collect(Collectors.toList());
		removeIdList.retainAll(buildInPanels);
		if (CollectionUtils.isNotEmpty(removeIdList)) {
			List<String> panelNames = this.listByIds(removeIdList).stream().map(VisualPanel::getName)
					.collect(Collectors.toList());
			throw new NZException(
					"These panel is built-in and cannot be deleted, panels info: " + panelNames.toString(),
					RCode.PANEL_BUILDIN_CAN_NOT_REMOVE.getCode());
		}
	}

	/**
	 * 校验 panel 相关参数是否正确
	 *
	 * @param panel
	 */
	public void validatePanel(VisualPanel panel) {
		String name = panel.getName();
		ValidateUtils.is(name).notNull(RCode.PANEL_NAME_ISNULL);

		if (name.contains("/")) {
			throw new NZException(RCode.PANEL_NAME_FORMAT_ERROR);
		}

		Integer pid = panel.getPid();
		pid = (pid == null) ? 0 : pid;
		// 校验name是否重复
		List<VisualPanel> list = this.list(new QueryWrapper<VisualPanel>().lambda().eq(VisualPanel::getPid, pid)
				.eq(VisualPanel::getType, panel.getType()).eq(VisualPanel::getName, panel.getName()));
		boolean flag = false;
		// 当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
		if ((list == null || list.size() == 0)
				|| (list != null && list.size() == 1 && list.get(0).getId().equals(panel.getId()))) {
			flag = true;
		}

		if (!flag) {
			throw new NZException(RCode.PANEL_NAME_DUPLICATE);
		}

		// 校验 type 与 link、 varType
		String type = panel.getType();
		if (StrUtil.isEmpty(type)) {
			if(ToolUtil.isNotEmpty(panel.getLink())){
				throw new NZException(RCode.PANEL_TYPE_ISNULL);
			}
			panel.setType(Constant.PanelType.DASHBOARD.getValue());
		} else if(StrUtil.equals(type, Constant.PanelType.TEMPLATE.getValue())){
			// 校验 varType
			Integer varType = panel.getVarType();
			ValidateUtils.is(varType).notNull(RCode.PANEL_VARTYPE_ISNULL);
			if(!Arrays.asList(new Integer[]{0, 1, 2}).contains(varType)){
				throw new NZException(RCode.PANEL_VARTYPE_INVALIDE);
			}
		} else {
			List<Constant.PanelType> panelTypes = Arrays.asList(Constant.PanelType.values());
			List<String> types = panelTypes.stream().map(Constant.PanelType::getValue).filter(val -> !val.equals(Constant.PanelType.DASHBOARD.getValue())).collect(Collectors.toList());
			if(types.contains(type)){
				ValidateUtils.is(panel.getLink()).notNull(RCode.PANEL_LINK_ISNULL);
			} else if(!StrUtil.equals(type, Constant.PanelType.DASHBOARD.getValue())){
				throw new NZException(RCode.PANEL_TYPE_INVALIDE);
			}
		}

		// pid 是否存在
		if (panel.getPid() != null && panel.getPid() != 0) {
			VisualPanel entity = this.getById(panel.getPid());
			if (entity == null) {
				throw new NZException(RCode.PANEL_NOT_EXIST);
			}
		}

		// panel param 校验
		RCode paramRCode = validatePanelParam(panel);
		if (paramRCode != null) {
			throw new NZException(paramRCode);
		}
	}

	public RCode validatePanelParam(VisualPanel panel){
		if (ToolUtil.isNotEmpty(panel.getParam())) {
			Map param = JSONObject.parseObject(JSONObject.toJSONString(panel.getParam()), Map.class);
			if (ToolUtil.isNotEmpty(param.get("report"))) {
				Map report = JSONObject.parseObject(JSONObject.toJSONString(param.get("report")), Map.class);
				// enable 为 true 时
				if (ToolUtil.isNotEmpty(report.get("enable"))
						&& ToolUtil.equals("true", report.get("enable").toString())) {
					if(ToolUtil.isEmpty(report.get("range"))){
						return RCode.PANEL_PARAM_REPORTRANGE_ISNULL;
					}
					if(ToolUtil.isEmpty(report.get("schedule"))){
						return RCode.PANEL_PARAM_REPORTSCHEDULE_ISNULL;
					}
					if(ToolUtil.isEmpty(report.get("receivers"))){
						return RCode.PANEL_PARAM_REPORTRECEIVERS_ISNULL;
					}
					Map range = JSONObject.parseObject(JSONObject.toJSONString(report.get("range")), Map.class);
					if(ToolUtil.isEmpty(range.get("type"))){
						return RCode.PANEL_PARAM_RANGETYPE_ISNULL;
					}
					if(ToolUtil.isEmpty(range.get("interval"))){
						return RCode.PANEL_PARAM_RANGEINTERVAL_ISNULL;
					}
					if(ToolUtil.isEmpty(range.get("unit"))){
						return RCode.PANEL_PARAM_RANGEUNIT_ISNULL;
					}
					if (!Arrays.asList(new String[] { "previous", "last" }).contains(range.get("type"))) {
						return RCode.PANEL_PARAM_RANGETYPE_INVALIDE;
					} else if (!Arrays.asList(new String[] { "hour", "day", "week", "month" })
							.contains(range.get("unit"))) {
						return RCode.PANEL_PARAM_RANGEUNIT_INVALIDE;
					}
					Map schedule = JSONObject.parseObject(JSONObject.toJSONString(report.get("schedule")), Map.class);
					if(ToolUtil.isEmpty(schedule.get("type"))){
						return RCode.PANEL_PARAM_SCHEDULETYPE_ISNULL;
					}
					if(ToolUtil.isEmpty(schedule.get("stime"))){
						return RCode.PANEL_PARAM_SCHEDULESTIME_ISNULL;
					}
					if (!Arrays.asList(new Integer[] { 1, 2, 3, 4 }).contains(schedule.get("type"))) {
						return RCode.PANEL_PARAM_SCHEDULETYPE_INVALIDE;
					}
					if (ToolUtil.equals(schedule.get("type").toString(), "2")) {
						if(ToolUtil.isEmpty(schedule.get("repeat"))){
							return RCode.PANEL_PARAM_SCHEDULEREPEAT_ISNULL;
						}
					} else if (ToolUtil.equals(schedule.get("type").toString(), "3")) {
						if(ToolUtil.isEmpty(schedule.get("repeat"))){
							return RCode.PANEL_PARAM_SCHEDULEREPEAT_ISNULL;
						}
						if(ToolUtil.isEmpty(schedule.get("nums"))){
							return RCode.PANEL_PARAM_SCHEDULENUMS_ISNULL;
						}
						if (!Arrays.asList(new Integer[] { 1, 2, 3, 4, 5, 6, 7 }).containsAll(
								JSONArray.parseObject(JSONObject.toJSONString(schedule.get("nums")), List.class))) {
							return RCode.PANEL_PARAM_SCHEDULENUMS_INVALIDE;
						}
					} else if (ToolUtil.equals(schedule.get("type").toString(), "4")) {
						if(ToolUtil.isEmpty(schedule.get("nums"))){
							return RCode.PANEL_PARAM_SCHEDULENUMS_ISNULL;
						}
						List<Integer> monthlyList = new ArrayList<>();
						for (int i = 1; i < 32; i++) {
							monthlyList.add(i);
						}
						monthlyList.add(-1);
						if (!monthlyList.containsAll(
								JSONArray.parseObject(JSONObject.toJSONString(schedule.get("nums")), List.class))) {
							return RCode.PANEL_PARAM_SCHEDULENUMS_INVALIDE;
						}
					}
				} else {
					if (ToolUtil.isEmpty(report.get("enable"))) {
						report.put("enable", false);
						param.put("report", report);
					}
				}
			}
			if (ToolUtil.isNotEmpty(param.get("chartShare"))) {
				if (!Arrays.asList(new String[] { "none", "crosshair", "tooltip" }).contains(param.get("chartShare"))) {
					return RCode.PANEL_PARAM_REPORTCHARTSHARE_INVALIDE;
				}
			} else {
				param.put("chartShare", "none");
			}
			panel.setParam(JSONObject.toJSONString(param));
		}
		return null;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveVisualPanel(VisualPanel panel) {
		panel.setName(StrUtil.trim(panel.getName()));
		this.validatePanel(panel);

		Long userId = ShiroUtils.getUserId();
		panel.setCreateBy(userId.intValue());

		panel.setUts(System.currentTimeMillis());
		String reportEnable = null;
		if (ToolUtil.isNotEmpty(panel.getParam())) {
			reportEnable = JSONPath.read(panel.getParam().toString(), "report.enable").toString();
			panel.setParam(String.valueOf(panel.getParam()));
		}

		Integer pid = panel.getPid();
		pid = (pid == null) ? 0 : pid;
		// 查询该父级节点下是否存在 Panel 节点
		List<VisualPanel> list = this.list(new QueryWrapper<VisualPanel>().lambda()
				.eq(VisualPanel::getType, panel.getType()).eq(VisualPanel::getPid, pid));
		if (CollectionUtils.isEmpty(list)) {
			// 父级节点下暂无其他节点，直接保存
			this.save(panel);
		} else {
			// 找到当前末尾的 panel ， 加到其后
			VisualPanel endPanel = list.stream().max(Comparator.comparingInt(VisualPanel::getWeight)).get();
			Integer weight = endPanel.getWeight();
			panel.setWeight(++weight);
			this.save(panel);
		}

		// publish report config when reportEnable is true
		if (Tool.StrUtil.equalsIgnoreCase("true", StrUtil.nullToDefault(reportEnable, "false"))) {
			Map publishMap = new HashMap(4);
			publishMap.put("panelId", panel.getId());
			publishMap.putAll(JSONObject.parseObject(panel.getParam().toString(), Map.class));

			redisTemplate.convertAndSend(Constant.REPORT_TOPIC_NAME, JSONObject.toJSONString(publishMap));
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateVisualPanel(VisualPanel panel) {
		panel.setName(StrUtil.trim(panel.getName()));
		this.validatePanel(panel);

		VisualPanel oldPanel = this.getById(panel.getId());

		if (oldPanel == null) {
			throw new NZException(RCode.PANEL_NOT_EXIST);
		}
		panel.setUts(System.currentTimeMillis());
		if (ToolUtil.isNotEmpty(panel.getParam()))
			panel.setParam(String.valueOf(panel.getParam()));

		// 改变 pid 父级节点时，将该 panel 加入到该 pid 下末尾位置
		if (!oldPanel.getPid().equals(panel.getPid())) {
			VisualPanel endPanel = this.getOne(new LambdaQueryWrapper<VisualPanel>()
					.eq(VisualPanel::getPid, panel.getPid()).orderByDesc(VisualPanel::getWeight).last("limit 0,1"));
			// 该 pid 下存在节点
			if (endPanel != null) {
				Integer weight = endPanel.getWeight();
				panel.setWeight(++weight);
			} else {
				panel.setWeight(0);
			}
		}

		// 修改
		this.updateById(panel);

		// report
		String oldParamStr = ToolUtil.isEmpty(oldPanel.getParam()) ? JSONObject.toJSONString(Tool.MapUtil.empty())
				: oldPanel.getParam().toString();
		Map oldParamMap = JSONObject.parseObject(oldParamStr, LinkedHashMap.class);
		String newParamStr = ToolUtil.isEmpty(panel.getParam()) ? JSONObject.toJSONString(Tool.MapUtil.empty())
				: panel.getParam().toString();
		Map newParamMap = JSONObject.parseObject(newParamStr, LinkedHashMap.class);
		if (!oldParamMap.equals(newParamMap)) {
			Map publishMap = new HashMap(4);
			publishMap.put("panelId", panel.getId());
			publishMap.putAll(newParamMap);

			redisTemplate.convertAndSend(Constant.REPORT_TOPIC_NAME, JSONObject.toJSONString(publishMap));
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void modify(List<VisualPanel> panelList) {
		if (CollectionUtils.isEmpty(panelList))
			throw new NZException(RCode.PANEL_ID_ISNULL);
		for (VisualPanel panel : panelList) {
			if (panel.getId() == null)
				throw new NZException(RCode.PANEL_ID_ISNULL);
			if (panel.getPid() == null)
				throw new NZException(RCode.PANEL_PID_ISNULL);
			if (panel.getWeight() == null)
				throw new NZException(RCode.PANEL_WEIGHT_ISNULL);
		}

		this.getBaseMapper().updateWeightBatch(panelList);
	}

	@Override
	public void getTemplateByLanguage(HttpServletResponse response, String format, String type) throws IOException {
		// 按类型获取表头模板信息
		String configKey = this.getChartTemplateConfigKeyByType(type);
		String sysHeaderJsonStr = sysConfigService.getValue(configKey);
		String fileName = "PanelChartTemplate";
		basicImportAndExportServices.getTemplate(sysHeaderJsonStr, format, fileName, response);
	}

	@Override
	public Map importChartData(MultipartFile multipartFile, Map<String, Object> params) throws IOException {
		String str = IoUtil.read(multipartFile.getInputStream(), CharsetUtil.charset("UTF-8"));
		List<LinkedHashMap> importDataList = JSONObject.parseArray(str, LinkedHashMap.class);

		// 表头信息 国际化之后的
		String panelHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_PANEL_EXPORT_HEADER);
		List<String> panelHeaderInfos = new ArrayList<>(basicImportAndExportServices.getI18nHeaderMap(panelHeaderJsonStr).keySet());
		String chartHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_CHART_EXPORT_HEADER);
		List<String> chartHeaderInfos = new ArrayList<>(basicImportAndExportServices.getI18nHeaderMap(chartHeaderJsonStr).keySet());

		// 返回统计信息
		Map statMap = this.savePanelsFromImportDataList(importDataList, panelHeaderInfos, chartHeaderInfos, params);
		return statMap;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void duplicate(Integer id) {
		VisualPanel panel = this.getById(id);
		if (panel == null) {
			throw new NZException(RCode.CHART_PANEL_ISNULL);
		}

		panel.setName("copy from " + panel.getName());
		if (panel.getName().length() > 64) {
			throw new NZException(RCode.PANEL_NAME_TOO_LONG);
		}
		Long userId = ShiroUtils.getUserId();
		panel.setCreateBy(userId.intValue());
		panel.setId(null);
		panel.setUts(System.currentTimeMillis());
		String reportEnable = null;
		if (ToolUtil.isNotEmpty(panel.getParam()) && ToolUtil.contains(panel.getParam(), "enable")) {
			reportEnable = JSONPath.read(panel.getParam().toString(), "report.enable").toString();
			panel.setParam(String.valueOf(panel.getParam()));
		}

		Integer pid = panel.getPid();
		// 查询该父级节点下是否存在 Panel 节点
		List<VisualPanel> list = this.list(new QueryWrapper<VisualPanel>().lambda()
				.eq(VisualPanel::getType, panel.getType()).eq(VisualPanel::getPid, pid == null ? 0 : pid));
		// 找到当前末尾的 panel ， 加到其后
		VisualPanel endPanel = list.stream().max(Comparator.comparingInt(VisualPanel::getWeight)).get();
		Integer weight = endPanel.getWeight();
		panel.setWeight(++weight);
		this.save(panel);

		// publish report config when reportEnable is true
		if (Tool.StrUtil.equalsIgnoreCase("true", StrUtil.nullToDefault(reportEnable, "false"))) {
			Map publishMap = new HashMap(4);
			publishMap.put("panelId", panel.getId());
			publishMap.putAll(JSONObject.parseObject(panel.getParam().toString(), Map.class));

			redisTemplate.convertAndSend(Constant.REPORT_TOPIC_NAME, JSONObject.toJSONString(publishMap));
		}

		// 复制panel 下的 charts
		List<VisualChart> chartsGroupList = visualChartService.list(new QueryWrapper<VisualChart>().lambda()
				.eq(VisualChart::getPanelId, id).orderByAsc(VisualChart::getGroupId));
		Map<Integer, Integer> chartsGroupMap = new HashMap<>();
		Integer chartId;
		if (!CollectionUtils.isEmpty(chartsGroupList)) {
			for (VisualChart chart : chartsGroupList) {
				chartId = chart.getId();
				chart.setId(null);
				chart.setUpdateAt(new Date());
				chart.setUpdateBy(userId.intValue());
				chart.setPanelId(panel.getId());
				if (ToolUtil.equals(chart.getType(), "group")) {
					visualChartService.save(chart);
					chartsGroupMap.put(chartId, chart.getId());
				} else {
					if (!ToolUtil.equals(chart.getGroupId(), 0))
						chart.setGroupId(chartsGroupMap.get(chart.getGroupId()));
					visualChartService.save(chart);
				}
				// 查询当前chart下的chartElement
				List<VisualChartElement> elementList = chartElementService.list(
						new QueryWrapper<VisualChartElement>().lambda().eq(VisualChartElement::getChartId, chartId));
				if (!CollectionUtils.isEmpty(elementList)) {
					for (VisualChartElement element : elementList) {
						element.setId(null);
						element.setChartId(chart.getId());
					}
					chartElementService.saveOrUpdateElements(chart.getId(), elementList);
				}
			}
		}
	}

	@Override
	public void snapshot(HttpServletResponse response, String format, Integer panelId, Long start, Long end,
			String language, String vars) throws IOException {
		// 生成模板
		String snapshotStr = this.genSnapshotByTemplate(panelId, start, end, language, vars);
		// 目前下载名称由前端控制
		// String downFileName = String.format("%s-%s.%s", "panel", panel.getName(),
		// StrUtil.emptyToDefault(format, "html"));
		ResponseUtil.downloadFile(response, "panel.html", snapshotStr.getBytes());
	}

	@Override
	public String genSnapshotByTemplate(Integer panelId, Long start, Long end, String language, String vars) {
		VisualPanel panel = this.getById(panelId);
		if (panel == null) {
			throw new NZException(RCode.CHART_PANEL_ISNULL);
		}
		// 计算步长
		String step = visualService.getTimeStep(start, end);
		// 封装返回结果
		Map<String, Object> resultMap = visualService.snapshotResultMap(start, end, language);
		resultMap.put("step", step);
		// charts 查询
		Map param = new HashMap<>();
		param.put("pageSize", "-1");
		param.put("panelId", panel.getId());
		param.put("groupId", 0);
		PageUtils chartPageUtils = visualChartService.queryPage(param);
		List<VisualChart> chartsList = (List<VisualChart>) chartPageUtils.getList();
		if (Tool.CollUtil.isEmpty(chartsList)) {
			throw new NZException(RCode.CHART_NOTEXSITS);
		} else {
			List<Map> varList = StrUtil.isNotEmpty(vars) ? JSONObject.parseArray(vars, Map.class)
					: Tool.ListUtil.empty();

			// 将chartsData获取任务装入futureList
			List<CompletableFuture<Map>> futureList = this.recursionChartsData(chartsList, resultMap, varList);
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
			resultMap.put("panel", R.ok(panel));
			resultMap.put("charts", R.ok(chartPageUtils));
		}
		return visualService.replaceSnapshotTemplate(resultMap);
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

	private List<CompletableFuture<Map>> recursionChartsData(List<VisualChart> chartsList,
			Map<String, Object> basicQueryParam, List<Map> varList) {
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
					List<CompletableFuture<Map>> callables = this.recursionChartsData(chart.getChildren(),
							basicQueryParam, varList);
					if (Tool.CollUtil.isNotEmpty(callables))
						futureList.addAll(callables);
				}
				continue;
			}
			Map<String, Object> chartParam = (HashMap) chart.getParam();
			if (StrUtil.equals("system", datasource)) {
				futureList.add(CompletableFuture.supplyAsync(() -> {
					VisualPanel visualPanel = this.getById(chart.getPanelId());
					R ok = R.ok();
					if (StrUtil.equals("assetInfo", chart.getType())) {
						ok.put("data", assetAssetService.queryAssetInfo(visualPanel.getLink()));
					} else if (StrUtil.equals("endpointInfo", chart.getType())) {
						ok.put("data", monitorEndpointService.queryEndpointEntity(visualPanel.getLink()));
					} else {
						cn.hutool.json.JSONObject dataSource = JSONUtil.parseArray(chartParam.get("datasource"))
								.getJSONObject(0);
						dataSource.set("select", JSONUtil.createArray().put(dataSource.get("select")));
						ok.putAllData(statServiceImpl.queryAnalyse(
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
					for (VisualChartElement element : chart.getElements()) {
						String indexStr = StrUtil.toString(eleIndex++);
						// 将表达式进行变量渲染
						String expr = this.replaceExprByVars(element.getExpression(), varList);
						futureList.add(CompletableFuture.supplyAsync(() -> {
							JSONObject jsonObject = null;
							try {
								if (StrUtil.equals("metrics", datasource)) {
									jsonObject = promApiService.query_range(expr, start, end, step,
											ToolUtil.toStr(chartParam.get("nullType"), null), null);
								} else if (StrUtil.equals("logs", datasource)) {
									jsonObject = lokiApiService.query_range(expr, start, end, limit, step, null,
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
		List<VisualPanel> panelList = this.list(new QueryWrapper<VisualPanel>().lambda().eq(VisualPanel::getType,
				Constant.PanelType.DASHBOARD.getValue()));
		List<Integer> delPanelIds = panelList.stream().filter(panel -> seq.equals(panel.getSeq()))
				.map(VisualPanel::getId).collect(Collectors.toList());
		if (CollectionUtils.isNotEmpty(delPanelIds)) {
			// 删除 panel
			this.remove(new QueryWrapper<VisualPanel>().lambda().eq(VisualPanel::getSeq, seq));
		}

		List<VisualChart> delCharts = visualChartService
				.list(new QueryWrapper<VisualChart>().lambda().eq(VisualChart::getSeq, seq));
		if (CollectionUtils.isNotEmpty(delCharts)) {
			// 删除 charts
			visualChartService.remove(new QueryWrapper<VisualChart>().lambda().eq(VisualChart::getSeq, seq));
		}
		chartElementService.remove(new QueryWrapper<VisualChartElement>().lambda().eq(VisualChartElement::getSeq, seq));
	}

	@Override
	public void export(HttpServletResponse response, Map<String, Object> params) throws IOException {
		String type = ToolUtil.toStr(params.get("type"), Constant.PanelType.DASHBOARD.getValue());
		if (!StrUtil.equalsAnyIgnoreCase(type, Constant.PanelType.DASHBOARD.getValue(), Constant.PanelType.TEMPLATE.getValue())) {
			throw new NZException(RCode.PANEL_IMPORT_TYPE_INCORRECT);
		}

		String maxLine = sysConfigService.getValue(Constant.SYSCONFIG_KEY_EXPORT_MAX_LINE);
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
		List<VisualPanel> panels = (List<VisualPanel>) this.queryList(params).getList();
		List<Map<String, Object>> exportDataList = this.handlePanelExportDataList(panels, chartTypeAndI18nMap);

		String fileName = "Panel.json";
		File file = FileUtil.writeUtf8String(JSONObject.toJSONString(exportDataList, SerializerFeature.PrettyFormat), Constant.TMP_ROOT_PATH + fileName);
		try {
			Tool.ResponseUtil.downloadFile(response, fileName, FileUtil.readBytes(file));
		} finally {
			if (file != null && file.exists()) file.delete();
		}
	}

	private String getChartTemplateConfigKeyByType(String type) {
		// 默认 dashboard 类型
		type = StringUtils.isEmpty(type) ? Constant.PanelType.DASHBOARD.getValue() : type;
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
			throw new NZException(RCode.PANEL_CHART_TEMPLATE_TYPE_INCORRECT);
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

	private Map savePanelsFromImportDataList(List<LinkedHashMap> importDataList, List<String> headerInfos, List<String> chartHeaderInfos, Map<String, Object> params) {
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

		Integer panelMaxWeight;
		List<VisualPanel> dashboardTypePanelList = this.list(new LambdaQueryWrapper<VisualPanel>()
				.eq(VisualPanel::getType, type));
		if (CollectionUtils.isEmpty(dashboardTypePanelList)) {
			panelMaxWeight = 0;
		} else {
			VisualPanel endPanel = dashboardTypePanelList.stream().max(Comparator.comparingInt(VisualPanel::getWeight))
					.get();
			panelMaxWeight = endPanel.getWeight();
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
			String panelName = ToolUtil.toStr(dataMap.get(PanelImportEnum.NAME.getName()));
			validateUtil.validateRequired(panelName, lineNum, PanelImportEnum.NAME.getIndex());
			boolean indexDuplicate = false;
			// 同类型下 name 不可重复
			VisualPanel oldPanel = this.getOne(new LambdaQueryWrapper<VisualPanel>().eq(VisualPanel::getType, type).eq(VisualPanel::getName, panelName));
			if(oldPanel != null) {
				indexDuplicate = true;
				if(!StrUtil.equals("1", existed)) {
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_DUPLICATE.setParam(PanelImportEnum.NAME.getIndex()));
				}
			}
			// 索引列重复 & 忽略标识，提示忽略本行
			if (BooleanUtil.and(indexDuplicate, StrUtil.equals("2", existed))) {
				validateUtil.ignoreThisRow(lineNum, PanelImportEnum.NAME.getIndex());
				failContentList.add(dataMap);
				continue;
			}
			VisualPanel panel = new VisualPanel();
			String panelType = ToolUtil.toStr(dataMap.get(PanelImportEnum.TYPE.getName()));
			validateUtil.validateRequired(panelType, lineNum, PanelImportEnum.TYPE.getIndex());
			validateUtil.validateEnumValArr(panelType, ListUtil.of(type), lineNum, PanelImportEnum.TYPE.getIndex());

			String varType = ToolUtil.toStr(dataMap.get(PanelImportEnum.VARTYPE.getName()));
			validateUtil.validateRequired(varType, lineNum, PanelImportEnum.VARTYPE.getIndex());
			if(StrUtil.equalsIgnoreCase(ToolUtil.toStr(type), Constant.PanelType.DASHBOARD.getValue())){
				validateUtil.validateEnumValArr(varType, ListUtil.of("0"), lineNum, PanelImportEnum.VARTYPE.getIndex());
			} else {
				validateUtil.validateEnumValArr(varType, ListUtil.of("1", "2"), lineNum, PanelImportEnum.VARTYPE.getIndex());
			}

			Object panelParam = dataMap.get(PanelImportEnum.PARAM.getName());
			if(ToolUtil.isNotEmpty(panelParam)){
				panel.setParam(panelParam);
				RCode rCode = this.validatePanelParam(panel);
				if(rCode != null){
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_SUPPORT.setParam(PanelImportEnum.PARAM.getIndex()));
					log.error("Panel " + panelName + " import param error: " + rCode.getMsg());
				}
			}

			String remark = ToolUtil.toStr(dataMap.get(PanelImportEnum.REMARK.getName()));

			String charts = ToolUtil.toStr(dataMap.get(PanelImportEnum.CHARTS.getName()));
			List<VisualChart> chartList = null;
			if(StrUtil.isNotEmpty(charts)){
				boolean isJsonArray = validateUtil.validateJsonArray(charts, lineNum, PanelImportEnum.CHARTS.getIndex());
				if(isJsonArray){
					List<String> groupNames;
					if(oldPanel == null){
						groupNames = ListUtil.list(false);
					} else {
						List<VisualChart> visualCharts = visualChartService.list(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getPanelId, oldPanel.getId()).eq(VisualChart::getType, Constant.CHART_GROUP_TYPE));
						groupNames = visualCharts.stream().map(VisualChart::getName).collect(Collectors.toList());
					}
					chartList = this.validatePanelCharts(charts, panelName, panelType, groupNames, typeReadableNameMap, lineNum, chartHeaderInfos, datasources, validateUtil, existed);
				} else {
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(PanelImportEnum.CHARTS.getIndex()));
				}
			}

			String children = ToolUtil.toStr(dataMap.get(PanelImportEnum.CHILDREN.getName()));
			List<VisualPanel> childrenList = null;
			if(StrUtil.isNotEmpty(children)){
				boolean isJsonArray = validateUtil.validateJsonArray(children, lineNum, PanelImportEnum.CHILDREN.getIndex());
				if(isJsonArray){
					childrenList = this.validatePanelChildren(children, panelName, type, panelMaxWeight, typeReadableNameMap, lineNum, headerInfos, chartHeaderInfos, datasources, validateUtil, existed);
				} else {
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(PanelImportEnum.CHILDREN.getIndex()));
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
				panel.setName(panelName);
				panel.setType(panelType);
				panel.setVarType(Integer.valueOf(varType));
				panel.setRemark(remark);
				panel.setWeight(++panelMaxWeight);
				panel.setUts(System.currentTimeMillis());
				panel.setCreateBy(ShiroUtils.getUserId().intValue());
				panel.setSeq(seq);
				panel.setPid(0);
				panel.setChildren(childrenList);
				panel.setCharts(chartList);
				TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(new DefaultTransactionDefinition());
				try {
					this.saveOrUpdateRowData(panel, params);
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
					log.error("Panel import roll back the transaction failure", e);
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

	public List<VisualPanel> validatePanelChildren(String children, String parentName, String type, Integer panelMaxWeight, Map<String, String> typeReadableNameMap, int lineNum,
							List<String> headerInfos, List<String> chartHeaderInfos, List<String> datasources, ImportValidateUtil validateUtil, String existed){
		cn.hutool.json.JSONArray panelArray = JSONUtil.parseArray(children);
		if(CollectionUtils.isEmpty(panelArray)){
			return null;
		}
		List<VisualPanel> panelList = new ArrayList<>();
		for (int i = 0; i < panelArray.size(); i++) {
			Map dataMap = panelArray.getBean(i, Map.class);
			// name， 以 name作为索引 标识该条记录是否存在
			String panelName = ToolUtil.toStr(dataMap.get(PanelImportEnum.NAME.getName()));
			String validaName = parentName + "/" + panelName + " ";
			if(StrUtil.isEmpty(panelName)){
				validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_REQUIRED.setParam(validaName + headerInfos.get(PanelImportEnum.NAME.getIndex())));
			}
			// 同类型下 name 不可重复
			VisualPanel oldPanel = this.getOne(new LambdaQueryWrapper<VisualPanel>().eq(VisualPanel::getType, type).eq(VisualPanel::getName, panelName));
			if (oldPanel != null && !StrUtil.equals("1", existed)) {
				validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_DUPLICATE.setParam(validaName + headerInfos.get(PanelImportEnum.NAME.getIndex())));
			}
			VisualPanel panel = new VisualPanel();
			String panelType = ToolUtil.toStr(dataMap.get(PanelImportEnum.TYPE.getName()));
			if(StrUtil.isEmpty(panelType)){
				validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_REQUIRED.setParam(validaName + headerInfos.get(PanelImportEnum.TYPE.getIndex())));
			}
			if(!StrUtil.equals(panelType, type)){
				validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_WITHIN_REASON.setParam(validaName + headerInfos.get(PanelImportEnum.TYPE.getIndex())));
			}

			String varType = ToolUtil.toStr(dataMap.get(PanelImportEnum.VARTYPE.getName()));
			if(StrUtil.isEmpty(varType)){
				validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_REQUIRED.setParam(validaName + headerInfos.get(PanelImportEnum.VARTYPE.getIndex())));
			}
			if (StrUtil.equalsIgnoreCase(ToolUtil.toStr(type), Constant.PanelType.DASHBOARD.getValue())) {
				if(!StrUtil.equals(varType, "0")){
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_WITHIN_REASON.setParam(validaName + headerInfos.get(PanelImportEnum.VARTYPE.getIndex())));
				}
			} else if(!ListUtil.of("1", "2").contains(varType)){
				validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_WITHIN_REASON.setParam(validaName + headerInfos.get(PanelImportEnum.VARTYPE.getIndex())));
			}

			Object panelParam = dataMap.get(PanelImportEnum.PARAM.getName());
			if (ToolUtil.isNotEmpty(panelParam)) {
				panel.setParam(panelParam);
				RCode rCode = this.validatePanelParam(panel);
				if (rCode != null) {
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_SUPPORT.setParam(validaName + headerInfos.get(PanelImportEnum.PARAM.getIndex())));
					log.error("Panel child " + validaName + " param import error: " + rCode.getMsg());
				}
			}

			String remark = ToolUtil.toStr(dataMap.get(PanelImportEnum.REMARK.getName()));

			String charts = ToolUtil.toStr(dataMap.get(PanelImportEnum.CHARTS.getName()));
			List<VisualChart> chartList = null;
			if (StrUtil.isNotEmpty(charts)) {
				boolean isJsonArray = validateUtil.validateJsonArray(charts, lineNum, PanelImportEnum.CHARTS.getIndex());
				if (isJsonArray) {
					List<String> groupNames;
					if (oldPanel == null) {
						groupNames = ListUtil.list(false);
					} else {
						List<VisualChart> visualCharts = visualChartService.list(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getPanelId, oldPanel.getId()).eq(VisualChart::getType, Constant.CHART_GROUP_TYPE));
						groupNames = visualCharts.stream().map(VisualChart::getName).collect(Collectors.toList());
					}
					chartList = this.validatePanelCharts(charts, parentName + "/" + panelName, panelType, groupNames, typeReadableNameMap, lineNum, chartHeaderInfos, datasources, validateUtil, existed);
				} else {
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(validaName + headerInfos.get(PanelImportEnum.CHARTS.getIndex())));
				}
			}
			panel.setName(panelName);
			panel.setType(panelType);
			panel.setVarType(Integer.valueOf(varType));
			panel.setRemark(remark);
			panel.setWeight(++panelMaxWeight);
			panel.setUts(System.currentTimeMillis());
			panel.setCreateBy(ShiroUtils.getUserId().intValue());
			panel.setCharts(chartList);
			panelList.add(panel);
		}
		return panelList;
	}

	public List<VisualChart> validatePanelCharts(String charts, String parentName, String panelType, List<String> groupNames, Map<String, String> typeReadableNameMap, int lineNum,
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
						boolean isJsonArray = validateUtil.validateJsonArray(children, lineNum, PanelImportEnum.CHARTS.getIndex());
						if(isJsonArray){
							List<VisualChart> childrenList = this.validatePanelCharts(children, parentName + "/" + name, panelType, groupNames, typeReadableNameMap, lineNum, chartHeaderInfos, datasources, validateUtil, existed);
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
			if (typeFlag) {
				String msg = this.validateChartParamForImport(type, param, panelType);
				if (StrUtil.isNotEmpty(msg)) {
					validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_SUPPORT.setParam(validaName + chartHeaderInfos.get(ChartsImportEnum.PARAM.getIndex())));
					log.error("Panel chart " + validaName + " param import error: " + msg);
				}
			}
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
					log.error("Panel chart " + validaName + " elements import error: " + msg);
				} else {
					List<VisualChartElement> elementsList = JSONArray.parseArray(elements, VisualChartElement.class);
					visualChart.setElements(elementsList);
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
			List<VisualChartElement> elementsList = null;
			try {
				elementsList = JSONArray.parseArray(elements, VisualChartElement.class);
			} catch (JSONException e) {
				return RCode.CHART_ELEMENTS_FORMAT.getMsg();
			}

			if (CollectionUtils.isEmpty(elementsList)) {
				return RCode.CHART_ELEMENT_ISNULL.getMsg();
			}

			for (VisualChartElement element : elementsList) {
				if (ToolUtil.isEmpty(element.getExpression())) {
					return RCode.CHARTELEMENT_EXPRESSION_ISNULL.getMsg();
				}
				if (ToolUtil.isEmpty(element.getName())) {
					return RCode.CHARTELEMENT_NAME_ISNULL.getMsg();
				}
			}
		}
		return "";
	}

	private String validateChartParamForImport(String type, String param, String panelType) {
		switch (type) {
			// url
			case Constant.CHART_URL_TYPE: {
				if (StringUtils.isEmpty(param)) {
					return RCode.CHART_URLPARAM_ISNULL.getMsg();
				}
				Map map = null;
				try {
					map = JSONObject.parseObject(param, Map.class);
				} catch (JSONException e) {
					return RCode.CHART_PARAM_FORMAT.getMsg();
				}
				Object url = map.get(Constant.CHART_URL_TYPE);
				if (ToolUtil.isEmpty(url)) {
					return RCode.CHART_URLPARAM_ISNULL.getMsg();
				}

				if (StrUtil.equals(Constant.PanelType.TEMPLATE.getValue(), panelType)) {
					// 替换全部变量 替换为 a（任意字符） 不影响原来的数据格式 然后进行校验
					String urlStr = url.toString();
					urlStr = urlStr.replaceAll("\\{\\{asset\\..*?\\}\\}", "a");

					boolean b = CommonUtils.checkUrlInModel(urlStr);
					boolean b1 = CommonUtils.checkUrl(urlStr);

					if (!b && !b1) {
						return RCode.CHART_PARAMURL_FORMAT.getMsg();
					}
				} else {
					if (!CommonUtils.checkUrl(url.toString())) {
						return RCode.CHART_PARAMURL_FORMAT.getMsg();
					}
				}
				break;
			}
			// singleStat
			case Constant.CHART_SINGLESTAT_TYPE: {
				if (StringUtils.isEmpty(param)) {
					return RCode.CHART_SINGLESTATPARAM_ISNULL.getMsg();
				}
				Map map = null;
				try {
					map = JSONObject.parseObject(param, Map.class);
				} catch (JSONException e) {
					return RCode.CHART_PARAM_FORMAT.getMsg();
				}
				Object statistics = map.get(Constant.CHART_SINGLESTAT_PARAMkEY);
				if (ToolUtil.isEmpty(statistics)) {
					return RCode.CHART_PARAMSINGLESTAT_ISNULL.getMsg();
				}
				if (!Constant.CHART_SINGLESTAT_STATISTICS.contains(statistics.toString())) {
					return RCode.CHART_PARAMSINGLESTAT_INVALIDE.getMsg();
				}
				break;
			}
			case Constant.CHART_TEXT_TYPE: {
				if (StringUtils.isEmpty(param)) {
					return RCode.CHART_TEXTPARAM_ISNULL.getMsg();
				}
				Map map = null;
				try {
					map = JSONObject.parseObject(param, Map.class);
				} catch (JSONException e) {
					return RCode.CHART_PARAM_FORMAT.getMsg();
				}
				Object text = map.get(Constant.CHART_TEXT_TYPE);
				if (ToolUtil.isEmpty(text)) {
					return RCode.CHART_TEXTPARAM_ISNULL.getMsg();
				}
				break;
			}
			default: {
				if (StringUtils.isNotEmpty(param)) {
					try {
						JSONObject.parseObject(param, Map.class);
					} catch (JSONException e) {
						return RCode.CHART_PARAM_FORMAT.getMsg();
					}
				}
			}
		}
		return "";
	}

	private void saveOrUpdateRowData(VisualPanel panel, Map<String, Object> params) {
		MapUtil.removeAny(params, "save", "update");
		String seq = params.get("seq").toString();

		// 已存在的内容处理方式，可选值：0：错误 error  1：覆盖 override   2：忽略 ignore   在此只需要处理更新即可
		String existed = Tool.ObjectUtil.defaultIfEmpty((String) params.get("existed"), "1");
		VisualPanel oldPanel = this.getOne(new LambdaQueryWrapper<VisualPanel>().eq(VisualPanel::getType, panel.getType()).eq(VisualPanel::getName, panel.getName()));

		if (StrUtil.equals("1", existed) && oldPanel != null) {
			params.put("update", true);
			// update
			panel.setId(oldPanel.getId());
			panel.setWeight(oldPanel.getWeight());
			this.updateById(panel);
		} else {
			params.put("save", true);
			// insert
			this.save(panel);
		}

		this.importSaveCharts(panel.getCharts(), 0, panel.getId(), seq);
		this.importSaveChildPane(panel.getChildren(), panel.getId(), seq, existed);
	}

	private void importSaveChildPane(List<VisualPanel> childPanelList, Integer panelId, String seq, String existed){
		if(CollectionUtils.isEmpty(childPanelList) || panelId == null) {
			return;
		}
		for (VisualPanel panel : childPanelList) {
			panel.setSeq(seq);
			panel.setPid(panelId);
			VisualPanel oldPanel = this.getOne(new LambdaQueryWrapper<VisualPanel>().eq(VisualPanel::getType, panel.getType()).eq(VisualPanel::getName, panel.getName()));
			if (StrUtil.equals("1", existed) && oldPanel != null) {
				panel.setId(oldPanel.getId());
				panel.setWeight(oldPanel.getWeight());
				this.updateById(panel);
			} else {
				this.save(panel);
			}
			this.importSaveCharts(panel.getCharts(), 0, panel.getId(), seq);
		}
	}

	private void importSaveCharts(List<VisualChart> visualChart, Integer groupId, Integer panelId, String seq){
		if(CollectionUtils.isEmpty(visualChart) || panelId == null) {
			return;
		}
		for (VisualChart chart : visualChart) {
			boolean isGroup = false;
			chart.setSeq(seq);
			chart.setPanelId(panelId);
			chart.setUpdateAt(new Date());
			chart.setUpdateBy((ShiroUtils.getUserId()).intValue());
			chart.setGroupId(groupId);
			if(StrUtil.equals(chart.getType(), Constant.CHART_GROUP_TYPE)){
				isGroup = true;
				VisualChart oldChart = visualChartService.getOne(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getPanelId, panelId)
						.eq(VisualChart::getType, chart.getType()).eq(VisualChart::getName, chart.getName()));
				if(oldChart != null){
					chart.setId(oldChart.getId());
					visualChartService.updateById(chart);
				}
			}
			if(chart.getId() == null){
				visualChartService.save(chart);
			}

			List<VisualChartElement> chartElements = chart.getElements();
			if (CollectionUtils.isNotEmpty(chartElements)) {
				chartElements.forEach(chartElement -> {
					chartElement.setChartId(chart.getId());
					chartElement.setSeq(seq);
					chartElement.setType(Constant.CHART_ELEMENT_EXPERT_TYPE);
				});
				chartElementService.saveBatch(chartElements);
			}
			if(isGroup){
				this.importSaveCharts(chart.getChildren(), chart.getId(), panelId, seq);
			}
		}
	}
	/**
	 * chart 导入 Type 是国际化后的，这里统一获取 chart type 可读名称 & 真实 type 值
	 *
	 * @return
	 */
	private Map<String, String> getChartTypeReadableNameMap() {
		List<SysDictEntity> chartTypeList = sysDictService
				.list(new LambdaQueryWrapper<SysDictEntity>().eq(SysDictEntity::getType, Constant.DICT_CHART_TYPE));
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

	private List<Map<String, Object>> handlePanelExportDataList(List<VisualPanel> panelList, Map<String, String> chartTypeAndI18nMap) {
		if(CollectionUtils.isEmpty(panelList)){
			return null;
		}
		List<Map<String, Object>> exportDataList = new ArrayList<>();
		Map<String, Object> tempMap;
		for (VisualPanel panel : panelList) {
			if (panel == null) continue;
			tempMap = new LinkedHashMap<>();
			tempMap.put(PanelImportEnum.NAME.getName(), panel.getName());
			tempMap.put(PanelImportEnum.TYPE.getName(), panel.getType());
			tempMap.put(PanelImportEnum.VARTYPE.getName(), panel.getVarType());
			tempMap.put(PanelImportEnum.PARAM.getName(), panel.getParam());
			tempMap.put(PanelImportEnum.REMARK.getName(), panel.getRemark());
			// chartParams 不可复用
			Map<String, Object> chartParams = Tool.MapUtil.newHashMap();
			chartParams.put("returnChildren", "1");
			chartParams.put("groupId", 0);
			chartParams.put("pageSize", "-1");
			chartParams.put("panelId", panel.getId());
			List<VisualChart> childChartList = (List<VisualChart>) visualChartService.queryPage(chartParams).getList();
			tempMap.put(PanelImportEnum.CHARTS.getName(), this.handleChartsExportDataList(childChartList, chartTypeAndI18nMap));
			tempMap.put(PanelImportEnum.CHILDREN.getName(), this.handlePanelExportDataList(panel.getChildren(), chartTypeAndI18nMap));
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
			tempMap.put(ChartsImportEnum.ELEMENTS.getName(), this.handleChartElementForExport(pChart.getElements()));
			tempMap.put(ChartsImportEnum.DATASOURCE.getName(), pChart.getDatasource());
			tempMap.put(ChartsImportEnum.CHILDREN.getName(), this.handleChartsExportDataList(pChart.getChildren(), chartTypeAndI18nMap));
			chartsMap.add(tempMap);
		}
		return chartsMap;
	}

	private List<Map> handleChartElementForExport(List<VisualChartElement> chartElements) {
		if (CollectionUtils.isEmpty(chartElements)) {
			return null;
		}
		List<Map> handleMaps = new ArrayList<>();
		Map tempMap;
		for (VisualChartElement element : chartElements) {
			tempMap = new LinkedHashMap(4);
			tempMap.put("expression", element.getExpression());
			tempMap.put("state", element.getState());
			if (ToolUtil.isNotEmpty(element.getLegend())) {
				tempMap.put("legend", element.getLegend());
			}
			if (ToolUtil.isNotEmpty(element.getName())) {
				tempMap.put("name", element.getName());
			}
			handleMaps.add(tempMap);
		}
		return handleMaps;
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
