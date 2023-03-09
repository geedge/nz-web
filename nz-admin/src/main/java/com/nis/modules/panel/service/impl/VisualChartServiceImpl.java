package com.nis.modules.panel.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.common.utils.Constant.ChartDatasourceType;
import com.nis.modules.asset.entity.AssetModel;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetModelService;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.module.service.MonitorModuleService;
import com.nis.modules.panel.dao.VisualChartDao;
import com.nis.modules.panel.entity.VisualChart;
import com.nis.modules.panel.entity.VisualChartElement;
import com.nis.modules.panel.entity.VisualPanel;
import com.nis.modules.panel.service.VisualChartElementService;
import com.nis.modules.panel.service.VisualChartService;
import com.nis.modules.panel.service.VisualPanelService;
import com.nis.modules.panel.utils.DashboardConstant;
import com.nis.modules.sys.service.SysConfService;
import com.nis.modules.sys.service.SysDictService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

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
	private VisualPanelService visualPanelService;

	@Autowired
	private VisualChartElementService chartElementService;

	@Autowired
	private AssetAssetService assetService;

	@Autowired
	private AssetModelService assetModelService;

	@Autowired
	private MonitorModuleService monitorModuleService;

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
		//查询elements
        List<Integer> chartIds = result.stream().map(VisualChart::getId).collect(Collectors.toList());
        List<VisualChartElement> chartElements = chartElementService.list(new QueryWrapper<VisualChartElement>().lambda().in(VisualChartElement::getChartId, chartIds));
		Map<Integer, List<VisualChartElement>> chartIdAndElement = chartElements.stream().collect(Collectors.groupingBy(VisualChartElement::getChartId));
        
		String param;
        for (VisualChart chart : result) {
            // param 格式处理
            param = (String) chart.getParam();
            if (StringUtils.isNotEmpty(param)) {
                chart.setParam(JSONObject.parseObject(param, Map.class));
            }
            chart.setElements(chartIdAndElement.get(chart.getId()));
        }
        
        String returnChildren = (String) params.get("returnChildren");
        if(ToolUtil.isEmpty(returnChildren)) {
        	returnChildren = "1" ;
        }
        if(StrUtil.equals(returnChildren, "1")) {
        	List<Integer> fatherIds = result.stream().filter(chart->chart.getGroupId()==0).map(VisualChart::getId).collect(Collectors.toList());
        	if(ToolUtil.isNotEmpty(fatherIds)) {
        		Map<String, Object> queryChildren =new HashMap<String,Object>();
    	        queryChildren.put(Constant.LIMIT, "-1");
            	IPage childrenpage = new Query(VisualChart.class).getPage(queryChildren);
            	queryChildren.put("groupIds", fatherIds);
            	List<VisualChart> childrens = this.baseMapper.queryPage(childrenpage,queryChildren);
            	// 查询chart相关的element
            	if(ToolUtil.isNotEmpty(childrens)) {
            		List<Integer> childIds = childrens.stream().map(VisualChart::getId).collect(Collectors.toList());
            		List<VisualChartElement> childElements = chartElementService.list(new QueryWrapper<VisualChartElement>().lambda().in(VisualChartElement::getChartId, childIds));
            		Map<Integer, List<VisualChartElement>> childIdAndElement = childElements.stream().collect(Collectors.groupingBy(VisualChartElement::getChartId));
            		for (VisualChart chart : childrens) {
                        // param 格式处理
                        param = (String) chart.getParam();
                        if (StringUtils.isNotEmpty(param)) {
                            chart.setParam(JSONObject.parseObject(param, Map.class));
                        }
                        chart.setElements(childIdAndElement.get(chart.getId()));
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
	 * 校验 chart 信息
	 * 1. panel 是否存在
	 * 2. tyep 有效性校验
	 * 3. param 校验
	 *
	 * @param chart
	 */
	@Override
	public void validateChart(VisualChart chart) {
		
		Integer panelId = chart.getPanelId();
		ValidateUtils.is(panelId).notNull(RCode.PANEL_ID_ISNULL);
	    // 1. panel 是否存在
	    this.validatePanel(panelId);
	
	    // 校验vartype
	    if(panelId==0) {
	    	ValidateUtils.is(chart.getVarType()).notNull(RCode.CHART_VARTYPE_ISNULL);
	    }
	    
	    String type = chart.getType();
	    ValidateUtils.is(chart.getName()).notNull(RCode.CHART_TITLE_ISNULL).maxLength(64)
	            .and(chart.getSpan()).notNull(RCode.CHART_SPAN_ISNULL)
	            .and(chart.getHeight()).notNull(RCode.CHART_HEIGHT_ISNULL)
	            .and(type).notNull(RCode.CHART_TYPE_ISNULL)
	            .and(chart.getRemark()).maxLength(256);
	
	    // 2. 验证type是否在枚举范围内
	    sysDictService.checkSysDicValue(Constant.DICT_CHART_TYPE, type, RCode.CHART_TYPE_INVALIDE);
	
	    // 3. 根据 type 校验 各项参数
	    Map param = (Map) chart.getParam();
	    List<VisualChartElement> elements = chart.getElements();
	    switch (type) {
	        // url类型
	        case Constant.CHART_URL_TYPE: {
	            ValidateUtils.is(param).notNull(RCode.CHART_URLPARAM_ISNULL);
	            Object url = param.get(Constant.CHART_URL_TYPE);
	            if (ToolUtil.isEmpty(url)) {
	                throw new NZException(RCode.CHART_URLPARAM_ISNULL);
	            }
	
	            String urlStr = url.toString();
	            // 如果是model panel添加charts url类型支持{{}}替换
	            VisualPanel panel = visualPanelService.getById(panelId);
	            if(ObjectUtil.isNotEmpty(panel) && StrUtil.equals(DashboardConstant.Type.TEMPLATE.getValue(),panel.getType())) {
	                // 替换全部变量 替换为 a（任意字符） 不影响原来的数据格式 然后进行校验
	                urlStr = urlStr.replaceAll("\\{\\{asset\\..*?\\}\\}", "a");
	
	                boolean b = CommonUtils.checkUrlInModel(urlStr);
	                boolean b1 = CommonUtils.checkUrl(urlStr);
	
	                if (!b && !b1) {
	                    throw new NZException(RCode.CHART_PARAMURL_FORMAT);
	                }
	            } else {
	                if (!CommonUtils.checkUrl(urlStr)) {
	                    throw new NZException(RCode.CHART_PARAMURL_FORMAT);
	                }
	            }
	            break;
	        }
	        // stat 类型
	        case Constant.CHART_SINGLESTAT_TYPE: {
	            ValidateUtils.is(param).notNull(RCode.CHART_SINGLESTATPARAM_ISNULL);
	
	            Object statistics = param.get(Constant.CHART_SINGLESTAT_PARAMkEY);
	            if (ToolUtil.isEmpty(statistics)) {
	                throw new NZException(RCode.CHART_PARAMSINGLESTAT_ISNULL);
	            }
	            if (!Constant.CHART_SINGLESTAT_STATISTICS.contains(statistics.toString())) {
	                throw new NZException(RCode.CHART_PARAMSINGLESTAT_INVALIDE);
	            }
	            break;
	        }
	        // group 类型不需要校验 elements
	        case Constant.CHART_GROUP_TYPE:{
	            chart.setElements(null);
	            break;
	        }
	        case Constant.CHART_TEXT_TYPE: {
	            ValidateUtils.is(param).notNull(RCode.CHART_TEXTPARAM_ISNULL);
	            Object text = param.get(Constant.CHART_TEXT_TYPE);
	            if (ToolUtil.isEmpty(text)) throw new NZException(RCode.CHART_TEXTPARAM_ISNULL);
	            break;
	        }
	    }
	
	    
	    // 4. 校验 unit 是否合法
	    Integer unit = chart.getUnit();
	    if (unit != null) {
	        List<Integer> unitList = new ArrayList(Constant.ALERT_RULE_UNIT_MAP.values());
	        if (unit < Collections.min(unitList) || unit > Collections.max(unitList)) {
	            throw new NZException(RCode.CHART_UNIT_NOTFOUND);
	        }
	    }
	    
	    // 5. 校验 groupId 是否有值
	    Integer groupId = chart.getGroupId();
	    if(ObjectUtils.isEmpty(groupId)) {
	    	throw new NZException(RCode.CHART_GROUPID_ISNULL);
	    }
	    
	    // 6. 当前操作人信息赋值
	    Long userId = ShiroUtils.getUserId();
	    chart.setUpdateBy(userId.intValue());
	    
	    // 7. 校验datasource是否为空 
	    if(StrUtil.isBlank(chart.getDatasource())) {
	    	throw new NZException(RCode.CHART_DATASOURCE_ISNULL);
	    }else {
	    	//  校验是否为内置类型
	    	sysDictService.checkSysDicValue(Constant.DICT_CHART_DATASOURCE, chart.getDatasource(), RCode.CHART_DATASOURCE_INVALIDE);
	    }
	    
	    // 8. 校验element参数 仅datasource类型为 logs metrics需要进行校验
		if (chart.getDatasource().equals(Constant.ChartDatasourceType.METRICS.getValue()) || chart.getDatasource().equals(Constant.ChartDatasourceType.LOGS.getValue())) {
            ValidateUtils.is(elements).notNull(RCode.CHART_ELEMENT_ISNULL);
            chartElementService.validateElements(elements);
	    }
	}

	/**
     * 校验 panel 是否存在
     *
     * @param panelId
     */
    public void validatePanel(Integer panelId) {
    	if(panelId.equals(0)) {
    		return;
    	}
        VisualPanel panel = visualPanelService.getById(panelId);
        if (panel == null) {
            throw new NZException(RCode.CHART_PANEL_ISNULL);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
	public Integer saveChart(VisualChart chart) {
    	// 1. 新增 chart 自动追加至末尾
		chart.setName(StrUtil.trim(chart.getName()));
        Object param = chart.getParam();
        if (param != null && param instanceof HashMap) {
            chart.setParam(JSONObject.toJSONString(param));
        }

	    // update info
	    chart.setUpdateAt(Tool.DateUtil.date());
	    chart.setUpdateBy(ShiroUtils.getUserId().intValue());

        // 2. 判断当前 panel 下是否已存在 charts
        List<VisualChart> list = this.list(new QueryWrapper<VisualChart>().lambda().eq(VisualChart::getPanelId, chart.getPanelId()));
        if (CollectionUtils.isEmpty(list)) {
            // 不存在 charts
            this.save(chart);
        } else {
        	VisualChart endChart = this.getOne(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getPanelId, chart.getPanelId()).orderByDesc(VisualChart::getWeight).last("limit 0,1"));
            Integer weight = endChart.getWeight();
            chart.setWeight(++weight);
            this.save(chart);
        }

        // 3. 保存 chart elements信息
        chartElementService.saveOrUpdateElements(chart.getId(), chart.getElements());
        return chart.getId();
	}
    
	@Override
	@Transactional(rollbackFor = Exception.class)
	public Integer updateChart(VisualChart chart) {
		// 1. 校验
		chart.setName(StrUtil.trim(chart.getName()));
        this.validateChart(chart);

        Object param = chart.getParam();
        if (param != null) {
            chart.setParam(JSONObject.toJSONString(param));
        }

		// update info
		chart.setUpdateAt(Tool.DateUtil.date());
		chart.setUpdateBy(ShiroUtils.getUserId().intValue());

        this.updateById(chart);

        // 3. 修改 chart elements
        chartElementService.saveOrUpdateElements(chart.getId(), chart.getElements());
        return chart.getId();
	}
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delCharts(Integer[] ids, Integer sync) {
		
        // 内置chart不能删除
        this.validateBuildInCharts(Arrays.asList(ids));

        List<VisualChart> deleteCharts = this.list(new QueryWrapper<VisualChart>().lambda().in(VisualChart::getId, Arrays.asList(ids)));
        
        List<Integer> deleteIds =new ArrayList<Integer>();
        for(VisualChart deleteChart : deleteCharts) {
        	if(deleteChart.getPanelId().equals(0)) {
        		deleteIds.add(deleteChart.getId());
        	}
        }
        
        if (CollectionUtils.isNotEmpty(deleteIds)) {
            if (sync != null) {
                // 同步删除
                if(sync.equals(1)) {
                    this.syncDeleteAssetsCharts(deleteIds);
                }
            }
        }

		List<Integer> idList = new ArrayList<>(Arrays.asList(ids));

		// 删除 Endpoint modules 引用的 chart templates
		List<MonitorModule> monitorModuleList = monitorModuleService.list(new QueryWrapper<MonitorModule>().lambda().isNotNull(MonitorModule::getChartIds).ne(MonitorModule::getChartIds, ""));
		for (MonitorModule monitorModule : monitorModuleList) {
			monitorModule.setChartIds(this.delIds(monitorModule.getChartIds(), idList));
		}
		if(Tool.CollUtil.isNotEmpty(monitorModuleList))
			monitorModuleService.saveOrUpdateBatch(monitorModuleList);
		// 删除 Asset models 引用的 chart templates
		List<AssetModel> assetModelList = assetModelService.list(new QueryWrapper<AssetModel>().lambda().isNotNull(AssetModel::getChartIds).ne(AssetModel::getChartIds, ""));
		for (AssetModel assetModel : assetModelList) {
			assetModel.setChartIds(this.delIds(assetModel.getChartIds(), idList));
		}
		if(Tool.CollUtil.isNotEmpty(assetModelList))
			assetModelService.saveOrUpdateBatch(assetModelList);

		// 如果删除 ids 中 type 为 group ，则补充 group 包含的 chart ids
		List<VisualChart> childrenCharts = this.list(new LambdaQueryWrapper<VisualChart>().in(VisualChart::getGroupId, Arrays.asList(ids)));
        if(CollectionUtils.isNotEmpty(childrenCharts)){
	        Set<Integer> idSet = childrenCharts.stream().map(VisualChart::getId).collect(Collectors.toSet());
	        idList.addAll(idSet);
        }

		// 删除自身
        this.baseMapper.deleteBatchIds(idList);

        // 删除char element
        chartElementService.delElementByChartIds(idList);
	}

	private String delIds(String chartIds, List<Integer> chartIdList){
		String str = "";
		for (String chartId : chartIds.split(",")) {
			if(!chartIdList.contains(Integer.valueOf(chartId))){
				str += "," + chartId;
			}
		}
		return StrUtil.isBlank(str) ? str : str.substring(1);
	}
	
	@Override
    @Transactional(rollbackFor = Exception.class)
    public void delVisualChartsByPanelId(Integer... panelIds) {
        if (ToolUtil.isEmpty(panelIds)) {
            return;
        }

        // 1. 再次通过 panelid 查询 是为了只删除当前 panel 下的 charts
        List<VisualChart> charts = this.list(new QueryWrapper<VisualChart>().lambda().in(VisualChart::getPanelId, panelIds));
        List<Integer> chartIds = charts.stream().map(VisualChart::getId).collect(Collectors.toList());

        // 2. 内置chart不能删除
        this.validateBuildInCharts(chartIds);

        // 3. 不为空 则删除自身  同时删除关联的 element
        if (CollectionUtils.isNotEmpty(chartIds)) {
            this.removeByIds(chartIds);
            chartElementService.delElementByChartIds(chartIds);
        }
    }
	
	/**
     * 校验是否是内置chart
     *
     * @param chartIds
     */
    private void validateBuildInCharts(List<Integer> chartIds) {
        // 内置charts
        List<VisualChart> buildInCharts = this.list(new QueryWrapper<VisualChart>().lambda().eq(VisualChart::getBuildIn, 1));
        Map<Integer, String> buildInChartIdAndTitleMap = buildInCharts.stream().collect(Collectors.toMap(VisualChart::getId, VisualChart::getName));

        chartIds.forEach(id -> {
            if (ToolUtil.isNotEmpty(buildInChartIdAndTitleMap.get(id))) {
                throw new NZException(buildInChartIdAndTitleMap.get(id) + " chart is built-in and cannot be deleted", RCode.CHART_BUILDIN_CAN_NOT_REMOVE.getCode());
            }
        });
    }
    
    /**
     * 根据 chart.pid 查找引用自此chart，同步删除
     *
     * @param deleteIds
     */
    private void syncDeleteAssetsCharts(List<Integer> deleteIds) {
        // 关联 charts
        List<VisualChart> list = this.list(new QueryWrapper<VisualChart>().lambda().in(VisualChart::getPid, deleteIds));
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        Map<Integer, List<VisualChart>> listMap = list.stream().collect(Collectors.groupingBy(VisualChart::getPanelId));

        List<Integer> allDeleteChartIds = listMap.values().stream().flatMap(Collection::stream).map(VisualChart::getId).collect(Collectors.toList());
        // 删除自身
        this.removeByIds(allDeleteChartIds);
        // 删除关联的 chart Elements
        chartElementService.delElementByChartIds(allDeleteChartIds);
    }

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void modify(VisualChart chart) {

		// 调整大小
        this.baseMapper.updateSizeById(chart);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
    public void weights(Integer panelId,List<VisualChart> chartList) {
       
        if (CollectionUtils.isEmpty(chartList)) throw new NZException(RCode.CHART_WEIGHT_ISNULL);
        for (VisualChart chart : chartList) {
            if (chart.getId() == null) throw new NZException(RCode.CHART_ID_ISNULL);
            if (chart.getWeight() == null) throw new NZException(RCode.CHART_WEIGHT_ISNULL);
        }

        this.baseMapper.updateWeightBatch(panelId,chartList);
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
		// del chart Elements
		chartElementService.delElementByChartIds(removeChartIdList);

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
		Map<VisualPanel, Object> panelAndLinkObjectMap = Tool.MapUtil.newHashMap();
		performSyncChartList.stream().map(VisualChart::getPanelId).distinct().forEach(panelId -> {
			VisualPanel syncChartPanel = visualPanelService.getById(panelId);
			Object linkObject = null;
			if (ObjectUtil.equal(DashboardConstant.VariableType.ASSET.getValue(), syncChartPanel.getVarType())) {
				linkObject = assetService.queryAssetInfo(syncChartPanel.getLink());
			} else if (ObjectUtil.equal(DashboardConstant.VariableType.ENDPOINT.getValue(), syncChartPanel.getVarType())) {
				linkObject = endpointService.queryEndpointEntity(syncChartPanel.getLink());
			}
			panelAndLinkObjectMap.put(syncChartPanel, linkObject);
		});

		// save batch
		List<VisualChartElement> saveChartElementList = Tool.ListUtil.list(true);

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

			// 同步时，var_type, link 值取自 sync panel
			VisualPanel syncChartPanel = panelAndLinkObjectMap.keySet().stream().filter(visualPanel -> ObjectUtil.equal(visualPanel.getId(), syncChart.getPanelId())).findFirst().get();
			Object linkObject = panelAndLinkObjectMap.get(syncChartPanel);

			if (StrUtil.equals(Constant.CHART_GROUP_TYPE, tmplChartForSync.getType())) {
				// group
				VisualChart visualChart = this.performChartSync(tmplChartForSync, syncChartPanel, linkObject, 0);
				groupIdMapping.put(chartId, visualChart.getId());

				saveChartElementList.addAll(Tool.CollUtil.defaultIfEmpty(visualChart.getElements(), Tool.ListUtil.empty()));
			} else {
				// 同步单个图表，group id 依旧为当前 group 下
				VisualChart visualChart = this.performChartSync(tmplChartForSync, syncChartPanel, linkObject, syncChart.getGroupId());
				saveChartElementList.addAll(Tool.CollUtil.defaultIfEmpty(visualChart.getElements(), Tool.ListUtil.empty()));
			}
		}

		// 同步 Group Chart 含有的 Chart
		List<VisualChart> syncGroupChartList = performSyncChartList.stream().filter(chart -> StrUtil.equals(Constant.CHART_GROUP_TYPE, chart.getType())).collect(Collectors.toList());
		for (VisualChart syncGroupChart : syncGroupChartList) {
			Integer syncGroupChartId = syncGroupChart.getId();
			// 直接以 Group 模板内的图表进行渲染，保证同步后 Group 内图表完全一致
			List<VisualChart> chartTemplateListWithinGroup = this.list(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getGroupId, syncGroupChart.getPid()));

			// 同步时，var_type, link 值取自 sync panel
			VisualPanel syncChartPanel = panelAndLinkObjectMap.keySet().stream().filter(visualPanel -> ObjectUtil.equal(visualPanel.getId(), syncGroupChart.getPanelId())).findFirst().get();
			Object linkObject = panelAndLinkObjectMap.get(syncChartPanel);

			for (VisualChart syncChart : chartTemplateListWithinGroup) {
				// find template chart
				VisualChart tmplChartForSync = allChartTemplateList.stream().filter(tmpl -> ObjectUtil.equal(tmpl.getId(), syncChart.getId())).findFirst().orElse(null);
				if (ObjectUtil.isNull(tmplChartForSync)) {
					log.warn("[syncChart] [chart template does not exist] [chart template id: {}] [sync group id: {}]", syncChart.getId(), syncGroupChart.getId());
					tmplChartForSync = this.queryInfo(syncChart.getId());
				}

				// group id is new group chart id
				VisualChart visualChart = this.performChartSync(tmplChartForSync, syncChartPanel, linkObject, groupIdMapping.get(syncGroupChartId));
				saveChartElementList.addAll(Tool.CollUtil.defaultIfEmpty(visualChart.getElements(), Tool.ListUtil.empty()));
			}
		}

		// save element
		if (Tool.CollUtil.isNotEmpty(saveChartElementList)) {
			chartElementService.saveBatch(saveChartElementList);
		}

		log.info("[syncChart] [finshed]");
	}

	/**
	 * perform Chart Sync
	 *
	 * @param tmplChartForSync
	 * @param syncChartPanel
	 * @param linkObject
	 * @param groupId
	 * @return
	 */
	private VisualChart performChartSync(VisualChart tmplChartForSync, VisualPanel syncChartPanel, Object linkObject, Integer groupId) {
		Integer syncPanelId = syncChartPanel.getId();
		Integer syncPanelVarType = syncChartPanel.getVarType();
		Integer syncPanelLink = syncChartPanel.getLink();

		// copy Properties
		VisualChart saveChart = new VisualChart();
		BeanUtil.copyProperties(tmplChartForSync, saveChart);

		saveChart.setId(null);
		saveChart.setPanelId(syncPanelId);
		saveChart.setPid(tmplChartForSync.getId());
		saveChart.setVarId(syncPanelLink);
		saveChart.setVarType(syncPanelVarType);

		saveChart.setUpdateAt(new Date());
		saveChart.setUpdateBy(ShiroUtils.getUserId().intValue());

		saveChart.setName(Tool.NzExpressionUtil.renderExpression(saveChart.getName(), linkObject));
		saveChart.setParam(ToolUtil.isNotEmpty(saveChart.getParam()) ? Tool.NzExpressionUtil.renderExpression(saveChart.getParam().toString(), linkObject) : null);

		// group id
		saveChart.setGroupId(ObjectUtil.defaultIfNull(groupId, 0));

		// save chart
		this.save(saveChart);

		// element
		if (Tool.CollUtil.isNotEmpty(saveChart.getElements())) {
			List<VisualChartElement> saveElementList = Tool.ListUtil.list(true);
			for (VisualChartElement element : saveChart.getElements()) {
				VisualChartElement saveElement = new VisualChartElement();
				BeanUtil.copyProperties(element, saveElement);

				saveElement.setId(null);
				saveElement.setExpression(Tool.NzExpressionUtil.renderExpression(element.getExpression(), linkObject));
				saveElement.setChartId(saveChart.getId());
				saveElementList.add(saveElement);
			}
			saveChart.setElements(saveElementList);
		}
		return saveChart;
	}

    @Override
    public void saveRelPanelAndChart(String type, Integer linkId, String name, Integer tempId, String seq) {
        VisualPanel panel = new VisualPanel();
        panel.setName(name);
        panel.setType(type);
        panel.setPid(0);
        panel.setLink(linkId);
        // temp_id
        // assetInfo dashboard from model.panel_id
        // endpointInfo dashboard from module.panel_id
        panel.setTmplId(ObjectUtil.defaultIfNull(tempId, -1));
        panel.setWeight(0);
        panel.setCreateBy(ShiroUtils.getUserId().intValue());
        panel.setUts(System.currentTimeMillis());
        if (StrUtil.equals(DashboardConstant.Type.ASSET.getValue(), type)) {
            panel.setVarType(DashboardConstant.VariableType.ASSET.getValue());
        } else if (StrUtil.equals(DashboardConstant.Type.ENDPOINT.getValue(), type)) {
            panel.setVarType(DashboardConstant.VariableType.ENDPOINT.getValue());
        }
        if (StrUtil.isNotEmpty(seq)) {
            panel.setSeq(seq);
        }
        visualPanelService.save(panel);

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
        target.setPanelId(panel.getId());
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

	@Override
	public List<VisualChart> queryChartsIncludeElementInfo(Integer panelId) {
		List<VisualChart> visualChartList = this.getBaseMapper().queryChartsIncludeElementInfo(panelId);
		return Tool.CollUtil.defaultIfEmpty(visualChartList, Tool.ListUtil.empty());
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
