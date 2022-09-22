package com.nis.modules.panel.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.CommonUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Constant.ChartDatasourceType;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.common.utils.StringUtils;
import com.nis.common.utils.Tool;
import com.nis.common.utils.Tool.NzExpressionUtil;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetModel;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetModelService;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
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
import com.nis.modules.sys.service.SysDictService;
import com.nis.modules.sys.shiro.ShiroUtils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;

@Service("visualChartService")
public class VisualChartServiceImpl extends ServiceImpl<VisualChartDao, VisualChart> implements VisualChartService {
	
	private Log log = Log.get();
	
	@Autowired
	private VisualPanelService visualPanelService;
	
	@Autowired
	private SysDictService sysDictService;
	
	@Autowired
	private VisualChartElementService chartElementService;
	
	@Autowired
	private AssetAssetService assetAssetService;
	
	@Autowired
	private MonitorEndpointService monitorEndpointService;
	
	@Autowired
	private AssetModelService assetModelService;
	
	@Autowired
	private MonitorModuleService monitorModuleService;

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
	            if(ObjectUtil.isNotEmpty(panel) && StrUtil.equals(Constant.PanelType.MODEL.getValue(),panel.getType())) {
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
	        	// 当chart为group时 不允许name重复  会影响chart批量导入
	        	List<VisualChart> charts = this.list(new QueryWrapper<VisualChart>().lambda().eq(VisualChart::getName, chart.getName()).eq(VisualChart::getType, Constant.CHART_GROUP_TYPE.toString()).eq(VisualChart::getPanelId, chart.getPanelId()).ne(ToolUtil.isNotEmpty(chart.getId()), VisualChart::getId,chart.getId()).ne(ToolUtil.isNotEmpty(chart.getVarType()), VisualChart::getVarType,chart.getVarType()));
	            if(ToolUtil.isNotEmpty(charts)) {
	            	throw new NZException(RCode.CHART_GROUP_NAME_DUPLICATE);
	            }
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
        chart.setUpdateAt(new Date());
        Object param = chart.getParam();
        if (param != null && param instanceof HashMap) {
            chart.setParam(JSONObject.toJSONString(param));
        }

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

		/*// 4. 同步新增处理
		Integer sync = chart.getSync();
		if (sync != null) {
		    VisualPanel panel = visualPanelService.getById(chart.getPanelId());
		    // 当前 chart 所属 panel 为 model 类型并且 同步标识 为 1 时 ，对已通过对应 model panel 创建的 asset panel 追加新增的 chart
		    if(StrUtil.equals(Constant.PanelType.MODEL.getValue(),panel.getType()) && sync.equals(1)) {
		        this.saveAssetChartsSimultaneously(panel, chart);
		    }
		}*/
        
        return chart.getId();
	}
    
    /**
     * 新增 model 时对已通过对应 model panel 创建的 asset panel 追加新增的 chart
     *
     * @param modelPanel
     * @param chart
     */
    private void saveAssetChartsSimultaneously(VisualPanel modelPanel, VisualChart chart) {
        // 1. 通过 panel  linke 的 modelId 查找该 model 下是否关联 asset
        List<AssetAsset> assets = assetAssetService.list(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getModelId, modelPanel.getLink()));
        if (CollectionUtils.isEmpty(assets)) {
            return;
        }

        // 2. 通过 assets 找到关联的所有 asset panel
        List<Integer> assetsIds = assets.stream().map(AssetAsset::getId).collect(Collectors.toList());
        List<VisualPanel> panels = visualPanelService.list(new QueryWrapper<VisualPanel>().lambda().eq(VisualPanel::getType, Constant.PanelType.ASSET.getValue()).in(VisualPanel::getLink, assetsIds));

        Map<Integer, AssetAsset> assetIdAndEntityMap = new HashMap<>(16);
        for (AssetAsset asset : assets) {
            assetIdAndEntityMap.put(asset.getId(), asset);
        }

        // 3. 得到 panelId 和 关联的 asset 实体 ，准备做 变量替换所需
        Map<Integer, AssetAsset> panelIdAndAsset = new HashMap<>();
        for (VisualPanel entity : panels) {
            panelIdAndAsset.put(entity.getId(), assetIdAndEntityMap.get(entity.getLink()));
        }

        // 4. 处理属性
        VisualChart targetChart = new VisualChart();
        AssetAsset asset;
        for (Map.Entry<Integer, AssetAsset> entry : panelIdAndAsset.entrySet()) {
            asset = entry.getValue();
            targetChart = JSONObject.parseObject(JSON.toJSONString(chart), VisualChart.class);
            targetChart.setSync(null);
            targetChart.setId(null);
            targetChart.setPid(chart.getId());

            // chart ： title param   chart element : expression   属性需要做 asset 变量替换
            targetChart.setName(Tool.NzExpressionUtil.renderExpression(targetChart.getName(),asset));
            targetChart.setParam(ToolUtil.isNotEmpty(targetChart.getParam()) ? Tool.NzExpressionUtil.renderExpression(targetChart.getParam().toString(), asset) : null);

            if (CollectionUtils.isNotEmpty(targetChart.getElements())) {
                for (VisualChartElement element : targetChart.getElements()) {
                    element.setId(null);
                    element.setExpression(ToolUtil.isNotEmpty(element.getExpression()) ? Tool.NzExpressionUtil.renderExpression(element.getExpression(), asset) : null);
                }
            }

            // 5. 调用保存方法进行 新增
            this.saveChart(targetChart);
        }
    }

    
	@Override
	@Transactional(rollbackFor = Exception.class)
	public Integer updateChart(VisualChart chart) {
		// 1. 校验
        this.validateChart(chart);

        Object param = chart.getParam();
        if (param != null) {
            chart.setParam(JSONObject.toJSONString(param));
        }

        this.updateById(chart);

        // 3. 修改 chart elements
        chartElementService.saveOrUpdateElements(chart.getId(), chart.getElements());

        // 4. 同步修改处理
        Integer sync = chart.getSync();
        if (sync != null) {
            VisualPanel panel = visualPanelService.getById(chart.getPanelId());
            // 当前 chart 所属 panel 为 model 类型并且 同步标识 为 1 时 ，对根据 chart pid 查找通过此 chart 创建的 chart 更新
            if(StrUtil.equals(Constant.PanelType.MODEL.getValue(),panel.getType()) && sync.equals(1)) {
                this.updateAssetChartsSimultaneously(chart);
            }
        }
        
        return chart.getId();
	}
	
	/**
     * 对根据 chart pid 查找通过此 chart 创建的 chart 进行更新
     *
     * @param chart
     */
    private void updateAssetChartsSimultaneously(VisualChart chart) {
        // 1. 查找引用的 charts
        List<VisualChart> charts = this.list(new QueryWrapper<VisualChart>().lambda().eq(VisualChart::getPid, chart.getId()));
        if (CollectionUtils.isEmpty(charts)) {
            return;
        }

        List<Integer> idList = charts.stream().map(VisualChart::getId).collect(Collectors.toList());

        // 2. 先删除之前的 elements  使用当前 chart element 进行更新
        chartElementService.delElementByChartIds(idList);

        // 3. 返回引用 chartId 和 对应的 asset 信息 ，准备做 变量替换所需
        List<VisualChart> chartIdWithAssetEntity = assetAssetService.getAssetsWithTagByChartIds(idList);
        Map<Integer, AssetAsset> chartIdAndAssetEntityMap = chartIdWithAssetEntity.stream().collect(Collectors.toMap(VisualChart::getId, VisualChart::getAsset));

        // 4. 处理更新
        List<VisualChartElement> elements = chart.getElements();
        List<VisualChartElement> saveElements = new ArrayList<>();
        VisualChartElement tempElement;
        AssetAsset asset;
        for (VisualChart entity : charts) {
            asset = chartIdAndAssetEntityMap.get(entity.getId());
            entity.setName(Tool.NzExpressionUtil.renderExpression(chart.getName(), asset));
            entity.setSpan(chart.getSpan());
            entity.setHeight(chart.getHeight());
            entity.setType(chart.getType());
            entity.setUnit(chart.getUnit());
            entity.setParam(ToolUtil.isNotEmpty(chart.getParam()) ? Tool.NzExpressionUtil.renderExpression(chart.getParam().toString(), asset) : null);
            if (CollectionUtils.isNotEmpty(elements)) {
                for (VisualChartElement chartElement : elements) {
                    tempElement = new VisualChartElement();
                    BeanUtils.copyProperties(chartElement, tempElement);
                    tempElement.setChartId(entity.getId());
                    tempElement.setId(null);
                    tempElement.setExpression(ToolUtil.isNotEmpty(chartElement.getExpression()) ? Tool.NzExpressionUtil.renderExpression(chartElement.getExpression(), asset) : null);
                    saveElements.add(tempElement);
                }
            }
        }

        this.saveOrUpdateBatch(charts);
        chartElementService.saveBatch(saveElements);
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
	public Map<Integer,Integer> queryChartNum() {
		Map<Integer, Integer> collect = new HashMap<Integer, Integer>();
		List<VisualChart> result = this.baseMapper.selectChartNum();
		if(!ObjectUtils.isEmpty(result)) {
			collect = result.stream().collect(Collectors.toMap(VisualChart::getId, VisualChart::getChartNums));
		}
		return collect;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Integer fromTmpl(Map<String, Object> params) {
		// 校验参数合法
		Integer pid = (Integer) params.get("pid");
		List<Integer> varIds = (List<Integer>) params.get("varIds");
		ValidateUtils.is(pid).notNull(RCode.CHART_PID_ISNULL).and(varIds).notNull(RCode.CHART_VARID_ISNULL);
		/*
		 * 当panelId 不为空时，是向一个panel添加多个 chart
		 * 当panelId 为空时，则向多个panel (varIds) 添加一个chart
		 */
		Integer panelId = (Integer) params.get("panelId");

		List<VisualChart> chartList = new ArrayList<>();
		// 查询模板添加的chart信息
		VisualChart chartInfo = this.baseMapper.queryInfo(pid);
		chartList.add(chartInfo);
		if (Constant.CHART_GROUP_TYPE.equalsIgnoreCase(chartInfo.getType())) {
			// 如果类型为 group 则添加组下所有 charts
			List<VisualChart> childrenChartList = this.list(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getGroupId, chartInfo.getId()).eq(VisualChart::getPanelId, 0));
			List<Integer> childrenChartIdList = childrenChartList.stream().map(VisualChart::getId).distinct().collect(Collectors.toList());
			
			if(ObjectUtil.isNotEmpty(childrenChartIdList)) {
				List<VisualChart> visualCharts = this.baseMapper.queryList(childrenChartIdList);
				chartList.addAll(visualCharts);
			}
		}

		// 按照 group 升序，先添加组
		chartList = chartList.stream().sorted(Comparator.comparing(VisualChart::getGroupId)).collect(Collectors.toList());

		List<VisualPanel> panelList = visualPanelService.list();

		if (ObjectUtil.isNotEmpty(panelId)) {
			VisualPanel panel = panelList.stream().filter(visualPanel -> visualPanel.getId().equals(panelId)).findAny().orElse(null);
			ValidateUtils.is(panel).notNull(RCode.PANEL_NOT_EXIST);
		}

		Integer total = 0;
		for (Integer entityId : varIds) {
			Map<Integer, Integer> groupIdMapping = new HashMap<>();

			List<VisualChart> tempChartList = JSONObject.parseArray(JSONObject.toJSONString(chartList), VisualChart.class);
			for (VisualChart chart : tempChartList) {
				Map<Integer, Object> objs = new HashMap<>();
				// 查询所有 varId 对应数据信息
				if (chart.getVarType().equals(Constant.ChartVariableType.ASSET.getValue())) {
					AssetAsset asset = assetAssetService.queryAssetInfo(entityId);
					objs.put(entityId, asset);
				} else if (chart.getVarType().equals(Constant.ChartVariableType.ENDPOINT.getValue())) {
					MonitorEndpoint endpoint = monitorEndpointService.queryEndpointEntity(entityId);
					objs.put(entityId, endpoint);
				}

				if (ToolUtil.isEmpty(objs.get(entityId))) {
					throw new NZException(RCode.CHART_VARID_ERROR);
				}

				VisualChart visualChart = new VisualChart();
				BeanUtil.copyProperties(chart, visualChart);

				visualChart.setId(null);
				visualChart.setPid(chart.getId());
				visualChart.setVarId(entityId);
				visualChart.setUpdateAt(new Date());
				visualChart.setUpdateBy(ShiroUtils.getUserId().intValue());

				if (ObjectUtil.isNotEmpty(panelId)) {
					visualChart.setPanelId(panelId);
				} else {
					String type = chart.getVarType().equals(Constant.ChartVariableType.ASSET.getValue()) ? Constant.PanelType.ASSET.getValue() : Constant.PanelType.ENDPOINT.getValue();
					VisualPanel linkPanel = panelList.stream().filter(panel -> type.equalsIgnoreCase(panel.getType()) && entityId.equals(panel.getLink())).findAny().orElse(null);
					visualChart.setPanelId(linkPanel.getId());
				}

				visualChart.setName(NzExpressionUtil.renderExpression(visualChart.getName(), objs.get(entityId)));
				visualChart.setParam(ToolUtil.isNotEmpty(visualChart.getParam()) ? Tool.NzExpressionUtil.renderExpression(visualChart.getParam().toString(), objs.get(entityId)) : null);

				Integer newGroupId = groupIdMapping.get(visualChart.getGroupId());
				visualChart.setGroupId(newGroupId == null ? 0 : newGroupId);

				// 因为 组名不能重复问题 如果添加组则校验组名唯一
				if (Constant.CHART_GROUP_TYPE.equalsIgnoreCase(chart.getType())) {
					List<VisualChart> charts = this.list(new QueryWrapper<VisualChart>().lambda().eq(VisualChart::getName, visualChart.getName()).eq(VisualChart::getType, Constant.CHART_GROUP_TYPE.toString()).eq(VisualChart::getPanelId, visualChart.getPanelId()));
					if (ToolUtil.isNotEmpty(charts)) {
						throw new NZException(RCode.CHART_GROUP_NAME_DUPLICATE);
					}
				}

				// 保存
				this.save(visualChart);
				total++;

				// 添加之后 group 类型保存 id mapping
				if (Constant.CHART_GROUP_TYPE.equalsIgnoreCase(chart.getType())) {
					groupIdMapping.put(chart.getId(), visualChart.getId());
				}

				if (CollectionUtils.isNotEmpty(visualChart.getElements())) {
					for (VisualChartElement element : visualChart.getElements()) {
						element.setId(null);
						element.setExpression(NzExpressionUtil.renderExpression(element.getExpression(), objs.get(entityId)));
						element.setChartId(visualChart.getId());
					}
					chartElementService.saveBatch(visualChart.getElements());
				}
			}
		}
		return total;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Integer syncTmpl(Map<String, Object> params) {
		// 校验参数合法
		Integer pid = (Integer) params.get("pid");
		List<Integer> ids = (List<Integer>) params.get("ids");
		Integer panelId = (Integer) params.get("panelId");
		List<Integer> models = (List<Integer>) params.get("models");
		Integer modules = (Integer) params.get("modules");
		// 五个参数不能同时为空
		if(ObjectUtils.isEmpty(pid) && ObjectUtils.isEmpty(ids) && ObjectUtils.isEmpty(panelId)
				&&ObjectUtils.isEmpty(models)&&ObjectUtils.isEmpty(modules)) {
			throw new NZException(RCode.CHART_SYNCTMPL_ERROR);
		}
		List<VisualChart> analysis = new ArrayList<VisualChart>();
		if(ToolUtil.isNotEmpty(pid) || ToolUtil.isNotEmpty(ids) || ToolUtil.isNotEmpty(panelId)) {
			// 查询
			List<VisualChart> charts = this.baseMapper.selectTmplCharts(params);
			if(ToolUtil.isEmpty(charts)) {
				return 0;
			}
			// 记录同步的chartId统一删除
			List<Integer> removeIds = charts.stream().map(VisualChart::getId).collect(Collectors.toList());
			// 获取所有父类模板的id信息
			List<Integer> pids = charts.stream().map(VisualChart::getPid).collect(Collectors.toList());
			// 获取所有模板数据
			List<VisualChart> templateCharts = this.baseMapper.queryList(pids);
			List<VisualChart> chartResult = this.handlerSyncChart(charts,templateCharts,null);
			if(ToolUtil.isNotEmpty(chartResult)) {
				// 批量删除chart 重新同步chart
				this.delCharts(removeIds.toArray(new Integer[] {removeIds.size()}), 1);
			}
			analysis.addAll(chartResult);
		}
		if(ToolUtil.isNotEmpty(models) || ToolUtil.isNotEmpty(modules)) {
			if(ToolUtil.isNotEmpty(models)) {
				List<AssetModel> assetModels = (List<AssetModel>) assetModelService.listByIds(models);
				for(AssetModel assetModel : assetModels) {
					if(StrUtil.isNotBlank(assetModel.getChartIds())) {
						List<String> assetModelChartIds = Arrays.asList(assetModel.getChartIds().split(","));
						List<Integer> chartIdList = assetModelChartIds.stream().map(Integer::parseInt).collect(Collectors.toList());					
						// 获取所有模板数据
						List<VisualChart> templateCharts = this.baseMapper.queryList(chartIdList);
						// 获取所有实例化图表数据信息 用于删除操作
						List<VisualChart> charts = this.baseMapper.selectInstanceChart(1, assetModel.getId());
						// 记录同步的chartId统一删除
						List<Integer> removeIds = charts.stream().map(VisualChart::getId).collect(Collectors.toList());
						Map<String,Object> assetMap =new HashMap<String,Object>();
						assetMap.put("modelIds", assetModel.getId()+"");
						assetMap.put("pageSize", "-1");
						PageUtils assetPage = assetAssetService.queryAssetPage(assetMap);
						List<AssetAsset> assetAssets = (List<AssetAsset>) assetPage.getList();
						if(ToolUtil.isEmpty(assetAssets)) {
							continue;
						}
						List<VisualChart> chartResult = this.handlerSyncChart(null,templateCharts,assetAssets);
						if(ToolUtil.isNotEmpty(chartResult)) {
							// 批量删除chart 重新同步chart
							this.delCharts(removeIds.toArray(new Integer[] {removeIds.size()}), 1);
						}
						analysis.addAll(chartResult);
					}
				}
			}
			
			if(ToolUtil.isNotEmpty(modules)) {
				MonitorModule monitorModule = monitorModuleService.getById(modules);
				if(StrUtil.isNotBlank(monitorModule.getChartIds())) {
					List<String> moduleChartIds = Arrays.asList(monitorModule.getChartIds().split(","));
					List<Integer> chartIdList = moduleChartIds.stream().map(Integer::parseInt).collect(Collectors.toList());					
					// 获取所有模板数据
					List<VisualChart> templateCharts = this.baseMapper.queryList(chartIdList);
					// 获取所有实例化图表数据信息 用于删除操作
					List<VisualChart> charts = this.baseMapper.selectInstanceChart(2, monitorModule.getId());
					// 记录同步的chartId统一删除
					List<Integer> removeIds = charts.stream().map(VisualChart::getId).collect(Collectors.toList());
					Map<String,Object> endpointMap =new HashMap<String,Object>();
					endpointMap.put("moduleIds", monitorModule.getId()+"");
					endpointMap.put("pageSize", "-1");
					PageUtils endpointPage = (PageUtils) monitorEndpointService.queryPage(endpointMap).get("page");
					List<MonitorEndpoint> monitorEndpoints = (List<MonitorEndpoint>) endpointPage.getList();
					if(ToolUtil.isEmpty(monitorEndpoints)) {
						return 0;
					}
					List<VisualChart> chartResult = this.handlerSyncChart(null,templateCharts,monitorEndpoints);
					if(ToolUtil.isNotEmpty(chartResult)) {
						// 批量删除chart 重新同步chart
						this.delCharts(removeIds.toArray(new Integer[] {removeIds.size()}), 1);
					}
					analysis.addAll(chartResult);
				}
			}
		}
		return analysis.size();
	}
	
	/**
	 * 传入已经通过模板生成的图表 用于获取绑定关联关系
	 * 传入模板图表 进行sync操作
	 * 
	 * @param instanceCharts
	 * @param tmpCharts
	 * @return
	 */
	public List<VisualChart> handlerSyncChart(List<VisualChart> instanceCharts,List<VisualChart> tmpCharts,Object obj) {
		List<VisualChart> resultCharts = new ArrayList<VisualChart>();
		// 处理模板图表 主要处理当前模板图表中有group组新增图表情况的可能 将图表类型是组下的图表先剔除 后添加 避免重复添加
		List<VisualChart> groupTmpCharts = new ArrayList<VisualChart>();
		
		Map<Integer,List<VisualChart>> groupIdAndChildren = new HashMap<Integer,List<VisualChart>>();
		// 根据组进行排序
		tmpCharts=tmpCharts.stream().sorted(Comparator.comparing(VisualChart::getGroupId).reversed()).collect(Collectors.toList());
		for(int i =tmpCharts.size()-1;i>=0;i--) {
			VisualChart tmpChart = tmpCharts.get(i);
			if(tmpChart.getType().equals(Constant.CHART_GROUP_TYPE)) {
				// 如果类型为组 查询组下所有模板信息
				List<VisualChart> list = this.list(new QueryWrapper<VisualChart>().lambda().eq(VisualChart::getGroupId, tmpChart.getId()));
				List<Integer> childIds = list.stream().map(VisualChart::getId).collect(Collectors.toList());
				if(ToolUtil.isNotEmpty(childIds)) {
					List<VisualChart> groupChildrens = this.baseMapper.queryList(childIds);
					groupTmpCharts.addAll(groupChildrens);
					groupIdAndChildren.put(tmpChart.getId(),groupChildrens);
				}
			}
			if(tmpChart.getGroupId()!=0 && ToolUtil.isNotEmpty(groupIdAndChildren.get(tmpChart.getGroupId()))) {
				tmpCharts.remove(i);
			}
		}
		// 组装所有模板图表包含新增图表
		tmpCharts.addAll(groupTmpCharts);
		// 根据后两项参数进行逻辑区分
		if (ToolUtil.isEmpty(obj) && instanceCharts!=null) {
			Map<Integer, VisualChart> map = tmpCharts.stream().collect(Collectors.toMap(VisualChart::getId, Function.identity()));
			instanceCharts = instanceCharts.stream().sorted(Comparator.comparing(VisualChart::getGroupId)).collect(Collectors.toList());
			for(VisualChart instanceChart : instanceCharts) {
				VisualChart temp = map.get(instanceChart.getPid());
				if(ToolUtil.isEmpty(temp) || (temp.getGroupId()!=0 && ToolUtil.isNotEmpty(groupIdAndChildren.get(temp.getGroupId())))) {
					continue;
				}
				Integer groupChartId = handlerSyncGroupChart(temp,instanceChart,resultCharts);
				if(temp.getType().equals(Constant.CHART_GROUP_TYPE)) {
					instanceChart.setGroupId(groupChartId);
					List<VisualChart> children = groupIdAndChildren.get(temp.getId());
					if(ToolUtil.isNotEmpty(children)) {
						for(VisualChart child : children) {
							handlerSyncGroupChart(child,instanceChart,resultCharts);
						}
					}
					
				}
			}
		}else if(ToolUtil.isNotEmpty(obj) && instanceCharts==null){
			Map<String,Integer> tmpGroupIdAndInstanceGroupId = new HashMap<String,Integer>();
			for(VisualChart tmpChart : tmpCharts) {
				for(Object data : (List<Object>)obj) {
					Integer varId =0;
					String type="";
					if(data instanceof MonitorEndpoint) {
						data = (MonitorEndpoint) data;
						varId = ((MonitorEndpoint) data).getId();
						type = Constant.PanelType.ENDPOINT.getValue();
					}else if(data instanceof AssetAsset){
						data = (AssetAsset) data;
						varId = ((AssetAsset) data).getId();
						type = Constant.PanelType.ASSET.getValue();
					}
					VisualChart syncChart =new VisualChart();
					BeanUtils.copyProperties(tmpChart, syncChart);
					Integer instantceId = tmpGroupIdAndInstanceGroupId.get(tmpChart.getGroupId()+":"+varId);
					syncChart.setGroupId(ToolUtil.isEmpty(instantceId)? 0:instantceId);
					syncChart.setPid(tmpChart.getId());
					syncChart.setVarId(varId);
					syncChart.setUpdateAt(new Date());
					syncChart.setUpdateBy(ShiroUtils.getUserId().intValue());
					syncChart.setName(NzExpressionUtil.renderExpression(tmpChart.getName(),data));
					syncChart.setRemark(NzExpressionUtil.renderExpression(tmpChart.getRemark(),data));
					syncChart.setParam(ToolUtil.isNotEmpty(tmpChart.getParam()) ? Tool.NzExpressionUtil.renderExpression(tmpChart.getParam().toString(), data) : null);
					// 获取各自的panelId信息
					VisualPanel panel = visualPanelService.getOne(new QueryWrapper<VisualPanel>().lambda().eq(VisualPanel::getType, type).eq(VisualPanel::getLink, varId));
					syncChart.setPanelId(panel.getId());
					List<VisualChartElement> elements = tmpChart.getElements();
					List<VisualChartElement> copyElements = new ArrayList<VisualChartElement>();
					//将表达式进行替换
					for(VisualChartElement element : elements) {
						VisualChartElement vce = new VisualChartElement();
						BeanUtil.copyProperties(element, vce);
						String renderExpression = NzExpressionUtil.renderExpression(vce.getExpression(), data);
						vce.setExpression(renderExpression);
						copyElements.add(vce);
					}
					syncChart.setElements(copyElements);
					resultCharts.add(syncChart);
					
					if(syncChart.getType().equals(Constant.CHART_GROUP_TYPE)) {
						// 当chart为group时 不允许name重复  会影响chart批量导入
						VisualChart chart = this.getOne(new QueryWrapper<VisualChart>().lambda().eq(VisualChart::getName, syncChart.getName()).eq(VisualChart::getType, Constant.CHART_GROUP_TYPE.toString()).eq(VisualChart::getPanelId, syncChart.getPanelId()).ne(ToolUtil.isNotEmpty(syncChart.getId()), VisualChart::getId,syncChart.getId()));
			            if(ToolUtil.isNotEmpty(chart)) {
			            	// group chart不能重复  先将group删除 防止不能创建组
							this.removeById(chart.getId());
			            }
					}
					this.save(syncChart);
					List<VisualChartElement> tmplChartElements = new ArrayList<VisualChartElement>();
					List<VisualChartElement> instanceElements = syncChart.getElements();
					for(VisualChartElement element : instanceElements) {
						element.setChartId(syncChart.getId());
						tmplChartElements.add(element);
					}
					chartElementService.saveBatch(tmplChartElements);
					if(tmpChart.getType().equals(Constant.CHART_GROUP_TYPE)) {
						tmpGroupIdAndInstanceGroupId.put(tmpChart.getId()+":"+varId, syncChart.getId());
					}
				}
			}
		}
		return resultCharts;
	}

	public Integer handlerSyncGroupChart(VisualChart temp,VisualChart instance,List<VisualChart> result) {
		VisualChart syncChart =new VisualChart();
		BeanUtils.copyProperties(temp, syncChart);
		syncChart.setPid(temp.getId());
		syncChart.setVarId(instance.getVarId());
		syncChart.setUpdateAt(new Date());
		syncChart.setUpdateBy(ShiroUtils.getUserId().intValue());

		Object obj = null;
		if (instance.getVarType().equals(Constant.ChartVariableType.ENDPOINT.getValue())) {
			obj = monitorEndpointService.queryEndpointEntity(instance.getVarId());
		} else if (instance.getVarType().equals(Constant.ChartVariableType.ASSET.getValue())) {
			obj = assetAssetService.queryAssetInfo(instance.getVarId());
		}
		
		syncChart.setName(NzExpressionUtil.renderExpression(temp.getName(),obj));
		syncChart.setRemark(NzExpressionUtil.renderExpression(temp.getRemark(),obj));
		syncChart.setParam(ToolUtil.isNotEmpty(temp.getParam()) ? Tool.NzExpressionUtil.renderExpression(temp.getParam().toString(), obj) : null);
		syncChart.setPanelId(instance.getPanelId());
		syncChart.setGroupId(instance.getGroupId());
		List<VisualChartElement> elements = temp.getElements();
		List<VisualChartElement> copyElements = new ArrayList<VisualChartElement>();
		//将表达式进行替换
		for(VisualChartElement element : elements) {
			VisualChartElement vce = new VisualChartElement();
			BeanUtil.copyProperties(element, vce);
			String renderExpression = NzExpressionUtil.renderExpression(vce.getExpression(), obj);
			vce.setExpression(renderExpression);
			copyElements.add(vce);
		}
		syncChart.setElements(copyElements);
		result.add(syncChart);		
		if(syncChart.getType().equals(Constant.CHART_GROUP_TYPE)) {
			// 当chart为group时 不允许name重复
        	VisualChart chart = this.getOne(new QueryWrapper<VisualChart>().lambda().eq(VisualChart::getName, syncChart.getName()).eq(VisualChart::getType, Constant.CHART_GROUP_TYPE.toString()).eq(VisualChart::getPanelId, syncChart.getPanelId()).ne(ToolUtil.isNotEmpty(syncChart.getId()), VisualChart::getId,syncChart.getId()));
			if(ToolUtil.isNotEmpty(chart)) {
				// group chart不能重复  先将group删除 防止不能创建组
				this.removeById(chart.getId());
			}
		}
		this.save(syncChart);
		List<VisualChartElement> tmplChartElements = new ArrayList<VisualChartElement>();
		List<VisualChartElement> instanceElements = syncChart.getElements();
		for(VisualChartElement element : instanceElements) {
			element.setChartId(syncChart.getId());
			tmplChartElements.add(element);
		}
		chartElementService.saveBatch(tmplChartElements);
		return syncChart.getId();
	}
	
	@Override
	public void saveRelPanelAndChart(String typeStr, Integer linkId, String seq, VisualChart chartTemplate) {
		VisualPanel panel = new VisualPanel();
		panel.setName(typeStr + "_panel_" + linkId);
		panel.setType(typeStr);
		panel.setPid(0);
		panel.setLink(linkId);
		panel.setWeight(0);
		panel.setCreateBy(ShiroUtils.getUserId().intValue());
		panel.setUts(new Date().getTime());
		if(StringUtils.isNotEmpty(seq)){
			panel.setSeq(seq);
		}
		visualPanelService.save(panel);

		VisualChart target = new VisualChart();
		BeanUtils.copyProperties(chartTemplate, target);
		target.setParam(ToolUtil.isNotEmpty(target.getParam()) ? JSONObject.toJSONString(target.getParam()) : null);
		target.setPanelId(panel.getId());
		target.setGroupId(0);
		target.setUpdateBy(ShiroUtils.getUserId().intValue());
		target.setDatasource(ChartDatasourceType.SYSTEM.getValue());
		target.setUpdateAt(new Date());
		if(StringUtils.isNotEmpty(seq)){
			target.setSeq(seq);
		}
		this.save(target);
	}

	@Override
	public List<VisualChart> queryChartWithElementByIds(List<Integer> chartIdList) {
		List<VisualChart> visualCharts = this.baseMapper.queryChartWithElementByIds(chartIdList);
		return CollectionUtils.isEmpty(visualCharts) ? new ArrayList<>() : visualCharts;
	}
}
