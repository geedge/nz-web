package com.nis.modules.endpoint.service.impl;

import cn.hutool.core.lang.hash.MurmurHash;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.dao.AlertMessageDao;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.endpoint.dao.MonitorEndpointDao;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.entity.MonitorEndpointConfig;
import com.nis.modules.endpoint.entity.MonitorEndpointState;
import com.nis.modules.endpoint.service.MonitorEndpointConfigService;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.endpoint.service.MonitorEndpointStateService;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.module.service.MonitorModuleService;
import com.nis.modules.panel.entity.VisualChart;
import com.nis.modules.panel.entity.VisualChartElement;
import com.nis.modules.panel.entity.VisualPanel;
import com.nis.modules.panel.service.VisualChartElementService;
import com.nis.modules.panel.service.VisualChartService;
import com.nis.modules.panel.service.VisualPanelService;
import com.nis.modules.project.entity.MonitorProject;
import com.nis.modules.project.service.MonitorProjectService;
import com.nis.modules.sys.dao.SysConfigDao;
import com.nis.modules.sys.entity.SnmpCredential;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SnmpCredentialService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MonitorEndpointServiceImpl extends ServiceImpl<MonitorEndpointDao, MonitorEndpoint> implements MonitorEndpointService {

    private static Log log = Log.get();

    @Autowired
    private AssetAssetService assetService;

    @Autowired
    private SysConfigDao sysConfigDao;

    @Autowired
    private AlertMessageDao alertMessageDao;

    @Autowired
    private MonitorEndpointDao monitorEndpointDao;

    @Autowired
    private MonitorModuleService monitorModuleService;

    @Autowired
    private MonitorProjectService monitorProjectService;

    @Autowired
    private MonitorEndpointStateService endpointStateService;

    @Autowired
    private VisualPanelService visualPanelService;

    @Autowired
    private VisualChartService visualChartService;

    @Autowired
    private AlertMessageService alertMessageService;

    @Autowired
    private VisualChartElementService visualChartElementService;

    @Autowired
    private MonitorEndpointConfigService endpointConfigService;

    @Autowired
    private SnmpCredentialService snmpCredentialService;

    @Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;

    @Override
    public MonitorEndpoint queryEndpointEntity(Integer id) {
        MonitorEndpoint endpoint = this.getById(id);
        if (ToolUtil.isEmpty(endpoint)) {
            throw new NZException(RCode.ENDPOINT_NOTFOUND);
        }
        MonitorModule module = monitorModuleService.getById(endpoint.getModuleId());
        endpoint.setModule(module);
        MonitorProject project = monitorProjectService.getById(module.getProjectId());
        endpoint.setProject(project);
        AssetAsset asset = assetService.getById(endpoint.getAssetId());
        endpoint.setAsset(asset);

        List<AlertMessageEntity> alerts = alertMessageDao.getAlertsStatColumn();
        alerts = alerts.stream().filter(alert -> ToolUtil.isNotEmpty(alert.getEndpointId())).collect(Collectors.toList());
        Map<Integer, List<AlertMessageEntity>> endpointIdAndAlersMap = alerts.stream().collect(Collectors.groupingBy(AlertMessageEntity::getEndpointId));

        List<AlertMessageEntity> tempAlert = endpointIdAndAlersMap.get(endpoint.getId());
        if (CollectionUtils.isEmpty(tempAlert)) {
            endpoint.setAlertNum(0);
        } else {
            endpoint.setAlertNum(tempAlert.size());
        }

        // configs
        List<MonitorEndpointConfig> endpointConfigs = endpointConfigService.list(new LambdaQueryWrapper<MonitorEndpointConfig>().eq(MonitorEndpointConfig::getEndpointId, endpoint.getId()));
        for (MonitorEndpointConfig endpointConfig : endpointConfigs) {
            endpointConfig.setVer(endpointConfig.getVersion());

            MonitorEndpointState endpointStateByConfigId = endpointStateService.getOne(new LambdaQueryWrapper<MonitorEndpointState>().eq(MonitorEndpointState::getConfigId, endpointConfig.getId()));
            if (endpointStateByConfigId == null)
                continue;
            endpointConfig.setState(endpointStateByConfigId.getState());
            endpointConfig.setLastUpdate(endpointStateByConfigId.getTime());
        }
        endpoint.setConfigs(endpointConfigs);
        return endpoint;
    }

    @Override
    public Map<String,Object> queryPage(Map<String, Object> params) {
    	Map<String,Object> result = new HashMap<String,Object>();
        // 处理请求参数
        this.handleQueryParam(params);

        IPage page = new Query(MonitorEndpoint.class).getPage(params);
        List<MonitorEndpoint> datas = monitorEndpointDao.queryPage(page, params);

        // 查询结果不为空时处理返回结果
        if (CollectionUtils.isNotEmpty(datas)) {
            this.handleResuleList(datas,params,result);
        }

        page.setRecords(datas);
        PageUtils pageUtils = new PageUtils(page);
        
        result.put("page", pageUtils);
        return result;
    }

    @SuppressWarnings("unlikely-arg-type")
	private void handleResuleList(List<MonitorEndpoint> endpointList,Map<String, Object> params,Map<String,Object> result) {
        List<MonitorEndpointConfig> endpointConfigs = endpointConfigService.list();

        for (MonitorEndpoint endpoint : endpointList) {
            List<MonitorEndpointConfig> configsByEndpointId = endpointConfigs.stream().filter(endpointConfig -> endpoint.getId().equals(endpointConfig.getEndpointId())).collect(Collectors.toList());
            for (MonitorEndpointConfig endpointConfig : configsByEndpointId) {
                endpointConfig.setVer(endpointConfig.getVersion());

                MonitorEndpointState endpointStateByConfigId = endpointStateService.getOne(new LambdaQueryWrapper<MonitorEndpointState>().eq(MonitorEndpointState::getConfigId, endpointConfig.getId()));
                if (endpointStateByConfigId == null)
                    continue;
                endpointConfig.setState(endpointStateByConfigId.getState());
                endpointConfig.setLastUpdate(endpointStateByConfigId.getTime());
            }
            endpoint.setConfigs(configsByEndpointId);
        }
        
        if(ToolUtil.isNotEmpty(params.get("statistics")) && Integer.valueOf((String)params.get("statistics")).equals(1)) {
        	Map<String,Object> statistics =new HashMap<String,Object>();
        	// project module state
        	Map<Integer,Object> projects = new HashMap<Integer,Object>();
        	Map<Integer,Object> modules = new HashMap<Integer,Object>();
        	Map<String,Object> states = new HashMap<String,Object>();
        	for (MonitorEndpoint endpoint : endpointList) {
        		Integer projectId = endpoint.getProject().getId();
        		if(ToolUtil.isEmpty(projects.get(projectId))) {
        			Map <String,Object> project = new HashMap<String,Object>();
        			project.put("id", projectId);
        			project.put("name", endpoint.getProject().getName());
        			project.put("num", 1);
        			projects.put(projectId,project);
        		}else {
        			Map<String,Object> project = (Map<String, Object>) projects.get(projectId);
        			project.put("num", (Integer)project.get("num")+1);
        		}
        		
        		Integer moduleId = endpoint.getModule().getId();
        		if(ToolUtil.isEmpty(modules.get(moduleId))) {
        			Map <String,Object> module = new HashMap<String,Object>();
        			module.put("id", moduleId);
        			module.put("name", endpoint.getModule().getName());
        			module.put("num", 1);
        			modules.put(moduleId,module);
        		}else {
        			Map<String,Object> module = (Map<String, Object>) modules.get(moduleId);
        			module.put("num", (Integer)module.get("num")+1);
        		}
        		
        		List<MonitorEndpointConfig> configs = (List<MonitorEndpointConfig>) endpoint.getConfigs();
        		String types=null;
        		if(ToolUtil.isNotEmpty(params.get("type"))) {
        			types = (String) params.get("type");
        		}
        		for(MonitorEndpointConfig config : configs) {
        			Integer state = config.getState();
        			String type = config.getType();
        			// endpoint刚新增时 还没有状态需要赋值初始 默认为down 等定时任务完成改变state为正确状态
        			if(ToolUtil.isEmpty(state)) {
        				state = 0;
        			}
					if(ToolUtil.isNotEmpty(types) && !types.contains(type)) {
						continue;
					}
        			if(state > 1) {
        				if(ToolUtil.isEmpty(states.get(config.getType()+2))) {
        					Map<String,Object> stateObject = new HashMap<String,Object>();
        					stateObject.put("id",2);
        					stateObject.put("type", config.getType());
        					stateObject.put("name","suspended");
        					stateObject.put("num",1);
        					states.put(config.getType()+2, stateObject);
        				}else{
        					Map<String,Object> stateObject = (Map<String, Object>) states.get(config.getType()+2);
        					stateObject.put("num", (Integer)stateObject.get("num")+1);
        				}
        			}else {
        				if(ToolUtil.isEmpty(states.get(config.getType()+config.getState()))) {
        					Map<String,Object> stateObject = new HashMap<String,Object>();
        					stateObject.put("id",state);
        					stateObject.put("type", config.getType());
        					stateObject.put("name",ToolUtil.equals(state, 0)?"down":"up");
        					stateObject.put("num",1);
        					states.put(config.getType()+config.getState(), stateObject);
        				}else{
        					Map<String,Object> stateObject = (Map<String, Object>) states.get(config.getType()+config.getState());
        					stateObject.put("num", (Integer)stateObject.get("num")+1);
        				}
        			}
        		}
        	}
        	
        	// 查询所有project module数据
        	Set<Integer> projectIds = projects.keySet();
        	List<MonitorProject> otherProjects = monitorProjectService.list(new QueryWrapper<MonitorProject>().lambda().notIn(MonitorProject::getId, projectIds));
        	for(MonitorProject otherProject : otherProjects) {
        		Map <String,Object> project = new HashMap<String,Object>();
    			project.put("id", otherProject.getId());
    			project.put("name", otherProject.getName());
    			project.put("num", 0);
    			projects.put(otherProject.getId(),project);
        	}
        	
        	Set<Integer> moduleIds = modules.keySet();
        	List<MonitorModule> otherModules = monitorModuleService.list(new QueryWrapper<MonitorModule>().lambda().notIn(MonitorModule::getId, moduleIds));
        	for(MonitorModule otherModule : otherModules) {
        		Map <String,Object> module = new HashMap<String,Object>();
    			module.put("id", otherModule.getId());
    			module.put("name", otherModule.getName());
    			module.put("num", 0);
    			modules.put(otherModule.getId(),module);
        	}
        	
        	// 组装state数据 将返回结果中未涉及的状态添加进去
        	for(int i = 0;i<3;i++) {
        		Object metricsObject = states.get("metrics"+i);
        		if(ToolUtil.isEmpty(metricsObject)) {
        			Map<String,Object> stateObject = new HashMap<String,Object>();
					stateObject.put("id",i);
					stateObject.put("type", "metrics");
					if(i==0) {
						stateObject.put("name","down");
					}else if(i==1) {
						stateObject.put("name","up");
					}else {
						stateObject.put("name","suspended");
					}
					stateObject.put("num",0);
					states.put("metrics"+i, stateObject);
        		}
        		
        		Object logsObject = states.get("logs"+i);
        		if(ToolUtil.isEmpty(logsObject)) {
        			Map<String,Object> stateObject = new HashMap<String,Object>();
					stateObject.put("id",i);
					stateObject.put("type", "logs");
					if(i==0) {
						stateObject.put("name","down");
					}else if(i==1) {
						stateObject.put("name","up");
					}else {
						stateObject.put("name","suspended");
					}
					stateObject.put("num",0);
					states.put("logs"+i, stateObject);
        		}
        	}
        	statistics.put("project", projects.values());
        	statistics.put("module", modules.values());
        	statistics.put("state", states.values());
        	result.put("statistics", statistics);
        }
    }

    private void handleQueryParam(Map<String, Object> params) {
        // ids
        if (ToolUtil.isNotEmpty(params.get("ids"))) {
            params.put("ids", String.valueOf(params.get("ids")).split(StringPool.COMMA));
        }
        // projectIds
        if (ToolUtil.isNotEmpty(params.get("projectIds"))) {
            params.put("projectIds", String.valueOf(params.get("projectIds")).split(StringPool.COMMA));
        }
        // moduleIds
        if (ToolUtil.isNotEmpty(params.get("moduleIds"))) {
            params.put("moduleIds", String.valueOf(params.get("moduleIds")).split(StringPool.COMMA));
        }
        // assetIds
        if (ToolUtil.isNotEmpty(params.get("assetIds"))) {
            params.put("assetIds", String.valueOf(params.get("assetIds")).split(StringPool.COMMA));
        }
        // state
        if (ToolUtil.isNotEmpty(params.get("state"))) {
            String[] states = String.valueOf(params.get("state")).split(StringPool.COMMA);
            params.put("states", states);
            for (String state : states) {
                if ("2".equalsIgnoreCase(state)) {
                    params.put("suspendedStatus", "2");
                    break;
                }
            }
        }

        // type
        if (ToolUtil.isNotEmpty(params.get("type"))) {
            params.put("types", String.valueOf(params.get("type")).split(StringPool.COMMA));
        }
        
        
        // metricsStates
        if (ToolUtil.isNotEmpty(params.get("metricsStates"))) {
        	String[] metricsStates = String.valueOf(params.get("metricsStates")).split(StringPool.COMMA);
            params.put("metricsStates", metricsStates);
            for (String metricsState : metricsStates) {
                if ("2".equalsIgnoreCase(metricsState)) {
                    params.put("suspendedStatus", "2");
                    break;
                }
            }
        }
        
        // logsStates
        if (ToolUtil.isNotEmpty(params.get("logsStates"))) {
        	String[] logsStates = String.valueOf(params.get("logsStates")).split(StringPool.COMMA);
            params.put("logsStates", logsStates);
            for (String logsState : logsStates) {
                if ("2".equalsIgnoreCase(logsState)) {
                    params.put("suspendedStatus", "2");
                    break;
                }
            }
        }
        
    }

    @Override
    public List<MonitorProject> queryEndpointTree(String name) {
        List<MonitorEndpoint> endpointList = this.list(new LambdaQueryWrapper<MonitorEndpoint>().like(StringUtils.isNotEmpty(name), MonitorEndpoint::getName, name));
        if (CollectionUtils.isEmpty(endpointList)) {
            return Collections.emptyList();
        }
        List<Integer> moduleIdList = endpointList.stream().map(MonitorEndpoint::getModuleId).distinct().collect(Collectors.toList());
        List<MonitorModule> moduleList = monitorModuleService.list(new LambdaQueryWrapper<MonitorModule>().in(MonitorModule::getId, moduleIdList));

        List<Integer> projectIdList = moduleList.stream().map(MonitorModule::getProjectId).distinct().collect(Collectors.toList());
        List<MonitorProject> projectList = monitorProjectService.list(new LambdaQueryWrapper<MonitorProject>().in(MonitorProject::getId, projectIdList));


        for (MonitorProject project : projectList) {
            List<MonitorModule> projectModules = new ArrayList<>();

            for (MonitorModule module : moduleList) {
                List<MonitorEndpoint> moduleEndpoints = new ArrayList();
                for (MonitorEndpoint endpoint : endpointList) {
                    if (endpoint.getModuleId().equals(module.getId())) {
                        moduleEndpoints.add(endpoint);
                    }
                }
                module.setChildren(moduleEndpoints);

                if (module.getProjectId().equals(project.getId())) {
                    projectModules.add(module);
                }
            }

            project.setChildren(projectModules);
        }
        return projectList;
    }

    public void saveModuleChartTmpl(MonitorEndpoint endpoint) {
        MonitorModule module = monitorModuleService.getById(endpoint.getModuleId());
        if (ToolUtil.isNotEmpty(module) && StrUtil.isNotBlank(module.getChartIds())) {
            List<String> list = Arrays.asList(module.getChartIds().split(","));
            List<Integer> chartIdList = list.stream().map(Integer::parseInt).collect(Collectors.toList());
            // 补充 group 下的 chart id
            List<VisualChart> childCharts = visualChartService.list(new LambdaQueryWrapper<VisualChart>().in(VisualChart::getGroupId, chartIdList));
            List<Integer> childChartIdList = childCharts.stream().map(VisualChart::getId).distinct().collect(Collectors.toList());
            chartIdList.addAll(childChartIdList);
            chartIdList = chartIdList.stream().distinct().collect(Collectors.toList());
            List<VisualChart> chartList = visualChartService.queryChartWithElementByIds(chartIdList);

            MonitorEndpoint endpointInfo = this.queryEndpointEntity(endpoint.getId());
            VisualPanel endpointPanel = visualPanelService.getOne(new LambdaQueryWrapper<VisualPanel>().eq(VisualPanel::getType, Constant.PanelType.ENDPOINT.getValue()).eq(VisualPanel::getLink, endpoint.getId()));

            // model chart 组 和 asset 新增 chart 组关联关系，用来做 asset 新增 chart 组关系映射
            Map<Integer, Integer> groupIdMapping = new HashMap<>();

            // 按照 group 升序，先添加组
            chartList = chartList.stream().sorted(Comparator.comparing(VisualChart::getGroupId)).collect(Collectors.toList());

            // 渲染新增
            for (VisualChart chart : chartList) {
                VisualChart saveChart = new VisualChart();
                Tool.BeanUtil.copyProperties(chart, saveChart);
                saveChart.setId(null);
                saveChart.setPanelId(endpointPanel.getId());
                saveChart.setUpdateAt(new Date());
                saveChart.setUpdateBy(ShiroUtils.getUserId().intValue());
                saveChart.setPid(chart.getId());

                saveChart.setVarType(1);
                saveChart.setVarId(endpoint.getId());

                Integer newGroupId = groupIdMapping.get(saveChart.getGroupId());
                saveChart.setGroupId(newGroupId == null ? 0 : newGroupId);

                saveChart.setName(Tool.NzExpressionUtil.renderExpression(chart.getName(), endpointInfo));
                saveChart.setParam(ToolUtil.isNotEmpty(chart.getParam()) ? Tool.NzExpressionUtil.renderExpression(chart.getParam().toString(), endpoint) : null);

                saveChart.setSeq(endpoint.getSeq());
                visualChartService.save(saveChart);

                if (Constant.CHART_GROUP_TYPE.equalsIgnoreCase(chart.getType())) {
                    groupIdMapping.put(chart.getId(), saveChart.getId());
                }

                if (CollectionUtils.isNotEmpty(saveChart.getElements())) {
                    for (VisualChartElement chartElement : saveChart.getElements()) {
                        chartElement.setId(null);
                        chartElement.setChartId(saveChart.getId());
                        chartElement.setExpression(ToolUtil.isNotEmpty(chartElement.getExpression()) ? Tool.NzExpressionUtil.renderExpression(chartElement.getExpression(), endpoint) : null);
                        chartElement.setSeq(endpoint.getSeq());
                    }
                    visualChartElementService.saveBatch(saveChart.getElements());
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEndpointByIds(List<Integer> ids) {
        // 删除 endpoint
        this.removeByIds(ids);

        // 删除 endpoint state
        endpointStateService.removeByIds(ids);

        // 删除 alert
        alertMessageService.removeByEndpointIds(ids);

        // 删除关联的 panel chart  chartElement信息
        List<VisualPanel> panels = visualPanelService.list(new LambdaQueryWrapper<VisualPanel>().eq(VisualPanel::getType, Constant.PanelType.ENDPOINT.getValue()).in(VisualPanel::getLink, ids));
        if (CollectionUtils.isNotEmpty(panels)) {
            Set<Integer> panelIds = panels.stream().map(VisualPanel::getId).collect(Collectors.toSet());
            visualPanelService.delPanelsAndChartInfoByPanelIds(panelIds);
        }

        // 删除 关联通过 chart temp 创建的 charts
        visualChartService.remove(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getVarType, 2).in(VisualChart::getVarId, ids));

        // configs
        endpointConfigService.remove(new LambdaQueryWrapper<MonitorEndpointConfig>().in(MonitorEndpointConfig::getEndpointId, ids));
    }

    @Override
    public void getExportTemplateByLanguage(HttpServletResponse response, String language) {
        SysConfigEntity configEntity = sysConfigDao.selectOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_MONITORENDPOINT_EXPORT_HEADER));
        Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);
        Map<String, String> exportHeaderMap;
        if (map.get(language) != null) {
            exportHeaderMap = (Map) map.get(language);
        } else {
            throw new NZException(RCode.ENDPOINT_EXPORT_HEADER_LANGUAGE_ERROR);
        }
        SXSSFWorkbook workbook = null;
        try {
            workbook = ExcelUtils.createExcel07(exportHeaderMap, "Endpoint", true);
            String fileName = "EndpointExportTemplate.xlsx";
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            log.error("endpoint 模板生成失败，错误信息是：" + e);
        } catch (RuntimeException e) {
            log.error("获取 endpoint 导入模板失败，表头语言是：" + language + " ，错误信息是：" + e);
        } finally {
            if (workbook != null) {
                IOUtils.closeQuietly(workbook);
            }
        }
    }

    @Override
    public void cancelImport(String seq) {
        List<MonitorEndpoint> endpointList = this.list(new QueryWrapper<MonitorEndpoint>().lambda().eq(MonitorEndpoint::getSeq, seq));
        List<Integer> endpointIds = endpointList.stream().map(MonitorEndpoint::getId).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(endpointIds)) {
            // 撤销导入
            this.remove(new LambdaQueryWrapper<MonitorEndpoint>().eq(MonitorEndpoint::getSeq, seq));

            // 删除关联的 panel chart  chartElement信息
            List<VisualPanel> panels = visualPanelService.list(new LambdaQueryWrapper<VisualPanel>().eq(VisualPanel::getType, Constant.PanelType.ENDPOINT.getValue()).in(VisualPanel::getLink, endpointIds));
            if (CollectionUtils.isNotEmpty(panels)) {
                Set<Integer> panelIds = panels.stream().map(VisualPanel::getId).collect(Collectors.toSet());
                visualPanelService.delPanelsAndChartInfoByPanelIds(panelIds);
            }

            // configs
            endpointConfigService.remove(new LambdaQueryWrapper<MonitorEndpointConfig>().eq(MonitorEndpointConfig::getSeq, seq));
        }
    }

    @Override
    public Map importEndpoint(MultipartFile excelFile, String language) {
        Workbook work = ExcelUtils.getWorkBookFromMultipartFile(excelFile);

        Sheet sheet = work.getSheetAt(0);

        // 表头校验
        SysConfigEntity config = sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_MONITORENDPOINT_EXPORT_HEADER));
        HashMap<String, Object> map = JSONObject.parseObject(config.getParamValue(), HashMap.class, Feature.OrderedField);
        boolean headerFlag = ExcelUtils.validateExcelHeader(sheet, sheet.getFirstRowNum(), map.values());
        if (!headerFlag) {
            throw new NZException(RCode.EXCELFILE_HEADER_TEMPLATE_ERROR);
        }

        Map<String, String> exportHeader = (Map) map.get(language);
        List<String> headerInfos = new ArrayList(exportHeader.keySet());
        // 预先查询出校验数据 避免循环查表
        List<Map> failDetails = new ArrayList<>();
        String seqUUID = UUID.randomUUID().toString();

        int successNum = this.saveTmplsDataFromSheet(sheet, failDetails, headerInfos, seqUUID);
        int totalNum = sheet.getLastRowNum() - sheet.getFirstRowNum();

        Map resultMap = new HashMap(8);
        resultMap.put("seq", seqUUID);
        resultMap.put("totalNum", totalNum);
        resultMap.put("successNum", successNum);
        resultMap.put("failNum", totalNum - successNum);
        resultMap.put("failDetail", CommonUtils.handleImportFailDetails(failDetails));
        return resultMap;
    }

    @Override
    public void export(HttpServletResponse response, Map<String, Object> params) {

        PageUtils page = (PageUtils) this.queryPage(params).get("page");
        List<MonitorEndpoint> list = (List<MonitorEndpoint>) page.getList();

        SXSSFWorkbook workbook = null;
        try {
            int rowNum = 0;
            SysConfigEntity configEntity = sysConfigDao.selectOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_MONITORENDPOINT_EXPORT_HEADER));
            Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);

            // 添加标题行 默认语言 en
            String language = (String) params.get("language");
            language = ToolUtil.isEmpty(language) ? "en" : language;
            Map<String, String> exportHeader = (Map) map.get(language);

            String sheetName = "Endpoints";
            workbook = ExcelUtils.createExcel07(exportHeader, sheetName, false);
            SXSSFSheet sheet = workbook.getSheet(sheetName);
            CellStyle dataStyle = ExcelUtils.createCellStyle(workbook, (short) 13, false);

            // 添加数据
            Row row1;
            for (MonitorEndpoint endpoint : list) {
                row1 = sheet.createRow(++rowNum);

                MonitorProject project = endpoint.getProject();
                MonitorModule module = endpoint.getModule();
                AssetAsset asset = endpoint.getAsset();

                // project name
                row1.createCell(0).setCellValue(project.getName());
                // module name
                row1.createCell(1).setCellValue(module.getName());
                // name
                row1.createCell(2).setCellValue(endpoint.getName());
                // asset name
                row1.createCell(3).setCellValue(asset.getName());
                // config
                List<MonitorEndpointConfig> configs = (List<MonitorEndpointConfig>) endpoint.getConfigs();
                if (Tool.CollectionUtil.isNotEmpty(configs)) {
                    MonitorEndpointConfig metricsEndpointConf = configs.stream().filter(monitorEndpointConfig -> Constant.MonitorConfType.METRICS.getValue().equals(monitorEndpointConfig.getType())).findFirst().orElse(null);
                    List<Map> tempConfigList = new ArrayList<>();
                    if (metricsEndpointConf != null) {
                        Map metricsMap = new HashMap();
                        metricsMap.put("type", Constant.MonitorConfType.METRICS.getValue());
                        metricsMap.put("config", JSONObject.parseObject(StrUtil.emptyToDefault(metricsEndpointConf.getConfig(), "")));
                        metricsMap.put("enable", metricsEndpointConf.getEnable());
                        tempConfigList.add(metricsMap);
                    }

                    MonitorEndpointConfig logsEndpointConf = configs.stream().filter(monitorEndpointConfig -> Constant.MonitorConfType.LOGS.getValue().equals(monitorEndpointConfig.getType())).findFirst().orElse(null);
                    if (logsEndpointConf != null) {
                        Map logsMap = new HashMap();
                        logsMap.put("type", Constant.MonitorConfType.LOGS.getValue());
                        logsMap.put("config", JSONObject.parseArray(StrUtil.emptyToDefault(logsEndpointConf.getConfig(), "")));
                        logsMap.put("enable", logsEndpointConf.getEnable());
                        tempConfigList.add(logsMap);
                    }

                    row1.createCell(4).setCellValue(JSONObject.toJSONString(tempConfigList));
                }

                // 设置单元格样式
                ExcelUtils.setDataCellStyle(dataStyle, row1, 4);
            }

            // 响应为流
            String fileName = "EndpointsExport.xlsx"; // 默认名称
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            log.error(e);
        } catch (RuntimeException e) {
            log.error(e);
        } finally {
            if (workbook != null) {
                IOUtils.closeQuietly(workbook);
            }
        }
    }

    @Override
    public List<Integer> getSuspendedEndpointIdList(Integer assetId) {
        return monitorEndpointDao.getSuspendedEndpointIdList(assetId);
    }

    @Override
    public List<MonitorEndpoint> getEnableMetricsEndpointInfoListForAgent() {
        List<MonitorEndpoint> enableEndpointInfoList = monitorEndpointDao.getEnableMetricsEndpointInfoListForAgent();
        // 如 asset 存在 parent , 则 set parent
        List<AssetAsset> assetList = assetService.list();
        Map<Integer, AssetAsset> assetIdAndEntityMap = assetList.stream().collect(Collectors.toMap(AssetAsset::getId, Function.identity()));
        for (MonitorEndpoint endpoint : enableEndpointInfoList) {
            AssetAsset relAsset = endpoint.getAsset();
            relAsset.setParent(assetIdAndEntityMap.get(relAsset.getPid()));
        }
        return enableEndpointInfoList;
    }

    @Override
    public List<MonitorEndpoint> getEnableLogsEndpointInfoListForAgent() {
        List<MonitorEndpoint> enableEndpointInfoList = monitorEndpointDao.getEnableLogsEndpointInfoListForAgent();
        // 如 asset 存在 parent , 则 set parent
        List<AssetAsset> assetList = assetService.list();
        Map<Integer, AssetAsset> assetIdAndEntityMap = assetList.stream().collect(Collectors.toMap(AssetAsset::getId, Function.identity()));
        for (MonitorEndpoint endpoint : enableEndpointInfoList) {
            AssetAsset relAsset = endpoint.getAsset();
            relAsset.setParent(assetIdAndEntityMap.get(relAsset.getPid()));
        }
        return enableEndpointInfoList;
    }

    private int saveTmplsDataFromSheet(Sheet sheet, List<Map> failDetails, List<String> headerInfos, String seqUUID) {
        List<MonitorProject> projectList = monitorProjectService.list();
        Map<String, Integer> projectNameAndIdMap = projectList.stream().collect(Collectors.toMap(MonitorProject::getName, MonitorProject::getId));

        List<MonitorModule> moduleList = monitorModuleService.list();
        Map<String, MonitorModule> monitorModuleMap = moduleList.stream().collect(Collectors.toMap(MonitorModule::getName, Function.identity()));

        List<AssetAsset> assets = assetService.list();
        Map<String, Integer> assetNameAndIdMap = assets.stream().collect(Collectors.toMap(AssetAsset::getName, AssetAsset::getId));

        // endpoint chart template
        SysConfigEntity endpointChartConfig = sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_ENDPOINT_CHART_TPL));
        VisualChart chartTemplate = JSONObject.parseObject(endpointChartConfig.getParamValue(), VisualChart.class);

        int successNum = 0;
        Row row;
        for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
            row = sheet.getRow(i);
            if (row == null) continue;

            int dataFromExcelRowNum = i + 1;
            boolean saveFlag = true;
            Integer projectId = null;
            String projectName = ExcelUtils.getCellValue(row.getCell(0));
            if (StringUtils.isEmpty(projectName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.PROJECT_NAME_ISNULL.getMsg());
            } else {
                projectId = projectNameAndIdMap.get(projectName);
                if (projectId == null) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.PROJECT_NOTFOUND.getMsg());
                }
            }
            MonitorModule module = null;
            String moduleName = ExcelUtils.getCellValue(row.getCell(1));
            if (StringUtils.isEmpty(moduleName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.MODULE_NAME_ISNULL.getMsg());
            } else {
                module = monitorModuleMap.get(moduleName);
                if (module == null) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.MODULE_NOTFOUND.getMsg());
                } else {
                    if (projectId != null && !module.getProjectId().equals(projectId)) {
                        module = null;
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.MODULE_NOTEXIS_PROJECT.getMsg());
                    }
                }
            }

            String assetName = ExcelUtils.getCellValue(row.getCell(3));
            if (StringUtils.isEmpty(assetName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.ASSET_NAME_ISNULL.getMsg());
            } else {
                if (ToolUtil.isEmpty(assetNameAndIdMap.get(assetName))) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.ASSET_NOT_EXIST.getMsg());
                }
            }

            String configStr = ExcelUtils.getCellValue(row.getCell(4));
            List<Map> configMapList = new ArrayList<>();
            if (StringUtils.isNotEmpty(configStr)) {
                try {
                    configMapList = JSONObject.parseArray(configStr, Map.class);
                } catch (JSONException e) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.ENDPOINT_CONFIGS_FORMAT.getMsg());
                }

                // 每个 type 最多包含一个
                long countByType = configMapList.stream().map(map -> map.get("type")).distinct().count();
                if (countByType != configMapList.size()) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.ENDPOINT_CONFIGS_TYPE_DUPLICATE.getMsg());
                }

                List<String> configTypeList = Arrays.asList(Constant.MonitorConfType.values()).stream().map(Constant.MonitorConfType::getValue).collect(Collectors.toList());
                for (Map map : configMapList) {
                    if (ToolUtil.isEmpty(map.get("type"))) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.ENDPOINT_TYPE_ISNULL.getMsg());
                    }
                    if (!configTypeList.contains(map.get("type").toString())) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.ENDPOINT_TYPE_INVALID.getMsg());
                    }
                }

                Map metricsConfigMap = configMapList.stream().filter(map -> {
                    String configType = (String) map.get("type");
                    return StrUtil.equals("metrics", configType);
                }).findFirst().orElse(null);
                if (metricsConfigMap != null && (1 == (Integer) ObjectUtil.defaultIfNull(metricsConfigMap.get("enable"), 1))) {
                    Map configMap = (Map) metricsConfigMap.get("config");
                    String protocol = (String) configMap.get("protocol");
                    // 校验 type 类型是否在枚举范围内
                    if (!StrUtil.equals(Constant.MODULE_TYPE_HTTP, protocol) && !StrUtil.equals(Constant.MODULE_TYPE_SNMP, protocol)
                    		&& !StrUtil.equals(Constant.MODULE_TYPE_HTTPS, protocol)) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.ENDPOINT_PROTOCOL_INVALID.getMsg());
                    }

                    Object port = configMap.get("port");
                    if (ToolUtil.isNotEmpty(port)) {
                        if (!CommonUtils.checkPort(port.toString())) {
                            saveFlag = false;
                            this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.MODULE_PORT_ERROR.getMsg());
                        }
                    }

                    Object host = configMap.get("host");
                    if (ToolUtil.isNotEmpty(host)) {
                        if (!CommonUtils.checkIp(host.toString())) {
                            saveFlag = false;
                            this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.ASSET_HOST_FORMAT.getMsg());
                        }
                    }
                    // snmp 类型校验 config
                    if (StrUtil.equals(Constant.MODULE_TYPE_SNMP, protocol)) {
                        // snmpCredentialsId 必须存在
                        Object snmpCredentialsId = configMap.get("snmpCredentialsId");
                        if (ToolUtil.isEmpty(snmpCredentialsId)) {
                            saveFlag = false;
                            this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.SNMP_CREDENTIAL_ID_ISNULL.getMsg());
                        }

                        SnmpCredential snmpCredential = snmpCredentialService.getById(snmpCredentialsId.toString());
                        if (ToolUtil.isEmpty(snmpCredential)) {
                            saveFlag = false;
                            this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.SNMP_CREDENTIAL_NOT_FOUND.getMsg());
                        }

                        Object walk = configMap.get("walk");
                        if (ToolUtil.isEmpty(walk)) {
                            saveFlag = false;
                            this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.ENDPOINT_SNMP_WALK_ISNULL.getMsg());
                        }

                        try {
                            JSONArray.parseArray(JSONObject.toJSONString(walk));
                        } catch (JSONException e) {
                            saveFlag = false;
                            this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.ENDPOINT_SNMP_WALK_FORMAT.getMsg());
                        }
                    }

                    Map<String, Object> labels = MapUtil.defaultIfEmpty((Map) configMap.get("labels"), Collections.emptyMap());
                    for (String labelKey : labels.keySet()) {
                        if (!ReUtil.isMatch(Constant.PROMETHEUS_LABELNAME_EXPRESSION, labelKey)) {
                            saveFlag = false;
                            this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.PROMETHEUS_LABELNAME_FORMAT_ERROR.getMsg());
                        }
                    }

                    List<Map> relabelConfigList = ObjectUtil.defaultIfNull((List) configMap.get("relabel_config"), Collections.emptyList());
                    for (Map map : relabelConfigList) {
                        List<String> sourceLabels = ObjectUtil.defaultIfNull((List) map.get("source_labels"), Collections.emptyList());
                        for (String sourceLabel : sourceLabels) {
                            if (!ReUtil.isMatch(Constant.PROMETHEUS_LABELNAME_EXPRESSION, sourceLabel)) {
                                saveFlag = false;
                                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.PROMETHEUS_LABELNAME_FORMAT_ERROR.getMsg());
                            }
                        }
                        String targetLabel = (String) map.get("target_label");
                        if (StrUtil.isNotEmpty(targetLabel)) {
                            if (!ReUtil.isMatch(Constant.PROMETHEUS_LABELNAME_EXPRESSION, targetLabel)) {
                                saveFlag = false;
                                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.PROMETHEUS_LABELNAME_FORMAT_ERROR.getMsg());
                            }
                        }
                    }
                }
                Map logsConfigMap = configMapList.stream().filter(map -> {
                    String configType = (String) map.get("type");
                    return StrUtil.equals(Constant.MonitorConfType.LOGS.getValue(), configType);
                }).findFirst().orElse(null);
                if (logsConfigMap != null && 1 == (Integer) ObjectUtil.defaultIfNull(logsConfigMap.get("enable"), 1)) {
                    if (ToolUtil.isNotEmpty(logsConfigMap.get("config"))) {
                        List<Map> configMaps = (List<Map>) logsConfigMap.get("config");
                        for (Map configMap : configMaps) {
                            if ("file".equals(configMap.get("type").toString()) && ToolUtil.isEmpty(configMap.get("fileName"))) {
                                saveFlag = false;
                                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.MODULE_CONFIG_FILENAME_ISNULL.getMsg());
                            }
                        }
                    }
                }
            }

            if (saveFlag) {
                Integer assetId = assetNameAndIdMap.get(assetName);
                // 通过 asset name
                if (assetId == null) {
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.ASSET_NOT_EXIST.getMsg());
                    continue;
                }

                MonitorEndpoint monitorEndpoint = new MonitorEndpoint();
                // endpoint asset_id
                monitorEndpoint.setAssetId(assetId);

                AssetAsset assetInfo = assetService.queryAssetInfo(assetId);
                MonitorModule moduleInfo = monitorModuleService.queryModuleInfo(module.getId());
                String name = ExcelUtils.getCellValue(row.getCell(2));
                if (StringUtils.isEmpty(name)) {
                    name = Tool.NzExpressionUtil.renderExpression(module.getEndpointNameTmpl(), assetInfo, moduleInfo);
                }

                int countByName = this.count(new LambdaQueryWrapper<MonitorEndpoint>().eq(MonitorEndpoint::getName, name));
                if (countByName > 0) {
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.ENDPOINT_NAME_DUPLICATE.getMsg());
                    continue;
                }

                monitorEndpoint.setName(name);
                monitorEndpoint.setSeq(seqUUID);
                monitorEndpoint.setModuleId(module.getId());

                // config 如果为空 则设置 module configs
                if (StringUtils.isEmpty(configStr)) {
                    String str = Tool.NzExpressionUtil.renderExpression(module.getConfigs(), assetInfo, moduleInfo);
                    monitorEndpoint.setConfigs(str);
                } else {
                    monitorEndpoint.setConfigs(configStr);
                }
                DefaultTransactionDefinition def = new DefaultTransactionDefinition();
                TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(def);
                try {
                    this.save(monitorEndpoint);
                    // 新增时需要新增 panel chart
                    visualChartService.saveRelPanelAndChart(Constant.PanelType.ENDPOINT.getValue(), monitorEndpoint.getId(), monitorEndpoint.getSeq(), chartTemplate);

                    // 保存配置信息
                    this.saveOrUpdateEndpointConfigs(monitorEndpoint, module);

                    // 新增时同时需要新增 module下chart ids关联的模板
                    this.saveModuleChartTmpl(monitorEndpoint);
                    dataSourceTransactionManager.commit(transactionStatus);
                    successNum++;
                } catch (RuntimeException e) {
                    dataSourceTransactionManager.rollback(transactionStatus);
                    log.error(String.format("endpoint 导入保存失败,表达式信息是：%s,错误信息是：%s", monitorEndpoint.toString(), e.getMessage()), e);
                    this.addFailDetail(dataFromExcelRowNum, failDetails, "System", RCode.ENDPOINT_IMPORT_ERROR.getMsg());
                }
            }
        }
        return successNum;
    }

    private void addFailDetail(int i, List<Map> failDetails, String headerInfo, String msg) {
        Map failDetail = new HashMap(4);
        failDetail.put("lineNo", i);
        failDetail.put("errorMsg", headerInfo + ": " + msg);
        failDetails.add(failDetail);
    }

    @Override
    public List<MonitorEndpoint> endpointDetailList() {
        return monitorEndpointDao.endpointDetailList();
    }

    private void validateEndpointInfo(MonitorEndpoint endpoint) {
        AssetAsset assetById = assetService.getById(endpoint.getAssetId());
        ValidateUtils.is(assetById).notNull(RCode.ASSET_NOT_EXIST);

        MonitorModule moduleById = monitorModuleService.getById(endpoint.getModuleId());
        ValidateUtils.is(moduleById).notNull(RCode.MODULE_NOTFOUND);

        // config 格式校验
        String configStr = (String) endpoint.getConfigs();
        if (StrUtil.isNotEmpty(configStr)) {

            List<Map> configMapList;
            try {
                configMapList = JSONObject.parseArray(configStr, Map.class);
            } catch (JSONException e) {
                throw new NZException(RCode.ENDPOINT_CONFIGS_FORMAT);
            }

            // 每个type 最多包含一个
            long countByType = configMapList.stream().map(map -> map.get("type")).distinct().count();
            if (countByType != configMapList.size()) {
                throw new NZException(RCode.ENDPOINT_CONFIGS_TYPE_DUPLICATE);
            }

            List<String> configTypeList = Arrays.asList(Constant.MonitorConfType.values()).stream().map(Constant.MonitorConfType::getValue).collect(Collectors.toList());
            for (Map map : configMapList) {
                if (ToolUtil.isEmpty(map.get("type"))) {
                    throw new NZException(RCode.ENDPOINT_TYPE_ISNULL);
                }
                if (!configTypeList.contains(map.get("type").toString())) {
                    throw new NZException(RCode.ENDPOINT_TYPE_INVALID);
                }
            }

            Map metricsConfigMap = configMapList.stream().filter(map -> {
                String configType = (String) map.get("type");
                return StrUtil.equals("metrics", configType);
            }).findFirst().orElse(null);

            if (metricsConfigMap != null && (1 == (Integer) ObjectUtil.defaultIfNull(metricsConfigMap.get("enable"), 1))) {
                Map configMap = (Map) metricsConfigMap.get("config");
                String protocol = (String) configMap.get("protocol");
                // 校验 type 类型是否在枚举范围内
                if (!StrUtil.equals(Constant.MODULE_TYPE_HTTP, protocol) && !StrUtil.equals(Constant.MODULE_TYPE_SNMP, protocol)
                		&& !StrUtil.equals(Constant.MODULE_TYPE_HTTPS, protocol)) {
                    throw new NZException(RCode.ENDPOINT_PROTOCOL_INVALID);
                }

                // snmp 类型校验 config
                if (StrUtil.equalsIgnoreCase(Constant.MODULE_TYPE_SNMP, protocol)) {
                    // snmpCredentialsId 必须存在
                    Object snmpCredentialsId = configMap.get("snmpCredentialsId");
                    ValidateUtils.is(snmpCredentialsId).notNull(RCode.SNMP_CREDENTIAL_ID_ISNULL);

                    SnmpCredential snmpCredential = snmpCredentialService.getById(snmpCredentialsId.toString());
                    ValidateUtils.is(snmpCredential).notNull(RCode.SNMP_CREDENTIAL_NOT_FOUND);

                    Object walk = configMap.get("walk");
                    ValidateUtils.is(walk).notNull(RCode.ENDPOINT_SNMP_WALK_ISNULL);

                    try {
                        JSONArray.parseArray(JSONObject.toJSONString(walk));
                    } catch (JSONException e) {
                        throw new NZException(RCode.ENDPOINT_SNMP_WALK_FORMAT);
                    }
                }

                Map<String, Object> labels = MapUtil.defaultIfEmpty((Map) configMap.get("labels"), Collections.emptyMap());
                for (String labelKey : labels.keySet()) {
                    ValidateUtils.is(labelKey).regex(Constant.PROMETHEUS_LABELNAME_EXPRESSION, RCode.PROMETHEUS_LABELNAME_FORMAT_ERROR);
                }

                List<Map> relabelConfigList = ObjectUtil.defaultIfNull((List) configMap.get("relabel_config"), Collections.emptyList());
                for (Map map : relabelConfigList) {
                    List<String> sourceLabels = ObjectUtil.defaultIfNull((List) map.get("source_labels"), Collections.emptyList());
                    for (String sourceLabel : sourceLabels) {
                        ValidateUtils.is(sourceLabel).regex(Constant.PROMETHEUS_LABELNAME_EXPRESSION, RCode.PROMETHEUS_LABELNAME_FORMAT_ERROR);
                    }
                    String targetLabel = (String) map.get("target_label");
                    if (StrUtil.isNotEmpty(targetLabel)) {
                        ValidateUtils.is(targetLabel).regex(Constant.PROMETHEUS_LABELNAME_EXPRESSION, RCode.PROMETHEUS_LABELNAME_FORMAT_ERROR);
                    }
                }
            }

            Map logsConfigMap = configMapList.stream().filter(map -> {
                String configType = (String) map.get("type");
                return StrUtil.equals(Constant.MonitorConfType.LOGS.getValue(), configType);
            }).findFirst().orElse(null);

            if (logsConfigMap != null && 1 == (Integer) ObjectUtil.defaultIfNull(logsConfigMap.get("enable"), 1)) {
                if (ToolUtil.isNotEmpty(logsConfigMap.get("config"))) {
                    List<Map> configMaps = (List<Map>) logsConfigMap.get("config");
                    for (Map configMap : configMaps) {
                        if ("file".equals(configMap.get("type").toString()) && ToolUtil.isEmpty(configMap.get("fileName"))) {
                            throw new NZException(RCode.MODULE_CONFIG_FILENAME_ISNULL);
                        }
                    }
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer saveEndpoint(MonitorEndpoint endpoint) {
        // 校验
        this.validateEndpointInfo(endpoint);

        MonitorModule module = monitorModuleService.getById(endpoint.getModuleId());

        // config 为 null 则使用 module config
        if (StrUtil.isEmpty((String) endpoint.getConfigs())) {
            endpoint.setConfigs(module.getConfigs());
        }

        // 名称为空 则按照 module endpoint 名称模板生成对应名称
        if (ToolUtil.isEmpty(endpoint.getName())) {
            AssetAsset asset = assetService.queryAssetInfo(endpoint.getAssetId());
            MonitorModule module1 = monitorModuleService.queryModuleInfo(endpoint.getModuleId());
            String generateEndpointName = Tool.NzExpressionUtil.renderExpression(module.getEndpointNameTmpl(), asset, module1);
            endpoint.setName(generateEndpointName);
        }

        // 校验名称是否重复
        int countByName = this.count(new LambdaQueryWrapper<MonitorEndpoint>().eq(MonitorEndpoint::getName, endpoint.getName()).eq(MonitorEndpoint::getModuleId, endpoint.getModuleId()));
        if (countByName > 0) {
            throw new NZException(RCode.ENDPOINT_NAME_DUPLICATE);
        }
        // 保存
        this.save(endpoint);

        // 保存配置信息
        this.saveOrUpdateEndpointConfigs(endpoint, module);

        // 新增时需要新增 panel chart
        SysConfigEntity endpointChartConfig = sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_ENDPOINT_CHART_TPL));
        VisualChart chartTemplate = JSONObject.parseObject(endpointChartConfig.getParamValue(), VisualChart.class);
        visualChartService.saveRelPanelAndChart(Constant.PanelType.ENDPOINT.getValue(), endpoint.getId(), endpoint.getSeq(), chartTemplate);

        // 新增时同时需要新增 module下chart ids关联的模板
        this.saveModuleChartTmpl(endpoint);
        return endpoint.getId();
    }

    private void saveOrUpdateEndpointConfigs(MonitorEndpoint endpoint, MonitorModule module) {
        List<MonitorEndpointConfig> configList = endpointConfigService.list(new LambdaQueryWrapper<MonitorEndpointConfig>().eq(MonitorEndpointConfig::getEndpointId, endpoint.getId()));
        AssetAsset asset = assetService.getById(endpoint.getAssetId());
        List<Map> configMapList = JSONObject.parseArray((String) endpoint.getConfigs(), Map.class);
        if (Tool.CollectionUtil.isEmpty(configList)) {
            // 新增
            for (Map configMap : configMapList) {
                MonitorEndpointConfig endpointConfig = new MonitorEndpointConfig();
                endpointConfig.setEndpointId(endpoint.getId());

                if (StrUtil.equals(Constant.MonitorConfType.METRICS.getValue(), configMap.get("type").toString())) {
                    Map map = (Map) configMap.get("config");
                    if (ToolUtil.isEmpty(map.get("port"))) {
                        List<Map> moduleConfigMapList = JSONObject.parseArray(module.getConfigs(), Map.class);
                        Map moduleMetricsMap = moduleConfigMapList.stream().filter(tempMap -> {
                            String configType = (String) tempMap.get("type");
                            return StrUtil.equals(Constant.MonitorConfType.METRICS.getValue(), configType);
                        }).findFirst().orElse(new HashMap());
                        Map moduleConfigMap = ObjectUtil.defaultIfNull((Map) moduleMetricsMap.get("config"), new HashMap());
                        map.put("port", moduleConfigMap.get("port"));
                    }
                    if (ToolUtil.isEmpty(map.get("host"))) {
                        map.put("host", asset.getManageIp());
                    }
                }
                endpointConfig.setConfig(JSONObject.toJSONString(configMap.get("config")));

                Object enable = configMap.get("enable");
                if (ToolUtil.isNotEmpty(enable)) {
                    endpointConfig.setEnable((Integer) enable);
                } else {
                    endpointConfig.setEnable(-1);
                }

                // 计算 endpointId + type + config 的 hash 值，用于确保相同配置不重复
                String hash = endpoint.getId() + endpointConfig.getType() + endpointConfig.getConfig();
                endpointConfig.setHash(String.valueOf(Math.abs(MurmurHash.hash64(hash))));
                endpointConfig.setSeq(endpoint.getSeq());
                endpointConfig.setType(configMap.get("type").toString());
                endpointConfigService.save(endpointConfig);
            }
        } else {
            // 修改
            for (MonitorEndpointConfig endpointConfig : configList) {
                String type = endpointConfig.getType();
                Map configByType = configMapList.stream().filter(map -> {
                    String configType = (String) map.get("type");
                    return StrUtil.equals(type, configType);
                }).findFirst().orElse(new HashMap());

                Object enable = configByType.get("enable");
                if (ToolUtil.isNotEmpty(enable)) {
                    endpointConfig.setEnable((Integer) enable);
                } else {
                    endpointConfig.setEnable(-1);
                }

                // 更新中未找到该类型配置 更新状态为未启用状态
                if (Tool.CollectionUtil.isEmpty(configByType)) {
                    endpointConfig.setEnable(-1);
                    endpointConfig.setConfig("{}");
                } else {
                    endpointConfig.setConfig(JSONObject.toJSONString(configByType.get("config")));
                    endpointConfig.setVersion(endpointConfig.getVersion() + 1);
                }
                endpointConfigService.updateById(endpointConfig);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer updateEndpoint(MonitorEndpoint endpoint) {
        // 校验
        this.validateEndpointInfo(endpoint);

        MonitorModule module = monitorModuleService.getById(endpoint.getModuleId());

        // config 为 null 则使用 module config
        if (StrUtil.isEmpty((String) endpoint.getConfigs())) {
            endpoint.setConfigs(module.getConfigs());
        }

        // 名称为空 则按照 module endpoint 名称模板生成对应名称
        if (ToolUtil.isEmpty(endpoint.getName())) {
            AssetAsset asset = assetService.queryAssetInfo(endpoint.getAssetId());
            MonitorModule module1 = monitorModuleService.queryModuleInfo(endpoint.getModuleId());
            String generateEndpointName = Tool.NzExpressionUtil.renderExpression(module.getEndpointNameTmpl(), asset, module1);
            endpoint.setName(generateEndpointName);
        }

        // 校验名称是否重复
        List<MonitorEndpoint> list = this.list(new LambdaQueryWrapper<MonitorEndpoint>().eq(MonitorEndpoint::getName, endpoint.getName()).eq(MonitorEndpoint::getModuleId, endpoint.getModuleId()));
        boolean flag = false;
        // 当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) || (list != null && list.size() == 1 && list.get(0).getId().equals(endpoint.getId()))) {
            flag = true;
        }

        if (!flag) {
            throw new NZException(RCode.ENDPOINT_NAME_DUPLICATE);
        }

        // 保存
        this.updateById(endpoint);

        // 保存配置信息
        this.saveOrUpdateEndpointConfigs(endpoint, module);
        return endpoint.getId();
    }

    @Override
    public Map saveBatchEndpoint(List<MonitorEndpoint> endpointList) {
        List<Map> failDetails = new ArrayList<>();
        String seqUUID = UUID.randomUUID().toString();

        int successNum = 0;
        for (int i = 0; i < endpointList.size(); i++) {
            try {
                MonitorEndpoint endpoint = endpointList.get(i);
                ValidateUtils.is(endpoint.getName()).notNull(RCode.ENDPOINT_NAME_ISNULL).maxLength(128)
                        .and(endpoint.getModuleId()).notNull(RCode.ENDPOINT_MODULEID_ISNULL)
                        .and(endpoint.getAssetId()).notNull(RCode.ENDPOINT_ASSETID_ISNULL);

                endpoint.setSeq(seqUUID);
                // 保存
                this.saveEndpoint(endpoint);
                successNum++;
            } catch (Exception e) {
                log.error(e);
                Map failDetail = new HashMap(4);
                failDetail.put("lineNo", i + 1);
                String errorMsg = e instanceof NZException ? ((NZException) e).getMsg() : e.getMessage();
                failDetail.put("errorMsg", errorMsg);
                failDetails.add(failDetail);
            }
        }
        int totalNum = endpointList.size();

        Map resultMap = new HashMap(8);
        resultMap.put("seq", seqUUID);
        resultMap.put("totalNum", totalNum);
        resultMap.put("successNum", successNum);
        resultMap.put("failNum", totalNum - successNum);
        resultMap.put("failDetail", failDetails);
        return resultMap;
    }

    @Override
    public List<Map> render(Map<String, String> params) {
        List<Integer> moduleIds = Arrays.stream(params.get("moduleIds").split(StringPool.COMMA)).map(Integer::parseInt).collect(Collectors.toList());
        List<Integer> assetIds = Arrays.stream(params.get("assetIds").split(StringPool.COMMA)).map(Integer::parseInt).collect(Collectors.toList());

        List<Map> resultMapList = new ArrayList<>();
        for (Integer moduleId : moduleIds) {
            MonitorModule moduleDetail = monitorModuleService.queryModuleInfo(moduleId);
            if (moduleDetail == null) throw new NZException(RCode.MODULE_NOTFOUND);
            for (Integer assetId : assetIds) {
                AssetAsset assetDetail = assetService.queryAssetInfo(assetId);
                if (assetDetail == null) throw new NZException(RCode.ASSET_NOT_EXIST);

                Map tempMap = new HashMap(8);
                String endpointNameExpression = moduleDetail.getEndpointNameTmpl();
                String endpointConfigsExpression = moduleDetail.getConfigs();

                tempMap.put("name", Tool.NzExpressionUtil.renderExpression(endpointNameExpression, moduleDetail, assetDetail));
                tempMap.put("configs", Tool.NzExpressionUtil.renderExpression(endpointConfigsExpression, moduleDetail, assetDetail));
                tempMap.put("module", moduleDetail);
                tempMap.put("asset", assetDetail);
                tempMap.put("project", monitorProjectService.getById(moduleDetail.getProjectId()));
                resultMapList.add(tempMap);
            }
        }
        return resultMapList;
    }
}
