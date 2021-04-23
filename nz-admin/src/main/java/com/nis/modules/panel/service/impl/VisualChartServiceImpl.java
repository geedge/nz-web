package com.nis.modules.panel.service.impl;

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
import com.nis.common.utils.DateUtil;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.common.utils.StringUtils;
import com.nis.common.utils.Tool.NzExpressionUtil;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.asset.dao.AssetAssetDao;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetTag;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.endpoint.dao.MonitorEndpointDao;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.panel.dao.VisualChartDao;
import com.nis.modules.panel.entity.Chart;
import com.nis.modules.panel.entity.Panel;
import com.nis.modules.panel.entity.VisualChart;
import com.nis.modules.panel.entity.VisualChartElement;
import com.nis.modules.panel.entity.VisualPanel;
import com.nis.modules.panel.service.VisualChartElementService;
import com.nis.modules.panel.service.VisualChartService;
import com.nis.modules.panel.service.VisualPanelService;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysDictService;
import com.nis.modules.sys.shiro.ShiroUtils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

@Service("visualChartService")
public class VisualChartServiceImpl extends ServiceImpl<VisualChartDao, VisualChart> implements VisualChartService {
	
	private Log log = Log.get();
	
	@Autowired
    private SysConfigService sysConfigService;
	
	@Autowired
	private VisualPanelService visualPanelService;
	
	@Autowired
	private SysDictService sysDictService;
	
	@Autowired
	private VisualChartElementService chartElementService;
	
	@Autowired
	private AssetService assetService;
	
	@Autowired
	private AssetAssetService assetAssetService;
	
	@Autowired
	private MonitorEndpointService monitorEndpointService;
	
	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		String ids = (String) params.get("ids");
        if(StrUtil.isNotBlank(ids)) {
        	List<String> listIds = Arrays.asList(ids.split(","));
        	params.put("ids",listIds);
        }
        String pid = (String) params.get("pid");
        if(StrUtil.isNotBlank(pid)) {
        	List<String> listIds = Arrays.asList(pid.split(","));
        	params.put("pid",listIds);
        }
		IPage<VisualChart> page = new Query<VisualChart>().getPage(params);
		List<VisualChart> result = this.baseMapper.queryPage(page,params);

		String param;
        for (VisualChart chart : result) {
            // param 格式处理
            param = (String) chart.getParam();
            if (StringUtils.isNotEmpty(param)) {
                chart.setParam(JSONObject.parseObject(param, Map.class));
            }
        }
        
        String returnChildren = (String) params.get("returnChildren");
        returnChildren = ToolUtil.isEmpty(returnChildren) ? "1" : returnChildren;
        if(ToolUtil.equals(returnChildren, "1")) {
        	result = this.getChartTreeList(result,0);
        }
        
        
        page.setRecords(result);
        //处理所有group下children
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
	 * @param panelId
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
	    ValidateUtils.is(chart.getName()).notNull(RCode.CHART_TITLE_ISNULL)
	            .and(chart.getSpan()).notNull(RCode.CHART_SPAN_ISNULL)
	            .and(chart.getHeight()).notNull(RCode.CHART_HEIGHT_ISNULL)
	            .and(type).notNull(RCode.CHART_TYPE_ISNULL);
	
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
	                throw new NZException(RCode.CHART_PARAMURL_ISNULL);
	            }
	
	            String urlStr = url.toString();
	            // 如果是model panel添加charts url类型支持{{}}替换
	            VisualPanel panel = visualPanelService.getById(panelId);
	            if(StrUtil.equals(Constant.PanelType.MODEL.getValue(),panel.getType())) {
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
	        // singleStat 类型
	        case Constant.CHART_SINGLESTAT_TYPE: {
	            ValidateUtils.is(param).notNull(RCode.CHART_SINGLESTATPARAM_ISNULL)
	                    .and(elements).notNull(RCode.CHART_ELEMENT_ISNULL);
	
	            Object statistics = param.get(Constant.CHART_SINGLESTAT_PARAMkEY);
	            if (ToolUtil.isEmpty(statistics)) {
	                throw new NZException(RCode.CHART_PARAMSINGLESTAT_ISNULL);
	            }
	            if (!Constant.CHART_SINGLESTAT_STATISTICS.contains(statistics.toString())) {
	                throw new NZException(RCode.CHART_PARAMSINGLESTAT_INVALIDE);
	            }
	
	            chartElementService.validateElements(elements);
	            break;
	        }
	        // alertList 类型不需要校验 elements
	        case Constant.CHART_ALERTLIST_TYPE:{
	            break;
	        }
	        // group 类型不需要校验 elements
	        case Constant.CHART_GROUP_TYPE:{
	            break;
	        }
	        case Constant.CHART_TEXT_TYPE: {
	            ValidateUtils.is(param).notNull(RCode.CHART_TEXTPARAM_ISNULL);
	            Object text = param.get(Constant.CHART_TEXT_TYPE);
	            if (ToolUtil.isEmpty(text)) throw new NZException(RCode.CHART_PARAMTEXT_ISNULL);
	            break;
	        }
	        default: {
	            // 除 url alertList 类型外，其他类型都需校验 elements
	            ValidateUtils.is(elements).notNull(RCode.CHART_ELEMENT_ISNULL);
	            chartElementService.validateElements(elements);
	        }
	    }
	
	    // 4. 校验 unit 是否合法
	    Integer unit = chart.getUnit();
	    if (unit != null) {
	        List<Integer> unitList = new ArrayList(Constant.ALERT_RULE_UNIT_MAP.values());
	        if (unit < Collections.min(unitList) || unit > Collections.max(unitList)) {
	            throw new NZException(RCode.ALERTRULE_UNIT_INVALIDE);
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
        chart.setUpdateAt(DateUtil.getUTCTimeByConfigTimeZone());
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

        // 4. 同步新增处理
        Integer sync = chart.getSync();
        if (sync != null) {
            VisualPanel panel = visualPanelService.getById(chart.getPanelId());
            // 当前 chart 所属 panel 为 model 类型并且 同步标识 为 1 时 ，对已通过对应 model panel 创建的 asset panel 追加新增的 chart
            if(StrUtil.equals(Constant.PanelType.MODEL.getValue(),panel.getType()) && sync.equals(1)) {
                this.saveAssetChartsSimultaneously(panel, chart);
            }
        }
        
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
        List<Asset> assets = assetService.getAssetsWithTagsByModelId(modelPanel.getLink());
        if (CollectionUtils.isEmpty(assets)) {
            return;
        }

        // 2. 通过 assets 找到关联的所有 asset panel
        List<Integer> assetsIds = assets.stream().map(Asset::getId).collect(Collectors.toList());
        List<VisualPanel> panels = visualPanelService.list(new QueryWrapper<VisualPanel>().lambda().eq(VisualPanel::getType, Constant.PanelType.ASSET.getValue()).in(VisualPanel::getLink, assetsIds));

        Map<Integer, Asset> assetIdAndEntityMap = new HashMap<>();
        for (Asset asset : assets) {
            assetIdAndEntityMap.put(asset.getId(), asset);
        }

        // 3. 得到 panelId 和 关联的 asset 实体 ，准备做 变量替换所需
        Map<Integer, Asset> panelIdAndAsset = new HashMap<>();
        for (VisualPanel entity : panels) {
            panelIdAndAsset.put(entity.getId(), assetIdAndEntityMap.get(entity.getLink()));
        }

        // 4. 处理属性
        VisualChart targetChart = new VisualChart();
        Asset asset;
        for (Map.Entry<Integer, Asset> entry : panelIdAndAsset.entrySet()) {
            asset = entry.getValue();
            targetChart = JSONObject.parseObject(JSON.toJSONString(chart), VisualChart.class);
            targetChart.setSync(null);
            targetChart.setId(null);
            targetChart.setPid(chart.getId());

            // chart ： title param   chart element : expression   属性需要做 asset 变量替换
            targetChart.setName(this.replacAssetInfos(targetChart.getName(), asset));
            targetChart.setParam(ToolUtil.isNotEmpty(targetChart.getParam()) ? this.replacAssetInfos(targetChart.getParam().toString(), asset) : null);

            if (CollectionUtils.isNotEmpty(targetChart.getElements())) {
                for (VisualChartElement element : targetChart.getElements()) {
                    element.setId(null);
                    element.setExpression(ToolUtil.isNotEmpty(element.getExpression()) ? this.replacAssetInfos(element.getExpression(), asset) : null);
                }
            }

            // 5. 调用保存方法进行 新增
            this.saveChart(targetChart);
        }
    }

    
    private static Pattern PATTERN = Pattern.compile(".*\\{\\{asset.tag.\\[.*\\]\\}\\}.*");
    /**
     * model 同步更新时 标签替换
     *
     * @param str
     * @param asset
     * @return
     */
    private String replacAssetInfos(String str, Asset asset) {
        str = str.replaceAll("\\{\\{asset.host\\}\\}", asset.getHost());
        str = str.replaceAll("\\{\\{asset.sn\\}\\}", asset.getSn());
        List<AssetTag> tags = asset.getTags();
        if(CollectionUtils.isNotEmpty(tags)){
            for (AssetTag tag : tags) {
                if(PATTERN.matcher(str).matches()){
                    str = str.replaceAll("\\{\\{asset.tag.\\[" + StringUtils.escapeString(tag.getTag()) + "\\]\\}\\}", tag.getValue());
                }
                str = str.replaceAll("\\{\\{asset.tag." + tag.getTag() + "\\}\\}", tag.getValue());
            }
        }
        return str;
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
        List<VisualChart> chartIdWithAssetEntity = assetService.getAssetsWithTagByChartIds(idList);
        Map<Integer, Asset> chartIdAndAssetEntityMap = chartIdWithAssetEntity.stream().collect(Collectors.toMap(VisualChart::getId, VisualChart::getAsset));

        // 4. 处理更新
        List<VisualChartElement> elements = chart.getElements();
        List<VisualChartElement> saveElements = new ArrayList<>();
        VisualChartElement tempElement;
        Asset asset;
        for (VisualChart entity : charts) {
            asset = chartIdAndAssetEntityMap.get(entity.getId());
            entity.setName(this.replacAssetInfos(chart.getName(), asset));
            entity.setSpan(chart.getSpan());
            entity.setHeight(chart.getHeight());
            entity.setType(chart.getType());
            entity.setUnit(chart.getUnit());
            entity.setParam(ToolUtil.isNotEmpty(chart.getParam()) ? this.replacAssetInfos(chart.getParam().toString(), asset) : null);
            if (CollectionUtils.isNotEmpty(elements)) {
                for (VisualChartElement chartElement : elements) {
                    tempElement = new VisualChartElement();
                    BeanUtils.copyProperties(chartElement, tempElement);
                    tempElement.setChartId(entity.getId());
                    tempElement.setId(null);
                    tempElement.setExpression(ToolUtil.isNotEmpty(chartElement.getExpression()) ? this.replacAssetInfos(chartElement.getExpression(), asset) : null);
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
        // 删除自身
        this.baseMapper.deleteBatchIds(Arrays.asList(ids));

        // 删除char element
        chartElementService.delElementByChartIds(Arrays.asList(ids));
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
		ValidateUtils.is(pid).notNull(RCode.CHART_PID_ISNULL)
		.and(varIds).notNull(RCode.CHART_VARID_ISNULL);
		
		String ids = org.apache.commons.lang3.StringUtils.join(varIds,StrUtil.COMMA);
		params.put("ids", ids);
		params.put("pageSize", "-1");
		List<VisualChart> tmplCharts = new ArrayList<VisualChart>();
		List<VisualChartElement> tmplChartElements = new ArrayList<VisualChartElement>();
		/*
		 * 当panelId 不为空时，是向一个panel添加多个 chart
		 * 当panelId 为空时，则向多个panel (varIds) 添加一个chart
		 */
		Integer panelId = (Integer) params.get("panelId");
				
		// 查询模板添加的chart信息
		VisualChart chart = this.baseMapper.queryInfo(pid);
		chart.setId(null);
		
		Map<Integer,Object> objs =new HashMap<Integer,Object>();
		// 查询所有varId对应数据信息
		if(chart.getVarType().equals(1)) {
			PageUtils assetPage = assetAssetService.queryAssetPage(params);
			List<AssetAsset> assets = (List<AssetAsset>) assetPage.getList();
			for(AssetAsset asset : assets) {
				objs.put(asset.getId(), asset);
			}
		}else if(chart.getVarType().equals(2)) {
			PageUtils endpointPage = monitorEndpointService.queryPage(params);
			List<MonitorEndpoint> endpoints = (List<MonitorEndpoint>) endpointPage.getList();
			for(MonitorEndpoint endpoint : endpoints) {
				objs.put(endpoint.getId(), endpoint);
			}
		}
		
		if(ToolUtil.isEmpty(objs) || !ToolUtil.equals(objs.size(), varIds.size())) {
			throw new NZException(RCode.CHART_VARID_ERROR);
		}
		
		if(ObjectUtil.isNotEmpty(panelId)) {	
			for(Integer id : varIds) {
				VisualChart visualChart = new VisualChart();
				BeanUtil.copyProperties(chart, visualChart);
				visualChart.setPid(chart.getId());
				visualChart.setVarId(id);
				visualChart.setUpdateAt(DateUtil.getUTCTimeByConfigTimeZone());
				visualChart.setUpdateBy(ShiroUtils.getUserId().intValue());
				visualChart.setPanelId(panelId);
				visualChart.setName(NzExpressionUtil.renderExpression(visualChart.getName(), objs.get(id)));
				visualChart.setRemark(NzExpressionUtil.renderExpression(visualChart.getRemark(), objs.get(id)));
				List<VisualChartElement> elements = visualChart.getElements();
				List<VisualChartElement> copyElements = new ArrayList<VisualChartElement>();
				//将表达式进行替换
				for(VisualChartElement element : elements) {
					VisualChartElement vce = new VisualChartElement();
					BeanUtil.copyProperties(element, vce);
					String renderExpression = NzExpressionUtil.renderExpression(vce.getExpression(), objs.get(id));
					vce.setExpression(renderExpression);
					copyElements.add(vce);
				}
				visualChart.setElements(copyElements);
				tmplCharts.add(visualChart);
			}
		}else {
			Integer varType = chart.getVarType();
			List<VisualPanel> panels = null;
			if(ToolUtil.equals(varType, 1)) {
				panels = visualPanelService.list(new QueryWrapper<VisualPanel>().lambda().eq(VisualPanel::getType, Constant.PanelType.ASSET));
			}else if(ToolUtil.equals(varType, 2)) {
				panels = visualPanelService.list(new QueryWrapper<VisualPanel>().lambda().eq(VisualPanel::getType, Constant.PanelType.ENDPOINT));
			}
			if(ToolUtil.isEmpty(panels)) {
				return 0;
			}
			for(VisualPanel panel : panels) {
				for(Integer id : varIds) {
					VisualChart visualChart = new VisualChart();
					BeanUtil.copyProperties(chart, visualChart);
					visualChart.setVarId(id);
					visualChart.setPanelId(panel.getId());
					visualChart.setUpdateAt(DateUtil.getUTCTimeByConfigTimeZone());
					visualChart.setUpdateBy(ShiroUtils.getUserId().intValue());
					visualChart.setName(NzExpressionUtil.renderExpression(visualChart.getName(), objs.get(id)));
					visualChart.setRemark(NzExpressionUtil.renderExpression(visualChart.getRemark(), objs.get(id)));
					visualChart.setPid(chart.getId());
					List<VisualChartElement> elements = visualChart.getElements();
					List<VisualChartElement> copyElements = new ArrayList<VisualChartElement>();
					//将表达式进行替换
					for(VisualChartElement element : elements) {
						VisualChartElement vce = new VisualChartElement();
						BeanUtil.copyProperties(element, vce);
						String renderExpression = NzExpressionUtil.renderExpression(vce.getExpression(), objs.get(id));
						vce.setExpression(renderExpression);
						copyElements.add(vce);
					}
					visualChart.setElements(copyElements);
					tmplCharts.add(visualChart);
				}
			}
		}
		
		// 保存
		this.saveBatch(tmplCharts);
		
		for(VisualChart tmplChart : tmplCharts) {
			List<VisualChartElement> elements = tmplChart.getElements();
			for(VisualChartElement element : elements) {
				element.setChartId(tmplChart.getId());
				tmplChartElements.add(element);
			}
		}
		chartElementService.saveBatch(tmplChartElements);
		return tmplCharts.size();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Integer syncTmpl(Map<String, Object> params) {
		// 校验参数合法
		Integer pid = (Integer) params.get("pid");
		List<Integer> ids = (List<Integer>) params.get("ids");
		Integer panelId = (Integer) params.get("panelId");
		// 三个参数不能同时为空
		if(ObjectUtils.isEmpty(pid) && ObjectUtils.isEmpty(ids) && ObjectUtils.isEmpty(panelId)) {
			throw new NZException(RCode.CHART_SYNCTMPL_ERROR);
		}
		
		List<VisualChart> result =new ArrayList<VisualChart>();
		List<VisualChart> charts = this.baseMapper.selectTmplCharts(params);
		List<Integer> pids = charts.stream().map(VisualChart::getPid).collect(Collectors.toList());
		String tempIds = org.apache.commons.lang3.StringUtils.join(pids,StrUtil.COMMA);
		Map<String,Object> tempMap =new HashMap<String,Object>();
		tempMap.put("ids", tempIds);
		tempMap.put("pageSize", "-1");
		List<VisualChart> templateCharts = this.baseMapper.queryPage(tempMap);
		Map<Integer,VisualChart> map = new HashMap<Integer,VisualChart>(); 
		for(VisualChart vc : templateCharts) {
			map.put(vc.getId(), vc);
		}
		
		// 记录同步的chartId统一删除
		List<Integer> removeIds = charts.stream().map(VisualChart::getId).collect(Collectors.toList());
		// 获取对应实体的数据信息 asset
		Map<Integer, Integer> chartIdAndAssetId = charts.stream().filter(item-> ToolUtil.equals(item.getVarType(), 1)).collect(Collectors.toMap(VisualChart::getId,VisualChart::getVarId));
		List<Integer> assetIds = charts.stream().filter(item-> ToolUtil.equals(item.getVarType(), 1)).map(VisualChart::getVarId).collect(Collectors.toList());
		// 获取对应实体的数据信息 endpoint
		Map<Integer, Integer> chartIdAndEndpointId = charts.stream().filter(item-> ToolUtil.equals(item.getVarType(), 2)).collect(Collectors.toMap(VisualChart::getId,VisualChart::getVarId));
		List<Integer> endpointIds = charts.stream().filter(item-> ToolUtil.equals(item.getVarType(), 2)).map(VisualChart::getVarId).collect(Collectors.toList());
		// 记录保存表达式数据
		List<VisualChartElement> tmplChartElements = new ArrayList<VisualChartElement>();
		
		Map<Integer,AssetAsset>  assetEntites = new HashMap<Integer,AssetAsset>();
		Map<Integer,MonitorEndpoint>  endpointEntites = new HashMap<Integer,MonitorEndpoint>();
		
		Map<Integer,Integer> assetPanelIds = new HashMap<Integer,Integer>();
		Map<Integer,Integer> endpointPanelIds = new HashMap<Integer,Integer>();
		
		if(ToolUtil.isNotEmpty(endpointIds)) {
			String endpointids = org.apache.commons.lang3.StringUtils.join(endpointIds,StrUtil.COMMA);
			Map<String,Object> param = new HashMap<String,Object>();
			param.put("pageSize", "-1");
			param.put("ids", endpointids);
			PageUtils endpointPage = monitorEndpointService.queryPage(param);
			List<MonitorEndpoint> endpoints = (List<MonitorEndpoint>) endpointPage.getList();
			for(MonitorEndpoint endpoint : endpoints) {
				endpointEntites.put(endpoint.getId(), endpoint);
			}
			
			List<VisualPanel> panelIds = visualPanelService.list(new QueryWrapper<VisualPanel>().lambda().eq
					(VisualPanel::getType,Constant.PanelType.ENDPOINT).in(VisualPanel::getLink,endpointIds));
			endpointPanelIds = panelIds.stream().collect(Collectors.toMap(VisualPanel::getLink, VisualPanel::getId));
		}
		
		if(ToolUtil.isNotEmpty(assetIds)) {
			String assetids = org.apache.commons.lang3.StringUtils.join(assetIds,StrUtil.COMMA);
			Map<String,Object> param = new HashMap<String,Object>();
			param.put("pageSize", "-1");
			param.put("ids", assetids);
			PageUtils assetPage = assetAssetService.queryAssetPage(param);
			List<AssetAsset> assets = (List<AssetAsset>) assetPage.getList();
			for(AssetAsset asset : assets) {
				assetEntites.put(asset.getId(), asset);
			}
			
			List<VisualPanel> panelIds = visualPanelService.list(new QueryWrapper<VisualPanel>().lambda().eq
					(VisualPanel::getType,Constant.PanelType.ASSET).in(VisualPanel::getLink,assetIds));
			assetPanelIds = panelIds.stream().collect(Collectors.toMap(VisualPanel::getLink, VisualPanel::getId));
		}
		// 找出自身父类模板信息
		for(VisualChart chart : charts) {
			VisualChart syncChart =new VisualChart();
			VisualChart temp = map.get(chart.getPid());
			BeanUtils.copyProperties(temp, syncChart);
			syncChart.setPid(temp.getId());
			syncChart.setVarId(chart.getVarId());
			syncChart.setUpdateAt(DateUtil.getUTCTimeByConfigTimeZone());
			syncChart.setUpdateBy(ShiroUtils.getUserId().intValue());
			Object obj = ToolUtil.isEmpty(chartIdAndAssetId.get(chart.getId())) ? 
						endpointEntites.get((chartIdAndEndpointId.get(chart.getId()))) :
						assetEntites.get((chartIdAndAssetId.get(chart.getId()))) ;		
			syncChart.setName(NzExpressionUtil.renderExpression(temp.getName(),obj));
			syncChart.setRemark(NzExpressionUtil.renderExpression(temp.getRemark(),obj));
			Integer panelid = ToolUtil.isEmpty(endpointPanelIds) ? 
					assetPanelIds.get(chart.getVarId()) :
						endpointPanelIds.get(chart.getVarId());
			syncChart.setPanelId(panelid);
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
		}
		
		// 批量删除chart 重新同步chart
		this.delCharts(removeIds.toArray(new Integer[] {removeIds.size()}), 1);
		
		// 保存
		
		this.saveBatch(result);
		
		for(VisualChart chart : result) {
			List<VisualChartElement> elements = chart.getElements();
			for(VisualChartElement element : elements) {
				element.setChartId(chart.getId());
				tmplChartElements.add(element);
			}
		}
		chartElementService.saveBatch(tmplChartElements);
		
		return result.size();
	}
}