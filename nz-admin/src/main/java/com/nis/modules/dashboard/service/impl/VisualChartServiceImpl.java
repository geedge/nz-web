package com.nis.modules.dashboard.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.common.utils.Constant.ChartDatasourceType;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.dashboard.dao.VisualChartDao;
import com.nis.modules.dashboard.entity.VisualChart;
import com.nis.modules.dashboard.entity.VisualDashboard;
import com.nis.modules.dashboard.service.VisualChartService;
import com.nis.modules.dashboard.service.VisualDashboardService;
import com.nis.modules.dashboard.utils.DashboardConstant;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.service.SysConfService;
import com.nis.modules.sys.service.SysDictService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service("visualChartService")
public class VisualChartServiceImpl extends ServiceImpl<VisualChartDao, VisualChart> implements VisualChartService {

	private static final Log log = Log.get();

	@Autowired
	private SysDictService sysDictService;

	@Autowired
    private SysConfService sysConfService;

	@Autowired
	private VisualDashboardService dashboardService;

	@Autowired
	private AssetAssetService assetService;

	@Autowired
	private MonitorEndpointService endpointService;

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		String ids = (String) params.get("ids");
        if(StrUtil.isNotBlank(ids)) {
        	List<String> listIds = Arrays.asList(ids.split(","));
        	params.put("ids",listIds);
        }
        String pid = (String) params.get("pids");
        if(StrUtil.isNotBlank(pid)) {
        	List<String> listIds = Arrays.asList(pid.split(","));
        	params.put("pids",listIds);
        }
		IPage page = new Query(VisualChart.class).getPage(params);
		// 查询出chart信息
		List<VisualChart> result = this.baseMapper.queryPage(page,params);
        for (VisualChart chart : result) {
            // param 格式处理
	        String param = (String) chart.getParam();
            if (StringUtils.isNotEmpty(param)) {
                chart.setParam(JSONObject.parseObject(param, Map.class));
            }
        }

		String returnChildren = (String) params.get("returnChildren");
		if (StrUtil.equals(StrUtil.emptyToDefault(returnChildren, "1"), "1")) {
        	List<Integer> fatherIds = result.stream().filter(chart->chart.getGroupId()==0).map(VisualChart::getId).collect(Collectors.toList());
        	if(ToolUtil.isNotEmpty(fatherIds)) {
		        Map<String, Object> queryChildren = new HashMap();
    	        queryChildren.put(Constant.LIMIT, "-1");
            	IPage childrenpage = new Query(VisualChart.class).getPage(queryChildren);
            	queryChildren.put("groupIds", fatherIds);
            	List<VisualChart> childrens = this.baseMapper.queryPage(childrenpage,queryChildren);
            	if(ToolUtil.isNotEmpty(childrens)) {
            		for (VisualChart chart : childrens) {
                        // param 格式处理
			            String param = (String) chart.getParam();
                        if (StringUtils.isNotEmpty(param)) {
                            chart.setParam(JSONObject.parseObject(param, Map.class));
                        }
                    }
                	result.addAll(childrens);
                	result = this.getChartTreeList(result,0);
            	}
        	}
        }
        page.setRecords(result);
		return new PageUtils(page);
	}

	/**
     * 递归获取树结构
     *
     * @param chartList
     * @param parentId
     * @return
     */
    private List<VisualChart> getChartTreeList(List<VisualChart> chartList, Integer parentId) {
        List<VisualChart> resultList = new ArrayList<>();
        for (VisualChart chart : chartList) {
            Integer id = chart.getId();
            Integer groupId = chart.getGroupId();
            if(parentId.equals(groupId)) {
            	chart.setChildren(this.getChartTreeList(chartList, id));
                resultList.add(chart);
            }
        }
        return resultList;
    }
	
	
	@Override
	public VisualChart queryInfo(Integer id) {
		return this.baseMapper.queryInfo(id);
	}

	/**
	 * validate Chart
	 *
	 * @param chart
	 */
	public void validateChartInfo(VisualChart chart) {
		ValidateUtils.is(chart.getDashboardId()).notNull(RCode.DASHBOARD_ID_ISNULL)
				.and(chart.getName()).notNull(RCode.CHART_TITLE_ISNULL).maxLength(64)
				.and(chart.getSpan()).notNull(RCode.CHART_SPAN_ISNULL)
				.and(chart.getHeight()).notNull(RCode.CHART_HEIGHT_ISNULL)
				.and(chart.getType()).notNull(RCode.CHART_TYPE_ISNULL)
				.and(chart.getGroupId()).notNull(RCode.CHART_GROUPID_ISNULL)
				.and(chart.getDatasource()).notNull(RCode.CHART_DATASOURCE_ISNULL)
				.and(chart.getRemark()).maxLength(256);

		// dashboard
		VisualDashboard dashboard = dashboardService.getById(chart.getDashboardId());
		ValidateUtils.is(dashboard).notNull(RCode.DASHBOARD_NOT_EXIST);

		// unit
		Integer unit = chart.getUnit();
		if (unit != null) {
			List<Integer> unitList = new ArrayList(Constant.ALERT_RULE_UNIT_MAP.values());
			if (unit < Collections.min(unitList) || unit > Collections.max(unitList)) {
				throw new NZException(RCode.CHART_UNIT_NOTFOUND);
			}
		}

		// type
		sysDictService.checkSysDicValue(Constant.DICT_CHART_TYPE, chart.getType(), RCode.CHART_TYPE_INVALIDE);

		// datasource
		sysDictService.checkSysDicValue(Constant.DICT_CHART_DATASOURCE, chart.getDatasource(), RCode.CHART_DATASOURCE_INVALIDE);

		// element, validate element when datasource is metrics or logs
		if (StrUtil.equalsAny(chart.getDatasource(), Constant.ChartDatasourceType.METRICS.getValue(), Constant.ChartDatasourceType.LOGS.getValue())) {
			List<Map> elements = chart.getElements();
			ValidateUtils.is(elements).notNull(RCode.CHART_ELEMENT_ISNULL);

			List<SysDictEntity> exprTypeList = sysDictService.querySysDicByType(Constant.DICT_EXPRESSION_TYPE);
			List<String> exprTypeValueList = exprTypeList.stream().map(SysDictEntity::getValue).collect(Collectors.toList());

			for (Map element : elements) {
				ValidateUtils.is(Tool.MapUtil.getStr(element, "expression", "")).notNull(RCode.CHARTELEMENT_EXPRESSION_ISNULL).maxLength(4096)
						.and(Tool.MapUtil.getStr(element, "type", "")).notNull(RCode.CHARTELEMENT_TYPE_ISNULL)
						.and(Tool.MapUtil.getStr(element, "legend", "")).maxLength(512)
						.and(Tool.MapUtil.getStr(element, "name", "")).notNull(RCode.CHARTELEMENT_NAME_ISNULL).maxLength(64);

				if (!exprTypeValueList.contains(Tool.MapUtil.getStr(element, "type", ""))) {
					throw new NZException(RCode.CHARTELEMENT_TYPE_INVALIDE);
				}
			}
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Integer saveChart(VisualChart chart) {
		// validate chart info
		this.validateChartInfo(chart);

		Object param = chart.getParam();
		if (param != null) {
			chart.setParam(JSONObject.toJSONString(param));
		}

		// update info
		chart.setUpdateAt(Tool.DateUtil.date());
		chart.setUpdateBy(ShiroUtils.getUserId().intValue());

		// position
		VisualChart endChart = this.getOne(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getDashboardId, chart.getDashboardId()).orderByDesc(VisualChart::getWeight).last("limit 0,1"));
		if (ObjectUtil.isNotNull(endChart)) {
			Integer weight = endChart.getWeight();
			chart.setWeight(++weight);
		}

		// element
		List<Map> elements = chart.getElements();
		chart.setElementArr(JSONUtil.toJsonStr(elements));

		// save
		this.save(chart);
		return chart.getId();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Integer updateChart(VisualChart chart) {
		// validate chart info
		this.validateChartInfo(chart);

		Object param = chart.getParam();
		if (param != null) {
			chart.setParam(JSONObject.toJSONString(param));
		}

		// update info
		chart.setUpdateAt(Tool.DateUtil.date());
		chart.setUpdateBy(ShiroUtils.getUserId().intValue());

		// element
		List<Map> elements = chart.getElements();
		chart.setElementArr(JSONUtil.toJsonStr(elements));

		// update
		this.updateById(chart);
		return chart.getId();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delCharts(List<Integer> idList, Integer sync) {
		List<VisualChart> childrenCharts = this.list(new LambdaQueryWrapper<VisualChart>().in(VisualChart::getGroupId, idList));
		if (CollectionUtils.isNotEmpty(childrenCharts)) {
			Set<Integer> idSet = childrenCharts.stream().map(VisualChart::getId).collect(Collectors.toSet());
			idList.addAll(idSet);
		}

		// sync = 1, 同步删除
		sync = ObjectUtil.defaultIfNull(sync, 0);
		if (sync == 1) {
			// delete referenced chart data
			List<VisualChart> referencedChart = this.list(new LambdaQueryWrapper<VisualChart>().in(VisualChart::getPid, idList));
			if (CollUtil.isNotEmpty(referencedChart)) {
				List<Integer> referencedChartIdList = referencedChart.stream().map(VisualChart::getId).collect(Collectors.toList());
				this.baseMapper.deleteBatchIds(referencedChartIdList);
			}
		} else {
			// update Referenced Chart Data pid=-1
			this.update(new LambdaUpdateWrapper<VisualChart>()
					.set(VisualChart::getPid, -1)
					.in(VisualChart::getPid, idList));
		}

		// delete chart
		this.baseMapper.deleteBatchIds(idList);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void modify(VisualChart chart) {

		// 调整大小
        this.baseMapper.updateSizeById(chart);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
    public void weights(Integer dashboardId,List<VisualChart> chartList) {
       
        if (CollectionUtils.isEmpty(chartList)) throw new NZException(RCode.CHART_WEIGHT_ISNULL);
        for (VisualChart chart : chartList) {
            if (chart.getId() == null) throw new NZException(RCode.CHART_ID_ISNULL);
            if (chart.getWeight() == null) throw new NZException(RCode.CHART_WEIGHT_ISNULL);
        }

        this.baseMapper.updateWeightBatch(dashboardId,chartList);
    }

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Integer chartTmplSynch(Integer pid, List<Integer> chartIdList) {
		log.info("[chartTmplSynch] [begin]");

		// get All Charts Need Be Synch By Params
		List<VisualChart> chartListToBeSync = this.getAllChartsNeedBeSynchByParams(pid, chartIdList);

		if (Tool.CollUtil.isEmpty(chartListToBeSync)) {
			log.warn("[chartTmplSynch] [No charts found need to be synchronized] [pid: {}] [chartIdList: {}]", pid, Tool.JSONUtil.toJsonStr(chartIdList));
			return chartListToBeSync.size();
		}

		// 获取所有模板数据
		List<Integer> allTmplChartIdList = chartListToBeSync.stream().map(VisualChart::getPid).distinct().collect(Collectors.toList());
		List<VisualChart> chartTemplateWithinGroup = this.list(new LambdaQueryWrapper<VisualChart>().in(VisualChart::getGroupId, allTmplChartIdList));
		if (Tool.CollUtil.isNotEmpty(chartTemplateWithinGroup)) {
			List<Integer> chartTemplateIdListWithinGroup = chartTemplateWithinGroup.stream().map(VisualChart::getId).collect(Collectors.toList());
			allTmplChartIdList.addAll(chartTemplateIdListWithinGroup);
			allTmplChartIdList = allTmplChartIdList.stream().distinct().collect(Collectors.toList());
		}
		List<VisualChart> allChartTemplateList = this.baseMapper.queryList(allTmplChartIdList);

		// 将不必要的图表过滤掉，避免重复同步
		// Group A {Chart-1,Chart-2,Chart-3}  只需要同步  Group A  即可
		List<Integer> syncChartGroupIdList = chartListToBeSync.stream().filter(chart -> StrUtil.equals(Constant.CHART_GROUP_TYPE, chart.getType())).map(VisualChart::getId).collect(Collectors.toList());

		// perform Sync Chart List
		List<VisualChart> performSyncChartList = chartListToBeSync.stream().filter(chart -> BooleanUtil.negate(Tool.CollUtil.contains(syncChartGroupIdList, chart.getGroupId()))).collect(Collectors.toList());
		this.syncChart(performSyncChartList, allChartTemplateList);

		// delete
		List<Integer> removeChartIdList = chartListToBeSync.stream().map(VisualChart::getId).collect(Collectors.toList());
		// del charts
		this.removeByIds(removeChartIdList);

		log.info("[chartTmplSynch] [finshed]");
		// total
		return removeChartIdList.size();
	}

	/**
	 * get All Charts Need Be Synch By Params
	 *
	 * @param pid
	 * @param chartIdList
	 * @return
	 */
	private List<VisualChart> getAllChartsNeedBeSynchByParams(Integer pid, List<Integer> chartIdList) {
		log.info("[getAllChartsNeedBeSynchByParams] [pid: {}] [chartIdList: {}]", pid, Tool.JSONUtil.toJsonStr(chartIdList));

		List<VisualChart> chartListToBeSync = Tool.ListUtil.list(true);
		if (ObjectUtil.isNotNull(pid)) {
			List<VisualChart> list = this.list(new LambdaQueryWrapper<VisualChart>().isNotNull(VisualChart::getPid).eq(VisualChart::getPid, pid));
			chartListToBeSync.addAll(list);
			if (Tool.CollUtil.isNotEmpty(list)) {
				List<Integer> tempChartIdList = list.stream().map(VisualChart::getId).collect(Collectors.toList());
				List<VisualChart> chaildChartList = this.list(new LambdaQueryWrapper<VisualChart>().isNotNull(VisualChart::getPid).in(VisualChart::getGroupId, tempChartIdList));
				chartListToBeSync.addAll(chaildChartList);
			}
		}

		if (Tool.CollUtil.isNotEmpty(chartIdList)) {
			List<VisualChart> list = this.list(new LambdaQueryWrapper<VisualChart>().isNotNull(VisualChart::getPid).in(VisualChart::getId, chartIdList));
			List<VisualChart> chaildChartList = this.list(new LambdaQueryWrapper<VisualChart>().isNotNull(VisualChart::getPid).in(VisualChart::getGroupId, chartIdList));
			chartListToBeSync.addAll(list);
			chartListToBeSync.addAll(chaildChartList);
		}
		// distinct data
		chartListToBeSync = chartListToBeSync.stream().distinct().collect(Collectors.toList());

		log.info("[getAllChartsNeedBeSynchByParams] [sync chart list] [size: {}]", chartListToBeSync.size());
		return chartListToBeSync;
	}

	/**
	 * sync Chart
	 *
	 * @param performSyncChartList
	 * @param allChartTemplateList
	 */
	private void syncChart(List<VisualChart> performSyncChartList, List<VisualChart> allChartTemplateList) {
		log.info("[syncChart] [begin] [perform Sync Chart List size: {}]", performSyncChartList.size());
		// 详情接口查询效率低，避免渲染时多次查询影响效率
		Map<VisualDashboard, Object> dashboardAndLinkObjectMap = Tool.MapUtil.newHashMap();
		performSyncChartList.stream().map(VisualChart::getDashboardId).distinct().forEach(dashboardId -> {
			VisualDashboard syncChartDashboard = dashboardService.getById(dashboardId);
			Object linkObject = null;
			if (ObjectUtil.equal(DashboardConstant.VariableType.ASSET.getValue(), syncChartDashboard.getVarType())) {
				linkObject = assetService.queryAssetInfo(syncChartDashboard.getLink());
			} else if (ObjectUtil.equal(DashboardConstant.VariableType.ENDPOINT.getValue(), syncChartDashboard.getVarType())) {
				linkObject = endpointService.queryEndpointEntity(syncChartDashboard.getLink());
			}
			dashboardAndLinkObjectMap.put(syncChartDashboard, linkObject);
		});

		Map<Integer, Integer> groupIdMapping = Tool.MapUtil.newHashMap();
		// 先同步独立的 Chart 和 独立的 Group Chart(不包含 Group 包含的图表)
		for (VisualChart syncChart : performSyncChartList) {
			Integer chartId = syncChart.getId();
			// find template chart
			VisualChart tmplChartForSync = allChartTemplateList.stream().filter(tmpl -> ObjectUtil.equal(tmpl.getId(), syncChart.getPid())).findFirst().orElse(null);
			if (ObjectUtil.isNull(tmplChartForSync)) {
				log.warn("[syncChart] [chart template does not exist] [sync chart id: {}] [pid: {}]", chartId, syncChart.getPid());
				tmplChartForSync = this.queryInfo(syncChart.getPid());
			}

			// 同步时，var_type, link 值取自 sync Dashboard
			VisualDashboard syncChartDashboard = dashboardAndLinkObjectMap.keySet().stream().filter(visualDashboard -> ObjectUtil.equal(visualDashboard.getId(), syncChart.getDashboardId())).findFirst().get();
			Object linkObject = dashboardAndLinkObjectMap.get(syncChartDashboard);

			if (StrUtil.equals(Constant.CHART_GROUP_TYPE, tmplChartForSync.getType())) {
				// group
				VisualChart visualChart = this.performChartSync(tmplChartForSync, syncChartDashboard, linkObject, 0);
				groupIdMapping.put(chartId, visualChart.getId());
			} else {
				// 同步单个图表，group id 依旧为当前 group 下
				this.performChartSync(tmplChartForSync, syncChartDashboard, linkObject, syncChart.getGroupId());
			}
		}

		// 同步 Group Chart 含有的 Chart
		List<VisualChart> syncGroupChartList = performSyncChartList.stream().filter(chart -> StrUtil.equals(Constant.CHART_GROUP_TYPE, chart.getType())).collect(Collectors.toList());
		for (VisualChart syncGroupChart : syncGroupChartList) {
			Integer syncGroupChartId = syncGroupChart.getId();
			// 直接以 Group 模板内的图表进行渲染，保证同步后 Group 内图表完全一致
			List<VisualChart> chartTemplateListWithinGroup = this.list(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getGroupId, syncGroupChart.getPid()));

			// 同步时，var_type, link 值取自 sync dashboard
			VisualDashboard syncChartDashboard = dashboardAndLinkObjectMap.keySet().stream().filter(visualDashboard -> ObjectUtil.equal(visualDashboard.getId(), syncGroupChart.getDashboardId())).findFirst().get();
			Object linkObject = dashboardAndLinkObjectMap.get(syncChartDashboard);

			for (VisualChart syncChart : chartTemplateListWithinGroup) {
				// find template chart
				VisualChart tmplChartForSync = allChartTemplateList.stream().filter(tmpl -> ObjectUtil.equal(tmpl.getId(), syncChart.getId())).findFirst().orElse(null);
				if (ObjectUtil.isNull(tmplChartForSync)) {
					log.warn("[syncChart] [chart template does not exist] [chart template id: {}] [sync group id: {}]", syncChart.getId(), syncGroupChart.getId());
					tmplChartForSync = this.queryInfo(syncChart.getId());
				}

				// group id is new group chart id
				this.performChartSync(tmplChartForSync, syncChartDashboard, linkObject, groupIdMapping.get(syncGroupChartId));
			}
		}
		log.info("[syncChart] [finshed]");
	}

	/**
	 * perform Chart Sync
	 *
	 * @param tmplChartForSync
	 * @param syncChartDashboard
	 * @param linkObject
	 * @param groupId
	 * @return
	 */
	private VisualChart performChartSync(VisualChart tmplChartForSync, VisualDashboard syncChartDashboard, Object linkObject, Integer groupId) {
		Integer syncDashboardId = syncChartDashboard.getId();
		Integer syncDashboardVarType = syncChartDashboard.getVarType();
		Integer syncDashboardLink = syncChartDashboard.getLink();

		// copy Properties
		VisualChart saveChart = JSONUtil.toBean(JSONUtil.toJsonStr(tmplChartForSync), VisualChart.class);

		saveChart.setId(null);
		saveChart.setDashboardId(syncDashboardId);
		saveChart.setPid(tmplChartForSync.getId());
		saveChart.setVarId(syncDashboardLink);
		saveChart.setVarType(syncDashboardVarType);

		saveChart.setUpdateAt(new Date());
		saveChart.setUpdateBy(ShiroUtils.getUserId().intValue());

		saveChart.setName(Tool.NzExpressionUtil.renderExpression(saveChart.getName(), linkObject));
		saveChart.setParam(ToolUtil.isNotEmpty(saveChart.getParam()) ? Tool.NzExpressionUtil.renderExpression(saveChart.getParam().toString(), linkObject) : null);

		// group id
		saveChart.setGroupId(ObjectUtil.defaultIfNull(groupId, 0));

		// element_arr
		if (Tool.CollUtil.isNotEmpty(saveChart.getElements())) {
			for (Map element : saveChart.getElements()) {
				element.put("expression", Tool.NzExpressionUtil.renderExpression(Tool.MapUtil.getStr(element, "expression", ""), linkObject));
			}
			saveChart.setElementArr(JSONUtil.toJsonStr(saveChart.getElements()));
		}

		// save chart
		this.save(saveChart);
		return saveChart;
	}

    @Override
    public void saveRelDashboardAndChart(String type, Integer linkId, String name, Integer tempId, String seq) {
        VisualDashboard dashboard = new VisualDashboard();
        dashboard.setName(name);
        dashboard.setType(type);
        dashboard.setPid(0);
        dashboard.setLink(linkId);
        // temp_id
        // assetInfo dashboard from model.dashboard_id
        // endpointInfo dashboard from module.dashboard_id
        dashboard.setTmplId(ObjectUtil.defaultIfNull(tempId, -1));
        dashboard.setWeight(0);
        dashboard.setCreateBy(ShiroUtils.getUserId().intValue());
        dashboard.setUts(System.currentTimeMillis());
        if (StrUtil.equals(DashboardConstant.Type.ASSET.getValue(), type)) {
            dashboard.setVarType(DashboardConstant.VariableType.ASSET.getValue());
        } else if (StrUtil.equals(DashboardConstant.Type.ENDPOINT.getValue(), type)) {
            dashboard.setVarType(DashboardConstant.VariableType.ENDPOINT.getValue());
        }
        if (StrUtil.isNotEmpty(seq)) {
            dashboard.setSeq(seq);
        }
        dashboardService.save(dashboard);

        // build-in chart include assetInfo and endpointInfo chart
        String chartConfigTempJsonStr = StrUtil.EMPTY;
        DashboardConstant.Type instanceByType = DashboardConstant.Type.getInstanceByType(type);
        switch (instanceByType) {
            case ASSET: {
                chartConfigTempJsonStr = sysConfService.getValue(Constant.SYSCONFIG_KEY_ASSET_CHART_TPL);
                break;
            }
            case ENDPOINT: {
                chartConfigTempJsonStr = sysConfService.getValue(Constant.SYSCONFIG_KEY_ENDPOINT_CHART_TPL);
                break;
            }
            default:
                break;
        }

        // assetInfo or endpointInfo
        VisualChart chartTemplate = JSONObject.parseObject(chartConfigTempJsonStr, VisualChart.class);

        VisualChart target = new VisualChart();
        BeanUtils.copyProperties(chartTemplate, target);
        target.setParam(ToolUtil.isNotEmpty(target.getParam()) ? JSONObject.toJSONString(target.getParam()) : null);
        target.setDashboardId(dashboard.getId());
        target.setGroupId(0);
        target.setDatasource(ChartDatasourceType.SYSTEM.getValue());
        target.setUpdateAt(new Date());
        target.setUpdateBy(ShiroUtils.getUserId().intValue());
        if (StrUtil.isNotEmpty(seq)) {
            target.setSeq(seq);
        }
        this.save(target);
    }

	@Override
	public List<VisualChart> getAllChartList(List<VisualChart> sourceChartList) {
		sourceChartList = Tool.CollUtil.defaultIfEmpty(sourceChartList, Tool.ListUtil.empty());

		// 递归展开节点信息
		List<VisualChart> allChartList = Tool.ListUtil.list(true);
		for (VisualChart entity : sourceChartList) {
			this.getAllChildrenChartList(entity, allChartList);
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
}
