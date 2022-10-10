package com.nis.modules.panel.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.CommonUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.JSONUtil;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ResponseUtil;
import com.nis.common.utils.StringUtils;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.agent.service.LokiApiService;
import com.nis.modules.agent.service.PromApiService;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.panel.dao.VisualPanelDao;
import com.nis.modules.panel.entity.VisualChart;
import com.nis.modules.panel.entity.VisualChartElement;
import com.nis.modules.panel.entity.VisualPanel;
import com.nis.modules.panel.service.VisualChartElementService;
import com.nis.modules.panel.service.VisualChartService;
import com.nis.modules.panel.service.VisualPanelService;
import com.nis.modules.panel.service.VisualService;
import com.nis.modules.report.job.ReportJobManager;
import com.nis.modules.stat.service.impl.StatServiceImpl;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.entity.SysUserStarredEntity;
import com.nis.modules.sys.service.BasicImportAndExportServices;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysDictService;
import com.nis.modules.sys.service.SysI18nService;
import com.nis.modules.sys.service.SysUserStarredService;
import com.nis.modules.sys.shiro.ShiroUtils;

import cn.hutool.core.thread.NamedThreadFactory;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;

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
	public List<VisualPanel> queryList(Map<String, Object> params) {
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
		// 2. dashboard 类型 需要进行排序返回
		List<VisualPanel> panelList = new ArrayList<>();
		if (StrUtil.equals(Constant.PanelType.DASHBOARD.getValue(), type)) {
			panelList = this.baseMapper.queryList(params);
			if (CollectionUtils.isNotEmpty(panelList) && panelList.size() != 1) {
				panelList = this.getVisualPanelTreeList(panelList, 0);
			}
		} else {
			// 3. 其他类型不进行排序
			panelList = this.baseMapper.queryList(params);
			for (VisualPanel visualPanel : panelList) {
				if (ToolUtil.isNotEmpty(visualPanel.getParam()))
					visualPanel.setParam(JSONObject.parseObject(String.valueOf(visualPanel.getParam()), Map.class));
			}
		}

		// 查询当前panel下的chart数量并且进行赋值
//        Map<Integer, Integer> chartNums = visualChartService.queryChartNum();
//        for(VisualPanel visualPanel : panelList) {
//        	Integer chartNum = ObjectUtil.isEmpty(chartNums.get(visualPanel.getId()))?0:chartNums.get(visualPanel.getId());
//        	visualPanel.setChartNum(chartNum);
//        }
		return panelList;
	}

	/**
	 * 递归获取树结构
	 *
	 * @param panelList
	 * @param parentId
	 * @return
	 */
	private List<VisualPanel> getVisualPanelTreeList(List<VisualPanel> panelList, Integer parentId) {
		List<VisualPanel> resultList = new ArrayList<>();
		for (VisualPanel panel : panelList) {
			if (ToolUtil.isNotEmpty(panel.getParam()) && panel.getParam() instanceof String)
				panel.setParam(JSONObject.parseObject(String.valueOf(panel.getParam()), Map.class));
			Integer id = panel.getId();
			Integer pid = panel.getPid();
			if (parentId.equals(pid)) {
				panel.setChildren(this.getVisualPanelTreeList(panelList, id));
				resultList.add(panel);
			}
		}
		return resultList;
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
				.eq(VisualPanel::getName, panel.getName()));
		boolean flag = false;
		// 当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
		if ((list == null || list.size() == 0)
				|| (list != null && list.size() == 1 && list.get(0).getId().equals(panel.getId()))) {
			flag = true;
		}

		if (!flag) {
			throw new NZException(RCode.PANEL_NAME_DUPLICATE);
		}

		// link 不为空时
		if (ToolUtil.isNotEmpty(panel.getLink())) {
			// 校验 type
			String type = panel.getType();
			ValidateUtils.is(type).notNull(RCode.PANEL_TYPE_ISNULL);
			List<Constant.PanelType> panelTypes = Arrays.asList(Constant.PanelType.values());
			List<String> types = panelTypes.stream().map(Constant.PanelType::getValue).collect(Collectors.toList());
			if (!types.contains(type)) {
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
		if (ToolUtil.isNotEmpty(panel.getParam())) {
			Map param = JSONObject.parseObject(JSONObject.toJSONString(panel.getParam()), Map.class);
			if (ToolUtil.isNotEmpty(param.get("report"))) {
				Map report = JSONObject.parseObject(JSONObject.toJSONString(param.get("report")), Map.class);
				// enable 为 true 时
				if (ToolUtil.isNotEmpty(report.get("enable"))
						&& ToolUtil.equals("true", report.get("enable").toString())) {
					ValidateUtils.is(report.get("range")).notNull(RCode.PANEL_PARAM_REPORTRANGE_ISNULL)
							.and(report.get("schedule")).notNull(RCode.PANEL_PARAM_REPORTSCHEDULE_ISNULL)
							.and(report.get("receivers")).notNull(RCode.PANEL_PARAM_REPORTRECEIVERS_ISNULL);
					Map range = JSONObject.parseObject(JSONObject.toJSONString(report.get("range")), Map.class);
					ValidateUtils.is(range.get("type")).notNull(RCode.PANEL_PARAM_RANGETYPE_ISNULL)
							.and(range.get("interval")).notNull(RCode.PANEL_PARAM_RANGEINTERVAL_ISNULL)
							.and(range.get("unit")).notNull(RCode.PANEL_PARAM_RANGEUNIT_ISNULL);
					if (!Arrays.asList(new String[] { "previous", "last" }).contains(range.get("type"))) {
						throw new NZException(RCode.PANEL_PARAM_RANGETYPE_INVALIDE);
					} else if (!Arrays.asList(new String[] { "hour", "day", "week", "month" })
							.contains(range.get("unit"))) {
						throw new NZException(RCode.PANEL_PARAM_RANGEUNIT_INVALIDE);
					}
					Map schedule = JSONObject.parseObject(JSONObject.toJSONString(report.get("schedule")), Map.class);
					ValidateUtils.is(schedule.get("type")).notNull(RCode.PANEL_PARAM_SCHEDULETYPE_ISNULL)
							.and(schedule.get("stime")).notNull(RCode.PANEL_PARAM_SCHEDULESTIME_ISNULL);
					if (!Arrays.asList(new Integer[] { 1, 2, 3, 4 }).contains(schedule.get("type"))) {
						throw new NZException(RCode.PANEL_PARAM_SCHEDULETYPE_INVALIDE);
					}
					if (ToolUtil.equals(schedule.get("type").toString(), "2")) {
						ValidateUtils.is(schedule.get("repeat")).notNull(RCode.PANEL_PARAM_SCHEDULEREPEAT_ISNULL);
					} else if (ToolUtil.equals(schedule.get("type").toString(), "3")) {
						ValidateUtils.is(schedule.get("repeat")).notNull(RCode.PANEL_PARAM_SCHEDULEREPEAT_ISNULL)
								.and(schedule.get("nums")).notNull(RCode.PANEL_PARAM_SCHEDULENUMS_ISNULL);
						if (!Arrays.asList(new Integer[] { 1, 2, 3, 4, 5, 6, 7 }).containsAll(
								JSONArray.parseObject(JSONObject.toJSONString(schedule.get("nums")), List.class))) {
							throw new NZException(RCode.PANEL_PARAM_SCHEDULENUMS_INVALIDE);
						}
					} else if (ToolUtil.equals(schedule.get("type").toString(), "4")) {
						ValidateUtils.is(schedule.get("nums")).notNull(RCode.PANEL_PARAM_SCHEDULENUMS_ISNULL);
						List<Integer> monthlyList = new ArrayList<>();
						for (int i = 1; i < 32; i++) {
							monthlyList.add(i);
						}
						monthlyList.add(-1);
						if (!monthlyList.containsAll(
								JSONArray.parseObject(JSONObject.toJSONString(schedule.get("nums")), List.class))) {
							throw new NZException(RCode.PANEL_PARAM_SCHEDULENUMS_INVALIDE);
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
					throw new NZException(RCode.PANEL_PARAM_REPORTCHARTSHARE_INVALIDE);
				}
			} else {
				param.put("chartShare", "none");
			}
			panel.setParam(JSONObject.toJSONString(param));
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveVisualPanel(VisualPanel panel) {
		this.validatePanel(panel);

		Long userId = ShiroUtils.getUserId();
		panel.setCreateBy(userId.intValue());

		if (ToolUtil.isEmpty(panel.getLink())) {
			panel.setType(Constant.PanelType.DASHBOARD.getValue());
		}
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
	public Map importChartData(MultipartFile multipartFile, String type, Integer linkId, String language)
			throws IOException {
		String configKey = this.getChartTemplateConfigKeyByType(type);
		String sysHeaderJsonStr = sysConfigService.getValue(configKey);
		Map<String, String> exportHeaderMap = basicImportAndExportServices.getI18nHeaderMap(sysHeaderJsonStr);
		List<Map<Integer, String>> importDataList = basicImportAndExportServices
				.getImportDataFromMultipartFile(multipartFile, sysHeaderJsonStr);

		List<Map> failDetails = new ArrayList<>();
		String seqUUID = UUID.randomUUID().toString();
		type = StringUtils.isEmpty(type) ? Constant.PanelType.DASHBOARD.getValue() : type;

		ArrayList<String> headerInfos = new ArrayList<>(exportHeaderMap.keySet());
		int successNum = 0;
		List<Map<Integer, String>> handleImportData = null;
		if (StrUtil.equals(Constant.PanelType.DASHBOARD.getValue(), type)) {
			// 处理excel表数据 将group chart相关排在前列
			handleImportData = this.groupChartRowHandle(importDataList, 1);
			successNum = this.saveDashboardChartData(handleImportData, failDetails, headerInfos, seqUUID);
		} else {
			handleImportData = this.groupChartRowHandle(importDataList, 0);
			successNum = this.saveAssetEndpointChartData(handleImportData, failDetails, seqUUID, type, linkId,
					headerInfos);
		}
		int totalNum = ToolUtil.isNotEmpty(handleImportData) ? handleImportData.size() : 0;

		Map resultMap = new HashMap(8);
		resultMap.put("seq", seqUUID);
		resultMap.put("totalNum", totalNum);
		resultMap.put("successNum", successNum);
		resultMap.put("failNum", totalNum - successNum);
		resultMap.put("failDetail", CommonUtils.handleImportFailDetails(failDetails));

		return resultMap;
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
		// 导出查询与列表查询一致
		List<VisualChart> charts = this.exportDataByParams(params);
		String type = Constant.PanelType.DASHBOARD.getValue();
		if (CollectionUtils.isNotEmpty(charts)) {
			VisualChart chart = charts.stream().findFirst().get();
			VisualPanel panel = this.getById(chart.getPanelId());
			if (panel == null || !StrUtil.equals(Constant.PanelType.DASHBOARD.getValue(), panel.getType())) {
				type = Constant.PanelType.ASSET.getValue();
			}
		}
		// 获取导出配置 key 名称
		String configKey = this.getChartTemplateConfigKeyByType(type);
		String sysHeaderJsonStr = sysConfigService.getValue(configKey);

		charts = charts.stream().sorted(Comparator.comparing(VisualChart::getGroupId)).collect(Collectors.toList());
		String fileName = "Charts";
		String formatStr = params.get("format").toString();

		if (StrUtil.equals(Constant.PanelType.DASHBOARD.getValue(), type)) {
			List<List<String>> exportDataList = this.handleDashboardChartExportDataList(charts);
			basicImportAndExportServices.exportDataByFormat(response, exportDataList, sysHeaderJsonStr, formatStr,
					fileName);
		} else {
			List<List<String>> exportDataList = this.handleAssetEndpointChartExportDataList(charts);
			basicImportAndExportServices.exportDataByFormat(response, exportDataList, sysHeaderJsonStr, formatStr,
					fileName);
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

	private List<List<String>> handleAssetEndpointChartExportDataList(List<VisualChart> charts) {
		Map<Integer, String> varTypeIdAndNameMap = new HashMap<>(4);
		varTypeIdAndNameMap.put(1, "Asset");
		varTypeIdAndNameMap.put(2, "Endpoint");

		// type 导出转换为 国际化之后的名称
		List<SysDictEntity> chartTypeList = sysDictService
				.list(new LambdaQueryWrapper<SysDictEntity>().eq(SysDictEntity::getType, Constant.DICT_CHART_TYPE));
		Map<String, String> chartTypeAndI18nMap = chartTypeList.stream()
				.collect(Collectors.toMap(SysDictEntity::getValue, SysDictEntity::getI18nCode));

		List<List<String>> exportDataList = new ArrayList<>();
		for (VisualChart chart : charts) {
			if (chart == null || Constant.CHART_ASSETINFO_TYPE.equalsIgnoreCase(chart.getType())
					|| Constant.CHART_ENDPOINTINFO_TYPE.equalsIgnoreCase(chart.getType()))
				continue;
			List<String> tempList = new ArrayList<>();
			String type = chart.getType();
			tempList.add(chart.getGroup().getName());
			tempList.add(chart.getName());
			String typeI18nCode = chartTypeAndI18nMap.get(type);
			tempList.add(sysI18nService.queryValue(typeI18nCode));
			tempList.add(ToolUtil.isEmpty(varTypeIdAndNameMap.get(chart.getVarType())) ? ""
					: varTypeIdAndNameMap.get(chart.getVarType()));
			tempList.add(ObjectUtil.defaultIfNull(chart.getSpan(), "").toString());
			tempList.add(ObjectUtil.defaultIfNull(chart.getHeight(), "").toString());
			if (StrUtil.equals(Constant.CHART_TEXT_TYPE, type)) {
				tempList.add("");
			} else {
				tempList.add(this.getUnitDescrByVal(chart.getUnit()));
			}
			tempList.add(this.handleChartParam(chart.getParam()));
			tempList.add(this.handleChartElementForExport(chart.getElements()));
			tempList.add(chart.getRemark());
			tempList.add(chart.getDatasource());
			tempList.add(ObjectUtil.defaultIfNull(chart.getX(), "").toString());
			tempList.add(ObjectUtil.defaultIfNull(chart.getY(), "").toString());
			exportDataList.add(tempList);
		}
		return exportDataList;
	}

	private String handleChartElementForExport(List<VisualChartElement> chartElements) {
		if (CollectionUtils.isEmpty(chartElements)) {
			return "";
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
		return JSONArray.toJSONString(handleMaps);
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

	private String handleChartParam(Object param) {
		if (ToolUtil.isEmpty(param)) {
			return "";
		}
		Map<String, Object> map = JSONObject.parseObject(JSONObject.toJSONString(param), Map.class);
		map.entrySet().removeIf(entry -> ToolUtil.isEmpty(entry.getValue()));
		return ToolUtil.isEmpty(map) ? "" : JSONObject.toJSONString(map);
	}

	public void getPanelNamePathAndIdMapFromTree(VisualPanel panel, String path,
			Map<String, Integer> nodeNamePathAndIdMap) {
		if (CollectionUtils.isEmpty(panel.getChildren())) {
			return;
		}
		for (VisualPanel cPanel : panel.getChildren()) {
			String cPath = "";
			cPath += path + "/" + cPanel.getName();
			nodeNamePathAndIdMap.put(cPath, cPanel.getId());
			this.getPanelNamePathAndIdMapFromTree(cPanel, cPath, nodeNamePathAndIdMap);
		}
	}

	private int saveDashboardChartData(List<Map<Integer, String>> importDataList, List<Map> failDetails,
			List<String> headerInfos, String seqUUID) {
		List<VisualPanel> panelTreeList = this.queryList(new HashMap());
		Map<String, Integer> nodeNamePathAndIdMap = new HashMap<>();
		for (VisualPanel panel : panelTreeList) {
			nodeNamePathAndIdMap.put(panel.getName(), panel.getId());
			this.getPanelNamePathAndIdMapFromTree(panel, panel.getName(), nodeNamePathAndIdMap);
		}

		List<VisualPanel> dashboardTypePanelList = this.list(new LambdaQueryWrapper<VisualPanel>()
				.eq(VisualPanel::getType, Constant.PanelType.DASHBOARD.getValue()));
		Integer panelMaxWeight;
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

		int successNum = 0;
		for (int i = 0; i < importDataList.size(); i++) {
			int dataFromExcelRowNum = i + 1;
			Map<Integer, String> dataMap = importDataList.get(i);

			boolean saveFlag = true;
			boolean typeFlag = true;
			String panelName = dataMap.get(0);
			if (StringUtils.isEmpty(panelName)) {
				saveFlag = false;
				this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0),
						RCode.PANEL_NAME_ISNULL.getMsg());
			}
			Integer panelId = this.handlePanelNameForCellVal(nodeNamePathAndIdMap, panelName, seqUUID,
					++panelMaxWeight);
			Integer groupId = 0;
			String groupName = dataMap.get(1);
			if (StrUtil.isNotBlank(groupName) && saveFlag) {
				VisualChart groupChart = visualChartService.getOne(new QueryWrapper<VisualChart>().lambda()
						.eq(VisualChart::getName, groupName).eq(VisualChart::getPanelId, panelId)
						.eq(VisualChart::getType, Constant.CHART_GROUP_TYPE.toString()));
				if (ToolUtil.isEmpty(groupChart)) {
					groupChart = new VisualChart();
					groupChart.setPanelId(panelId);
					groupChart.setName(groupName);
					groupChart.setGroupId(0);
					groupChart.setSpan(12d);
					groupChart.setHeight(-1d);
					groupChart.setUpdateBy((ShiroUtils.getUserId()).intValue());
					groupChart.setUpdateAt(new Date());
					groupChart.setType(Constant.CHART_GROUP_TYPE.toString());
					groupChart.setUnit(2);
					groupChart.setBuildIn(0);
					groupChart.setRemark("");
					groupChart.setSeq(seqUUID);
					// group 默认 datasource misc
					groupChart.setDatasource(Constant.ChartDatasourceType.MISC.getValue());
					visualChartService.save(groupChart);
				}
				groupId = groupChart.getId();
			}

			String name = dataMap.get(2);
			if (StringUtils.isEmpty(name)) {
				saveFlag = false;
				this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2),
						RCode.CHART_TITLE_ISNULL.getMsg());
			} else if (name.length() > 64) {
				saveFlag = false;
				this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2),
						RCode.CHART_NAME_TOO_LONG.getMsg());
			}
			String type = dataMap.get(3);

			if (StringUtils.isEmpty(type)) {
				saveFlag = typeFlag = false;
				this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3),
						RCode.CHART_TYPE_ISNULL.getMsg());
			} else {
				// 根据可读名称，获取 chart type
				type = typeReadableNameMap.get(type);
				if (StringUtils.isEmpty(type)) {
					saveFlag = typeFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3),
							RCode.CHART_TYPE_INVALIDE.getMsg());
				}
			}

			VisualChart groupChart = visualChartService
					.getOne(new QueryWrapper<VisualChart>().lambda().eq(VisualChart::getName, name)
							.eq(VisualChart::getPanelId, panelId).eq(VisualChart::getType, Constant.CHART_GROUP_TYPE));
			if (StrUtil.equals(type, Constant.CHART_GROUP_TYPE) && ToolUtil.isNotEmpty(groupChart)) {
				// chart group 不允许重复
				saveFlag = false;
				this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3),
						RCode.CHART_GROUP_NAME_DUPLICATE.getMsg());
			}

			String span = dataMap.get(4);
			if (StringUtils.isEmpty(span)) {
				saveFlag = false;
				this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4),
						RCode.CHART_SPAN_ISNULL.getMsg());
			} else {
				Integer width = null;
				try {
					width = Double.valueOf(span).intValue();
				} catch (NumberFormatException e) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4),
							RCode.CHART_WIDTH_FORMAT.getMsg());
				}
				if (width != null && (width < 1 || width > 12)) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4),
							RCode.CHART_WIDTH_INVALIDE.getMsg());
				}
			}
			String height = dataMap.get(5);
			if (StringUtils.isEmpty(height)) {
				saveFlag = false;
				this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(5),
						RCode.CHART_HEIGHT_ISNULL.getMsg());
			} else {
				Integer heightIntVal = null;
				try {
					heightIntVal = Double.valueOf(height).intValue();
				} catch (NumberFormatException e) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(5),
							RCode.CHART_HEIGHT_FORMAT.getMsg());
				}
				if (heightIntVal != null && (heightIntVal < 1 || heightIntVal > 12)) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(5),
							RCode.CHART_HEIGHT_INVALIDE.getMsg());
				}
			}
			String unit = dataMap.get(6);
			if (StringUtils.isNotEmpty(unit)) {
				if (ToolUtil.isEmpty(Constant.ALERT_RULE_UNIT_MAP.get(unit))) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(6),
							RCode.CHART_UNIT_NOTFOUND.getMsg());
				}
			}
			String param = dataMap.get(7);
			if (typeFlag) {
				String msg = this.validateChartParamForImport(type, param, "dashboard");
				if (StringUtils.isNotEmpty(msg)) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(7), msg);
				}
			}

			String datasource = dataMap.get(10);
			if (StringUtils.isEmpty(datasource)) {
				saveFlag = typeFlag = false;
				this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(10),
						RCode.CHART_DATASOURCE_ISNULL.getMsg());
			} else {
				if (!datasources.contains(datasource)) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(10),
							RCode.CHART_DATASOURCE_INVALIDE.getMsg());
				}
			}

			String elements = dataMap.get(8);
			if (typeFlag) {
				String msg = this.validateChartElementForImport(datasource, elements);
				if (StringUtils.isNotEmpty(msg)) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(8), msg);
				}
			}
			String description = dataMap.get(9);

			String x = dataMap.get(11);
			if (StringUtils.isEmpty(x)) {
				saveFlag = false;
				this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(11),
						RCode.CHART_X_ISNULL.getMsg());
			} else {
				Integer xIntVal = null;
				try {
					xIntVal = Double.valueOf(x).intValue();
				} catch (NumberFormatException e) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(11),
							RCode.CHART_X_FORMAT.getMsg());
				}
				if (xIntVal != null && (xIntVal < 0 || xIntVal > 12)) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(11),
							RCode.CHART_X_INVALIDE.getMsg());
				}
			}

			// x和width之和不能大于12
			if (StringUtils.isNotEmpty(x) && StringUtils.isNotEmpty(span)) {
				Double xVal = Double.valueOf(x);
				Double widthVal = Double.valueOf(span);
				if (xVal + widthVal > 12) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(11),
							RCode.CHART_X_WIDTH_INVALIDE.getMsg());
				}
			}

			String y = dataMap.get(12);
			if (StringUtils.isEmpty(y)) {
				saveFlag = false;
				this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(12),
						RCode.CHART_Y_ISNULL.getMsg());
			} else {
				Integer yIntVal = null;
				try {
					yIntVal = Double.valueOf(y).intValue();
				} catch (NumberFormatException e) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(12),
							RCode.CHART_Y_FORMAT.getMsg());
				}
			}

			if (saveFlag) {
				TransactionStatus transactionStatus = dataSourceTransactionManager
						.getTransaction(transactionDefinition);
				try {
					VisualChart chart = new VisualChart();
					chart.setSeq(seqUUID);
					chart.setPanelId(panelId);
					chart.setName(name);
					chart.setType(type);
					chart.setSpan(Double.valueOf(span));
					chart.setHeight(Double.valueOf(height));
					// unit 为空 设置默认值为 short
					unit = StringUtils.isEmpty(unit) ? "short" : unit;
					chart.setUnit(Constant.ALERT_RULE_UNIT_MAP.get(unit));
					chart.setParam(param.toString());
					chart.setUpdateAt(new Date());
					chart.setUpdateBy((ShiroUtils.getUserId()).intValue());
					chart.setGroupId(groupId);
					chart.setRemark(description);
					chart.setDatasource(datasource);
					chart.setX(Double.valueOf(x));
					chart.setY(Double.valueOf(y));
					// 查询当前 panel 下的 charts
					List<VisualChart> chartList = visualChartService.list(
							new QueryWrapper<VisualChart>().lambda().eq(VisualChart::getPanelId, chart.getPanelId()));
					if (CollectionUtils.isEmpty(chartList)) {
						visualChartService.save(chart);
					} else {
						VisualChart endChart = visualChartService.getOne(
								new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getPanelId, chart.getPanelId())
										.orderByDesc(VisualChart::getWeight).last("limit 0,1"));
						Integer weight = endChart.getWeight();
						chart.setWeight(++weight);
						visualChartService.save(chart);
					}
					List<VisualChartElement> chartElements = JSONArray.parseArray(elements, VisualChartElement.class);
					if (CollectionUtils.isNotEmpty(chartElements)) {
						chartElements.forEach(chartElement -> {
							chartElement.setChartId(chart.getId());
							chartElement.setSeq(seqUUID);
							chartElement.setType(Constant.CHART_ELEMENT_EXPERT_TYPE);
						});
						chartElementService.saveBatch(chartElements);
					}
					dataSourceTransactionManager.commit(transactionStatus);
					successNum++;
				} catch (RuntimeException e) {
					dataSourceTransactionManager.rollback(transactionStatus);
					this.addFailDetail(dataFromExcelRowNum, failDetails, "System", e.getMessage() + "");
					log.error(String.format("保存 panel chart chartelement失败,回滚事务,错误信息:%s", e));
				}
			}
		}
		return successNum;
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

	/**
	 * @param panelName 格式： 使用 / 作为字符分隔，表示 父子节点关系
	 * @param seqUUID   导入序列号
	 */
	private Integer handlePanelNameForCellVal(Map<String, Integer> nodeNamePathAndIdMap, String panelName,
			String seqUUID, Integer panelWeight) {
		String savePanelNames = panelName;
		Integer panelId;
		while (true) {
			panelId = nodeNamePathAndIdMap.get(panelName);
			if (panelId != null) {
				break;
			}
			// 仍然包含 ‘/’ 证明还是有父子关系
			if (panelName.lastIndexOf("/") != -1) {
				panelName = panelName.substring(0, panelName.lastIndexOf("/"));
			} else {
				// 最后的 panel 也不存在，赋值 为空 ，下面会进行过滤添加
				panelName = "";
				break;
			}
		}
		savePanelNames = savePanelNames.replace(panelName, "");

		// 如果存在需要保存的 panel
		if (StringUtils.isNotEmpty(savePanelNames)) {
			if (savePanelNames.startsWith("/")) {
				savePanelNames = savePanelNames.substring(savePanelNames.indexOf("/") + 1);
			}

			VisualPanel panel = new VisualPanel();
			panel.setType(Constant.PanelType.DASHBOARD.getValue());
			panel.setCreateBy(ShiroUtils.getUserId().intValue());
			panel.setSeq(seqUUID);
			panel.setWeight(panelWeight);
			panel.setUts(System.currentTimeMillis());

			List<String> saveList = Arrays.asList(savePanelNames.split("/"));
			int size = saveList.size();
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < size; i++) {
				String name = saveList.get(i);
				panel.setName(name);
				if (panel.getId() == null) {
					panel.setPid(panelId);
				} else {
					panel.setPid(panel.getId());
				}
				this.save(panel);
				sb.append(name + "/");
				String temp = panelName + "/" + sb.substring(0, sb.lastIndexOf("/"));
				// 因为用户输入根节点没有 ‘/’，所以不保存根节点 '/' 路径作为对比
				if (temp.startsWith("/")) {
					temp = temp.substring(temp.indexOf("/") + 1);
				}
				nodeNamePathAndIdMap.put(temp, panel.getId());
			}
			return panel.getId();
		}
		return panelId;
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

			if (StrUtil.equals(Constant.PanelType.MODEL.getValue(), panelType)) {
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

	private void addFailDetail(int i, List<Map> failDetails, String headerInfo, String msg) {
		Map failDetail = new HashMap(4);
		failDetail.put("lineNo", i);
		failDetail.put("errorMsg", headerInfo + ": " + msg);
		failDetails.add(failDetail);
	}

	private int saveAssetEndpointChartData(List<Map<Integer, String>> importDataList, List<Map> failDetails,
			String seqUUID, String panelType, Integer linkId, List<String> headerInfos) {
		// 导入类型 template 标识
		Boolean templateFlag = Constant.PanelType.TEMPLATE.getValue().equalsIgnoreCase(panelType);
		// chart template 导入导出 varType map
		Map<String, Integer> varTypeAndValMap = new HashMap<>(4);
		varTypeAndValMap.put("Asset", 1);
		varTypeAndValMap.put("Endpoint", 2);

		if (linkId == null && !templateFlag)
			throw new NZException(RCode.PANEL_CHART_IMPORT_LINKID_ISNULL);

		VisualPanel panel = this.getOne(new QueryWrapper<VisualPanel>().lambda().eq(VisualPanel::getType, panelType)
				.eq(VisualPanel::getLink, linkId));
		if (panel == null && !templateFlag)
			throw new NZException(RCode.PANEL_NOT_EXIST);

		// 查询 chart datasource类型防止频繁查库
		List<SysDictEntity> chartDatasources = sysDictService.list(
				new QueryWrapper<SysDictEntity>().lambda().eq(SysDictEntity::getType, Constant.DICT_CHART_DATASOURCE));
		List<String> datasources = chartDatasources.stream().map(SysDictEntity::getValue).collect(Collectors.toList());

		// chart 导入 Type 是国际化后的，这里统一获取 chart type 可读名称 & 真实 type 值
		Map<String, String> typeReadableNameMap = this.getChartTypeReadableNameMap();

		int successNum = 0;
		for (int i = 0; i < importDataList.size(); i++) {
			int dataFromExcelRowNum = i + 1;
			Map<Integer, String> dataMap = importDataList.get(i);
			boolean saveFlag = true;
			boolean typeFlag = true;

			Integer groupId = 0;
			String groupName = dataMap.get(0);
			String varType = dataMap.get(3);
			if (StrUtil.isNotBlank(groupName)) {
				VisualChart groupChart;
				if (templateFlag) {
					// template 类型，panel = 0 为 template
					groupChart = visualChartService
							.getOne(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getName, groupName)
									.eq(VisualChart::getPanelId, 0).eq(VisualChart::getType, Constant.CHART_GROUP_TYPE)
									.eq(VisualChart::getVarType, varTypeAndValMap.get(varType.trim())));
				} else {
					// asset endpoint 类型，通过指定 panel group name 查找 group
					groupChart = visualChartService.getOne(new QueryWrapper<VisualChart>().lambda()
							.eq(VisualChart::getName, groupName).eq(VisualChart::getPanelId, panel.getId())
							.eq(VisualChart::getType, Constant.CHART_GROUP_TYPE.toString()));
				}
				if (ToolUtil.isEmpty(groupChart)) {
					groupChart = new VisualChart();
					// panel 为 null,即为 template type, panel_id = 0
					groupChart.setPanelId(ToolUtil.isEmpty(panel) ? 0 : panel.getId());
					groupChart.setName(groupName);
					groupChart.setGroupId(0);
					groupChart.setSpan(12d);
					groupChart.setHeight(-1d);
					groupChart.setUpdateBy((ShiroUtils.getUserId()).intValue());
					groupChart.setUpdateAt(new Date());
					groupChart.setType(Constant.CHART_GROUP_TYPE.toString());
					groupChart.setUnit(2);
					groupChart.setBuildIn(0);
					groupChart.setRemark("");
					if (templateFlag) {
						groupChart.setVarType(varTypeAndValMap.get(varType.trim()));
					}
					// group 默认 datasource misc
					groupChart.setDatasource(Constant.ChartDatasourceType.MISC.getValue());
					groupChart.setSeq(seqUUID);
					visualChartService.save(groupChart);
				}
				groupId = groupChart.getId();
			}

			String name = dataMap.get(1);
			if (StringUtils.isEmpty(name)) {
				saveFlag = false;
				this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0),
						RCode.CHART_TITLE_ISNULL.getMsg());
			} else if (name.length() > 64) {
				saveFlag = false;
				this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2),
						RCode.CHART_NAME_TOO_LONG.getMsg());
			}

			String type = dataMap.get(2);
			if (StringUtils.isEmpty(type)) {
				saveFlag = typeFlag = false;
				this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2),
						RCode.CHART_TYPE_ISNULL.getMsg());
			} else {
				// 根据可读名称，获取 chart type
				type = typeReadableNameMap.get(type);
				if (StringUtils.isEmpty(type)) {
					saveFlag = typeFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2),
							RCode.CHART_TYPE_INVALIDE.getMsg());
				}
			}

			VisualChart groupChart = null;
			if (templateFlag) {
				groupChart = visualChartService
						.getOne(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getName, name)
								.eq(VisualChart::getPanelId, 0).eq(VisualChart::getType, Constant.CHART_GROUP_TYPE)
								.eq(VisualChart::getVarType, varTypeAndValMap.get(varType.trim())));
			} else {
				groupChart = visualChartService.getOne(new QueryWrapper<VisualChart>().lambda()
						.eq(VisualChart::getName, name).eq(VisualChart::getPanelId, panel.getId())
						.eq(VisualChart::getType, Constant.CHART_GROUP_TYPE));
			}
			if (StrUtil.equals(type, Constant.CHART_GROUP_TYPE) && ToolUtil.isNotEmpty(groupChart)) {
				// chart group 不允许重复
				saveFlag = false;
				this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2),
						RCode.CHART_GROUP_NAME_DUPLICATE.getMsg());
			}

			// varType, type = template required

			if (templateFlag) {
				if (StringUtils.isEmpty(varType)) {
					saveFlag = typeFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3),
							RCode.CHART_VARTYPE_ISNULL.getMsg());
				} else if (ToolUtil.isEmpty(varTypeAndValMap.get(varType.trim()))) {
					saveFlag = typeFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3),
							RCode.CHART_VARTYPE_ERROR.getMsg());
				}
			}

			String span = dataMap.get(4);
			if (StringUtils.isEmpty(span)) {
				saveFlag = false;
				this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4),
						RCode.CHART_SPAN_ISNULL.getMsg());
			} else {
				Integer width = null;
				try {
					width = Double.valueOf(span).intValue();
				} catch (NumberFormatException e) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4),
							RCode.CHART_WIDTH_FORMAT.getMsg());
				}
				if (width != null && (width < 1 || width > 12)) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4),
							RCode.CHART_WIDTH_INVALIDE.getMsg());
				}
			}
			String height = dataMap.get(5);
			if (StringUtils.isEmpty(height)) {
				saveFlag = false;
				this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(5),
						RCode.CHART_HEIGHT_ISNULL.getMsg());
			} else {
				Integer heightIntVal = null;
				try {
					heightIntVal = Double.valueOf(height).intValue();
				} catch (NumberFormatException e) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(5),
							RCode.CHART_HEIGHT_FORMAT.getMsg());
				}
				if (heightIntVal != null && (heightIntVal < 1 || heightIntVal > 12)) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(5),
							RCode.CHART_HEIGHT_INVALIDE.getMsg());
				}
			}
			String unit = dataMap.get(6);
			if (StringUtils.isNotEmpty(unit)) {
				if (ToolUtil.isEmpty(Constant.ALERT_RULE_UNIT_MAP.get(unit))) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(6),
							RCode.CHART_UNIT_NOTFOUND.getMsg());
				}
			}
			String param = dataMap.get(7);
			if (typeFlag) {
				String msg = this.validateChartParamForImport(type, param, type);
				if (StringUtils.isNotEmpty(msg)) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(7), msg);
				}
			}

			String datasource = dataMap.get(10);
			if (StringUtils.isEmpty(datasource)) {
				saveFlag = typeFlag = false;
				this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(10),
						RCode.CHART_DATASOURCE_ISNULL.getMsg());
			} else {
				if (!datasources.contains(datasource)) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(10),
							RCode.CHART_DATASOURCE_INVALIDE.getMsg());
				}
			}

			String elements = dataMap.get(8);
			if (typeFlag) {
				String msg = this.validateChartElementForImport(datasource, elements);
				if (StringUtils.isNotEmpty(msg)) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(8), msg);
				}
			}
			String description = dataMap.get(9);

			String x = dataMap.get(11);
			if (StringUtils.isEmpty(x)) {
				saveFlag = false;
				this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(11),
						RCode.CHART_X_ISNULL.getMsg());
			} else {
				Integer xIntVal = null;
				try {
					xIntVal = Double.valueOf(x).intValue();
				} catch (NumberFormatException e) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(11),
							RCode.CHART_X_FORMAT.getMsg());
				}
				if (xIntVal != null && (xIntVal < 0 || xIntVal > 12)) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(11),
							RCode.CHART_X_INVALIDE.getMsg());
				}
			}

			// x和width之和不能大于12
			if (StringUtils.isNotEmpty(x) && StringUtils.isNotEmpty(span)) {
				Double xVal = Double.valueOf(x);
				Double widthVal = Double.valueOf(span);
				if (xVal + widthVal > 12) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(11),
							RCode.CHART_X_WIDTH_INVALIDE.getMsg());
				}
			}

			String y = dataMap.get(12);
			if (StringUtils.isEmpty(y)) {
				saveFlag = false;
				this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(12),
						RCode.CHART_Y_ISNULL.getMsg());
			} else {
				Integer yIntVal = null;
				try {
					yIntVal = Double.valueOf(y).intValue();
				} catch (NumberFormatException e) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(12),
							RCode.CHART_Y_FORMAT.getMsg());
				}
			}

			if (saveFlag) {
				TransactionStatus transactionStatus = dataSourceTransactionManager
						.getTransaction(transactionDefinition);
				try {
					VisualChart chart = new VisualChart();
					chart.setSeq(seqUUID);
					// panel 为 null,即为 template type, panel_id = 0
					chart.setPanelId(ToolUtil.isEmpty(panel) ? 0 : panel.getId());
					chart.setName(name);
					chart.setType(type);
					chart.setSpan(Double.valueOf(span));
					chart.setHeight(Double.valueOf(height));
					// unit 为空 设置默认值为 short
					unit = StringUtils.isEmpty(unit) ? "short" : unit;
					chart.setUnit(Constant.ALERT_RULE_UNIT_MAP.get(unit));
					chart.setParam(param.toString());
					chart.setUpdateAt(new Date());
					chart.setUpdateBy((ShiroUtils.getUserId()).intValue());
					chart.setGroupId(groupId);
					chart.setVarType(varTypeAndValMap.get(varType));
					chart.setVarId(linkId);
					chart.setRemark(description);
					chart.setDatasource(datasource);
					chart.setX(Double.valueOf(x));
					chart.setY(Double.valueOf(y));
					// 查询当前 panel 下的 charts
					List<VisualChart> chartList = visualChartService.list(
							new QueryWrapper<VisualChart>().lambda().eq(VisualChart::getPanelId, chart.getPanelId()));
					if (CollectionUtils.isEmpty(chartList)) {
						visualChartService.save(chart);
					} else {
						VisualChart endChart = visualChartService.getOne(
								new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getPanelId, chart.getPanelId())
										.orderByDesc(VisualChart::getWeight).last("limit 0,1"));
						Integer weight = endChart.getWeight();
						chart.setWeight(++weight);
						visualChartService.save(chart);
					}
					List<VisualChartElement> chartElements = JSONArray.parseArray(elements, VisualChartElement.class);
					if (CollectionUtils.isNotEmpty(chartElements)) {
						chartElements.forEach(chartElement -> {
							chartElement.setChartId(chart.getId());
							chartElement.setSeq(seqUUID);
							chartElement.setType(Constant.CHART_ELEMENT_EXPERT_TYPE);
						});
						chartElementService.saveBatch(chartElements);
					}
					dataSourceTransactionManager.commit(transactionStatus);
					successNum++;
				} catch (RuntimeException e) {
					dataSourceTransactionManager.rollback(transactionStatus);
					this.addFailDetail(dataFromExcelRowNum, failDetails, "System", e.getMessage() + "");
					log.error(String.format("保存 panel chart chartelement失败,回滚事务,错误信息:%s", e));
				}
			}
		}
		return successNum;
	}

	private List<VisualChart> exportDataByParams(Map<String, Object> params) {
		String panelId = null;
		if (ToolUtil.isNotEmpty(params.get("panelId"))) {
			panelId = params.get("panelId").toString();
		}
		String chartId = null;
		if (ToolUtil.isNotEmpty(params.get("chartId"))) {
			chartId = params.get("chartId").toString();
		}
		String panelType = null;
		if (ToolUtil.isNotEmpty(params.get("type"))) {
			panelType = params.get("type").toString();
			if (!Constant.PanelType.TEMPLATE.getValue().equals(panelType)) {
				params.put("panelType", panelType);
			} else {
				panelId = "0";
			}
			params.remove("type");
		}
		List<VisualChart> chartList = new ArrayList<VisualChart>();
		if (chartId != null) {
			// 只导出当前 chart
			chartList.addAll(this.queryChartsByPanelId(panelId, chartId, params));
		} else if (ToolUtil.isEmpty(panelId)) {
			// 导出所有 dashboard panel
			List<VisualPanel> panelList = this.list(new LambdaQueryWrapper<VisualPanel>()
					.eq(VisualPanel::getType, Constant.PanelType.DASHBOARD.getValue())
					.orderByAsc(VisualPanel::getWeight, VisualPanel::getId));
			for (VisualPanel panel : panelList) {
				Map<String, Object> param = new HashMap<String, Object>();
				param.put("panelType", panelType);
				param.put("pageSize", (String) params.get("pageSize"));
				chartList.addAll(this.queryChartsByPanelId(panel.getId().toString(), null, param));
			}
		} else if (ToolUtil.isNotEmpty(panelId)) {
			// 导出 panel 下所有chart
			chartList.addAll(this.queryChartsByPanelId(panelId, null, params));
		}
		return chartList;
	}

	private List<List<String>> handleDashboardChartExportDataList(List<VisualChart> charts) {
		List<VisualPanel> panelTreeList = this.queryList(new HashMap());
		Map<Integer, String> nodeIdAndNamePathMap = new HashMap<>();
		for (VisualPanel panel : panelTreeList) {
			nodeIdAndNamePathMap.put(panel.getId(), panel.getName());
			this.getPanelIdAndNamePathMapFromTree(panel, panel.getName(), nodeIdAndNamePathMap);
		}
		// type 导出转换为 国际化之后的名称
		List<SysDictEntity> chartTypeList = sysDictService
				.list(new LambdaQueryWrapper<SysDictEntity>().eq(SysDictEntity::getType, Constant.DICT_CHART_TYPE));
		Map<String, String> chartTypeAndI18nMap = chartTypeList.stream()
				.collect(Collectors.toMap(SysDictEntity::getValue, SysDictEntity::getI18nCode));

		List<List<String>> exportDataList = new ArrayList<>();
		for (VisualChart chart : charts) {
			if (chart == null)
				continue;
			List<String> tempList = new ArrayList<>();
			String type = chart.getType();
			tempList.add(nodeIdAndNamePathMap.get(chart.getPanelId()));
			tempList.add(chart.getGroup().getName());
			tempList.add(chart.getName());
			String typeI18nCode = chartTypeAndI18nMap.get(type);
			tempList.add(sysI18nService.queryValue(typeI18nCode));
			tempList.add(ObjectUtil.defaultIfNull(chart.getSpan(), "").toString());
			tempList.add(ObjectUtil.defaultIfNull(chart.getHeight(), "").toString());
			if (StrUtil.equals(Constant.CHART_TEXT_TYPE, type)) {
				tempList.add("");
			} else {
				tempList.add(this.getUnitDescrByVal(chart.getUnit()));
			}
			tempList.add(this.handleChartParam(chart.getParam()));
			tempList.add(this.handleChartElementForExport(chart.getElements()));
			tempList.add(chart.getRemark());
			tempList.add(chart.getDatasource());
			tempList.add(ObjectUtil.defaultIfNull(chart.getX(), "").toString());
			tempList.add(ObjectUtil.defaultIfNull(chart.getY(), "").toString());
			exportDataList.add(tempList);
		}
		return exportDataList;
	}

	public void getPanelIdAndNamePathMapFromTree(VisualPanel panel, String path,
			Map<Integer, String> nodeIdAndNamePathMap) {
		if (CollectionUtils.isEmpty(panel.getChildren())) {
			return;
		}
		for (VisualPanel cPanel : panel.getChildren()) {
			String cPath = "";
			cPath += path + "/" + cPanel.getName();
			nodeIdAndNamePathMap.put(cPanel.getId(), cPath);
			this.getPanelIdAndNamePathMapFromTree(cPanel, cPath, nodeIdAndNamePathMap);
		}
	}

	public List<VisualChart> queryChartsByPanelId(String panelId, String chartId, Map<String, Object> params) {
		params.put("panelId", panelId);
		// 加上原有 ids
		String ids = (String) params.get("ids");
		if (StrUtil.isNotEmpty(ids)) {
			chartId = StrUtil.emptyToDefault(chartId, "");
			chartId += chartId.endsWith(",") ? ids : "," + ids;
		}
		params.put("ids", chartId);
		params.put("returnChildren", "1");
		params.put("groupId", 0);
		PageUtils queryPage = visualChartService.queryPage(params);
		List<VisualChart> result = (List<VisualChart>) queryPage.getList();

		// 递归展开节点信息
		List<VisualChart> allChartList = new ArrayList<>();
		for (VisualChart entity : result) {
			getAllChildrenChartList(entity, allChartList);
		}
		return allChartList;
	}

	/**
	 * 平铺 获取所有 chart 节点
	 *
	 * @param chart
	 * @param chartList
	 */
	public void getAllChildrenChartList(VisualChart chart, List<VisualChart> chartList) {
		chartList.add(chart);
		if (CollectionUtils.isNotEmpty(chart.getChildren())) {
			for (VisualChart pojo : chart.getChildren()) {
				getAllChildrenChartList(pojo, chartList);
			}
		}
	}

	public List<Map<Integer, String>> groupChartRowHandle(List<Map<Integer, String>> importDataList, Integer index) {
		// type标识dashboard还是asset 区别去第几列查找group chart信息
		List<Map<Integer, String>> list = importDataList.stream().filter(dataMap -> {
			String groupName = dataMap.get(index);
			if (StringUtils.isEmpty(groupName)) {
				return true;
			}
			return false;
		}).collect(Collectors.toList());
		importDataList.removeAll(list);
		list.addAll(importDataList);
		return list;
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
