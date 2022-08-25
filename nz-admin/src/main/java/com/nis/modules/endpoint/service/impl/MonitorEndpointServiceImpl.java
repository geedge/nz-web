package com.nis.modules.endpoint.service.impl;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.lang.hash.MurmurHash;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.smartvalidate.utils.CommonUtil;
import com.nis.common.utils.*;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.AgentService;
import com.nis.modules.agent.service.PromApiService;
import com.nis.modules.agent.util.AgentUtil;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetTalonStatus;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetTalonStatusService;
import com.nis.modules.endpoint.dao.MonitorEndpointDao;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.entity.MonitorEndpointConfig;
import com.nis.modules.endpoint.entity.MonitorEndpointImportEnum;
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
import com.nis.modules.stat.service.StatService;
import com.nis.modules.sys.entity.SnmpCredential;
import com.nis.modules.sys.entity.SysUserStarredEntity;
import com.nis.modules.sys.service.*;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MonitorEndpointServiceImpl extends ServiceImpl<MonitorEndpointDao, MonitorEndpoint> implements MonitorEndpointService {

    private static Log log = Log.get();

    @Autowired
    private AssetAssetService assetService;

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
    private StatService statService;

    @Autowired
    private BasicImportAndExportServices basicImportAndExportServices;

    @Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SysUserStarredService starredService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private AssetTalonStatusService assetTalonService;

    @Autowired
    private SysI18nService i18nService;

    @Autowired
    private PromApiService promApiService;

    @Autowired
    private SysConfService sysConfService;
    
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

        List<AlertMessageEntity> alertMessageList = alertMessageService.list(new LambdaQueryWrapper<AlertMessageEntity>().eq(AlertMessageEntity::getEndpointId, endpoint.getId()));
        endpoint.setAlertNum(alertMessageList.size());

        Map<Integer, Long> ruleIdAndAlertCountMap = alertMessageList.parallelStream().collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
        List<Map<String, Object>> alert = statService.getSeverityStatByRuleMsgCount(Tool.MapUtil.empty(), ruleIdAndAlertCountMap);
        endpoint.setAlert(alert);

        endpoint.setConfigs(getEndpointConfigs(endpoint));


        try{
            // 当前用户收藏状态  1：收藏  0：未收藏
            int count = starredService.count(new LambdaQueryWrapper<SysUserStarredEntity>()
                    .eq(SysUserStarredEntity::getTid, endpoint.getId())
                    .eq(SysUserStarredEntity::getType, "endpoint")
                    .eq(SysUserStarredEntity::getUid, ShiroUtils.getUserId().intValue())
            );
            endpoint.setStarred(count == 0 ? 0 : 1);
        }catch (UnavailableSecurityManagerException e){
            // 快照线程无法获取ShiroSession
        }
        // 获取agent信息
        Map endpointProm = redisTemplate.opsForHash().entries(Constant.REDIS_KEY_ENDPOINT_PROM);
        if(ToolUtil.isNotEmpty(endpointProm)) {
            String agentId = ToolUtil.toStr(endpointProm.get("" + id));
            if(StrUtil.isNotBlank(agentId))
                endpoint.setAgent(agentService.getById(Integer.valueOf(agentId)));
        }
        return endpoint;
    }

    // 获取 endpointConfigs 及其 state
    private List<MonitorEndpointConfig> getEndpointConfigs(MonitorEndpoint endpoint){
        Map endpointMetricsStates = redisTemplate.opsForHash().entries(Constant.ENDPOINT_METRICS_STATE_CACHE_KEY);
        Map endpointLogsStates = redisTemplate.opsForHash().entries(Constant.ENDPOINT_LOGS_STATE_CACHE_KEY);

        List<MonitorEndpointConfig> endpointConfigs = endpointConfigService.list(new LambdaQueryWrapper<MonitorEndpointConfig>().eq(MonitorEndpointConfig::getEndpointId, endpoint.getId()));
        for (MonitorEndpointConfig endpointConfig : endpointConfigs) {
            endpointConfig.setVer(endpointConfig.getVersion());

            MonitorEndpointState endpointStateByConfigId = null;
            if(endpointConfig.getType().equals(Constant.MonitorConfType.METRICS.getValue())) {
                if(ToolUtil.isNotEmpty(endpointMetricsStates)) {
                    Object object = endpointMetricsStates.get(endpointConfig.getEndpointId().toString());
                    if(ToolUtil.isNotEmpty(object)) {
                        endpointStateByConfigId = JSON.parseObject((String)object,MonitorEndpointState.class);
                    }
                }
            }else{
                if(ToolUtil.isNotEmpty(endpointLogsStates)) {
                    Object object = endpointLogsStates.get(endpointConfig.getEndpointId().toString());
                    if(ToolUtil.isNotEmpty(object)) {
                        endpointStateByConfigId = JSON.parseObject((String)object,MonitorEndpointState.class);
                    }
                }
            }
            // 缓存中未取到值时 从数据库中获取
            if(endpointStateByConfigId == null) {
                endpointStateByConfigId = endpointStateService.getOne(new LambdaQueryWrapper<MonitorEndpointState>().eq(MonitorEndpointState::getConfigId, endpointConfig.getId()));
            }
            if (endpointStateByConfigId == null)
                continue;
            endpointConfig.setState(endpointStateByConfigId.getState());
            endpointConfig.setLastUpdate(endpointStateByConfigId.getTime());
        }
        return endpointConfigs;
    }

    @Override
    public Map<String,Object> queryPage(Map<String, Object> params) {
    	Map<String,Object> result = new HashMap<String,Object>();
        // 处理请求参数
        this.handleQueryParam(params);

        //当前用户id
        params.put("uid",ShiroUtils.getUserId().intValue());

        IPage page = new Query(MonitorEndpoint.class).getPage(params);
        List<MonitorEndpoint> datas = monitorEndpointDao.queryPage(page, params);

        // 查询结果不为空时处理返回结果
        if (CollectionUtils.isNotEmpty(datas)) {
            this.handleResuleList(datas);
        }

        page.setRecords(datas);
        PageUtils pageUtils = new PageUtils(page);
        
        result.put("page", pageUtils);
        return result;
    }

    @Override
	public Map<String, List> queryEndpointGroupBy(Map<String, Object> params) {
    	Map<String,List> result = new HashMap<String, List>();
    	QueryWrapper wrapper = new QueryWrapper().select("id","name");
		wrapper.orderByAsc("name");
    	//project
    	List<Map<String, Object>> projectList = monitorProjectService.listMaps(wrapper);
    	List<Map<String,Object>> projectGroupList = this.baseMapper.queryEndpointGroupBy("p.id", params);
    	this.assetGroupBysetNum(projectList, projectGroupList);
    	result.put("project", projectList);
    	//module
    	List<Map<String, Object>> moduleList = monitorModuleService.listMaps(wrapper);
    	List<Map<String,Object>> moduleGroupList = this.baseMapper.queryEndpointGroupBy("m.id", params);
    	this.assetGroupBysetNum(moduleList, moduleGroupList);
    	result.put("module", moduleList);
    	
    	//state
    	Map<String,Map<String,Object>> stateMap =new HashMap<String,Map<String,Object>>();
    	Map<String,Object> metricsDownMap =new HashMap<String,Object>();
		metricsDownMap.put("id", 0);
		metricsDownMap.put("name", "down");
		metricsDownMap.put("type", "metrics");
		metricsDownMap.put("num", 0);
		stateMap.put("metricsDownMap", metricsDownMap);

		Map<String,Object> logsDownMap =new HashMap<String,Object>();
		logsDownMap.put("id", 0);
		logsDownMap.put("name", "down");
		logsDownMap.put("type", "logs");
		logsDownMap.put("num", 0);
		stateMap.put("logsDownMap", logsDownMap);

		Map<String,Object> metricsUpMap =new HashMap<String,Object>();
		metricsUpMap.put("id",1);
		metricsUpMap.put("name", "up");
		metricsUpMap.put("type", "metrics");
		metricsUpMap.put("num", 0);
		stateMap.put("metricsUpMap", metricsUpMap);

		Map<String,Object> logsUpMap =new HashMap<String,Object>();
		logsUpMap.put("id",1);
		logsUpMap.put("name", "up");
		logsUpMap.put("type", "logs");
		logsUpMap.put("num", 0);
		stateMap.put("logsUpMap", logsUpMap);

		Map<String,Object> metricsSuspendedMap =new HashMap<String,Object>();
		metricsSuspendedMap.put("id",2);
		metricsSuspendedMap.put("name", "suspended");
		metricsSuspendedMap.put("type", "metrics");
		metricsSuspendedMap.put("num", 0);
		stateMap.put("metricsSuspendedMap", metricsSuspendedMap);

		Map<String,Object> logsSuspendedMap =new HashMap<String,Object>();
		logsSuspendedMap.put("id",2);
		logsSuspendedMap.put("name", "suspended");
		logsSuspendedMap.put("type", "logs");
		logsSuspendedMap.put("num", 0);
		stateMap.put("logsSuspendedMap", logsSuspendedMap);

    	List<Map<String,Object>> stateList = new ArrayList<Map<String,Object>>();
    	List<Map<String,Object>> stateGroupList = this.baseMapper.queryEndpointGroupBy("e.state", params);
    	for(Map<String,Object> state : stateGroupList) {
            Object objVal = Tool.ObjectUtil.defaultIfNull(state.get("state"), 0);
            Long val = Long.valueOf(objVal.toString());
    		if(val.equals(0L)) {
    			stateMap.get("metricsDownMap").put("num", state.get("metricsDown"));
    			stateMap.get("logsDownMap").put("num", state.get("logsDown"));
    		}else if(val.equals(1L)){
    			stateMap.get("metricsUpMap").put("num", state.get("metricsUp"));
    			stateMap.get("logsUpMap").put("num", state.get("logsUp"));
    		}else {
    			stateMap.get("metricsSuspendedMap").put("num", state.get("metricsSuspended"));
    			stateMap.get("logsSuspendedMap").put("num", state.get("logsSuspended"));
    		}
    	}

    	for(Map<String, Object> value :stateMap.values()) {
    		stateList.add(value);
    	}

    	result.put("state", stateList);
		return result;
	}

    /**
     * endpoint配置同步接口
     * 根据参数查询符合条件的 endpoint，再根据module模板信息重新渲染endpoint配置
     * 当根据module模板生成的 endpoint 配置，缺少必填项时，以原endpoint 配置中内容为准
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncTmpl(List<Integer> moduleIds, List<Integer> endpointIds, List<Integer> assetIds) {
        LambdaQueryWrapper<MonitorEndpoint> queryWrapper = new QueryWrapper<MonitorEndpoint>().lambda();
        if(!CommonUtil.isNull(assetIds)){
            queryWrapper.in(MonitorEndpoint::getAssetId, assetIds);
        }
        if(!CommonUtil.isNull(moduleIds)){
            queryWrapper.in(MonitorEndpoint::getModuleId, moduleIds);
        }
        if(!CommonUtil.isNull(endpointIds)){
            queryWrapper.in(MonitorEndpoint::getId, endpointIds);
        }
        List<MonitorEndpoint> endpointList = this.list(queryWrapper);
        if(Tool.CollUtil.isEmpty(endpointList)){
            throw new NZException(RCode.ENDPOINT_NOTFOUND);
        }

        endpointList.forEach(endpoint -> {
            // 查询模板变量
            MonitorEndpoint endpointInfo = this.queryEndpointEntity(endpoint.getId());
            AssetAsset assetInfo = assetService.queryAssetInfo(endpoint.getAssetId());
            MonitorModule module = endpointInfo.getModule();
            // 以module模板生成config
            List<Map> configMapList = JSONObject.parseArray(Tool.NzExpressionUtil.renderExpression(module.getConfigs(), endpointInfo, assetInfo), Map.class);
            // 原endpointConfig
            List<MonitorEndpointConfig> configList = (List<MonitorEndpointConfig>) endpointInfo.getConfigs();
            // 对新生成的配置进行必填项的校验，空或表达式替换为原来的配置
            configMapList.forEach(configMap -> {
                String type = String.valueOf(configMap.get("type"));
                MonitorEndpointConfig oldEndpointConfig = configList.stream().filter(endpointConfig -> {
                    return StrUtil.equals(type, endpointConfig.getType());
                }).findFirst().orElse(new MonitorEndpointConfig());
                switch (type){
                    case "logs":
                        List<Map> logConfigMapList = logsDisplaceVal(JSONObject.parseArray(String.valueOf(configMap.get("config")), Map.class),JSONObject.parseArray(oldEndpointConfig.getConfig(), Map.class));
                        configMap.put("config",logConfigMapList);
                        break;
                    case "metrics":
                        JSONObject metricsConfig = metricsDisplaceVal(JSONObject.parseObject(String.valueOf(configMap.get("config"))), JSONObject.parseObject(oldEndpointConfig.getConfig()));
                        configMap.put("config",metricsConfig);
                        break;
                    default:break;
                }
            });
            endpoint.setConfigs(JSONObject.toJSONString(configMapList));
            this.saveOrUpdateEndpointConfigs(endpoint,module);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bulkEdit(Map<String, Object> params) {
        Integer editType = (Integer) params.get("editType"),enable = null;
        List<Integer> ids = (List<Integer>) params.get("ids");
        List<Map> labels = null;
        if(editType == 1){
            ValidateUtils.is(params.get("labels")).notNull(RCode.ENDPOINT_LABELS_ISNULL);
            labels = (List<Map>) params.get("labels");
        } else {
            if (!Arrays.asList(new Integer[]{0, 1}).contains(params.get("enable"))) {
                throw new NZException(RCode.ENDPOINT_ENABLED_INVALIDE);
            }
            enable = (Integer) params.get("enable");
        }

        List<MonitorEndpointConfig> endpointConfigList = endpointConfigService.list(new QueryWrapper<MonitorEndpointConfig>().lambda()
                .eq(MonitorEndpointConfig::getType, editType==3 ? "logs" : "metrics")
                .in(MonitorEndpointConfig::getEndpointId, ids));
        for (MonitorEndpointConfig endpointConfig : endpointConfigList) {
            switch (editType) {
                case 1: // metric label
                    JSONObject config = JSONObject.parseObject(endpointConfig.getConfig());
                    JSONObject configlabels = config.getJSONObject("labels");
                    for(Map label : labels) {
                        ValidateUtils.is(label.get("action")).notNull(RCode.ENDPOINT_LABELS_ACTION_ISNULL)
                                .and(label.get("key")).notNull(RCode.ENDPOINT_LABELS_KEY_ISNULL);
                        Integer action = (Integer) label.get("action");
                        if (ToolUtil.equals(action, 0)) {
                            configlabels.put(ToolUtil.toStr(label.get("key")), label.get("value"));
                        } else if (ToolUtil.equals(action, 1)) {
                            configlabels.remove(label.get("key"));
                        } else {
                            throw new NZException(RCode.ENDPOINT_LABELS_ACTION_INVALIDE);
                        }
                    }
                    endpointConfig.setConfig(config.toJSONString());
                    break;
                case 2 : // metric enable
                    endpointConfig.setEnable(enable);
                    // 启用时需进行校验
                    if (ToolUtil.equals(1, enable)) {
                        this.validateEndpointMetrics(Tool.BeanUtil.beanToMap(endpointConfig));
                        // 当 metric host为空时，用 asset manageIp填充
                        config = JSONObject.parseObject(endpointConfig.getConfig());
                        if (ToolUtil.isEmpty(config.get("host"))) {
                            MonitorEndpoint monitorEndpoint = this.getById(endpointConfig.getEndpointId());
                            AssetAsset asset = assetService.getById(monitorEndpoint.getAssetId());
                            config.put("host", asset.getManageIp());
                            endpointConfig.setConfig(config.toJSONString());
                        }
                    }
                    break;
                case 3 : // log enable
                    endpointConfig.setEnable(enable);
                    // 启用时需进行校验
                    if (ToolUtil.equals(1, enable)) {
                        this.validateEndpointLogs(Tool.BeanUtil.beanToMap(endpointConfig));
                    }
                break;
            }
        }
        endpointConfigService.updateBatchById(endpointConfigList);
    }

    /**
     * logs必填项校验
     * @param logConfigMapList module模板生成的logs配置
     * @param oldLogConfigList 原endpoint logs配置
     * @return
     */
    private List<Map> logsDisplaceVal(List<Map> logConfigMapList, List<Map> oldLogConfigList){
        for (Map logConfigMap : logConfigMapList) {
            String logType = String.valueOf(logConfigMap.get("type"));
            Map oldLogConfigMap = oldLogConfigList.stream().filter(oldLog -> {
                return StrUtil.equals(logType, String.valueOf(oldLog.get("type")));
            }).findFirst().orElse(new HashMap());
            // Basic
            if(StrUtil.equals("file", logType)){
                displaceVal(logConfigMap,oldLogConfigMap,"fileName");
            }else if(StrUtil.equals("journal", logType)){
                displaceVal(logConfigMap,oldLogConfigMap,"unit");
            }else if(StrUtil.equals("syslog", logType)){
                displaceVal(logConfigMap,oldLogConfigMap,"appName");
                displaceVal(logConfigMap,oldLogConfigMap,"listenAddress");
            }
            // Labels
            String logLabelsStr = String.valueOf(logConfigMap.get("labels"));
            if(StrUtil.isNotBlank(logLabelsStr) && !StrUtil.equals(logLabelsStr, "{}")){
                Map logLabels = displaceJson(JSONObject.parseObject(logLabelsStr, Map.class),JSONObject.parseObject(String.valueOf(oldLogConfigMap.get("labels")), Map.class));
                logConfigMap.put("labels", logLabels);
            }
            // Pipeline
            if(StrUtil.isNotBlank(String.valueOf(logConfigMap.get("pipeline")))){
                List<Map> pipelineList = JSONObject.parseArray(String.valueOf(logConfigMap.get("pipeline")), Map.class);
                List<Map> oldPipelineList = JSONObject.parseArray(String.valueOf(oldLogConfigMap.get("pipeline")), Map.class);
                List<Map> newpipelineList = new ArrayList<>();
                Map oldPipelineMap = new HashMap();
                for (Map pipeline: pipelineList) {
                    String pipelType = String.valueOf(pipeline.get("type"));
                    if(oldPipelineList != null){
                        oldPipelineMap = oldPipelineList.stream().filter(oldLog -> {
                            return StrUtil.equals(pipelType, String.valueOf(oldLog.get("type")));
                        }).findFirst().orElse(new HashMap());
                    }
                    if(StrUtil.equals("regex", pipelType)){
                        displaceVal(pipeline,oldPipelineMap,"expression");
                    }else if(StrUtil.equals("template", pipelType)){
                        displaceVal(pipeline,oldPipelineMap,"template");
                    }else if(StrUtil.equals("timestamp", pipelType)){
                        displaceVal(pipeline,oldPipelineMap,"format");
                    }else if(StrUtil.equals("json", pipelType)){
                        Map expressions = displaceJson(JSONObject.parseObject(String.valueOf(pipeline.get("expressions")), Map.class),JSONObject.parseObject(String.valueOf(oldPipelineMap.get("expressions")), Map.class));
                        pipeline.put("expressions", expressions);
                    }
                    if(!StrUtil.equals("labels", pipelType)){
                        displaceVal(pipeline,oldPipelineMap,"source");
                    }
                    newpipelineList.add(pipeline);
                }
                logConfigMap.put("pipeline",newpipelineList);
            }
        }
        return logConfigMapList;
    }

    /**
     * metrics必填项校验
     * @param metricsConfig module模板生成的metrics配置
     * @param oldMetricsConfig 原endpoint metrics配置
     * @return
     */
    private JSONObject metricsDisplaceVal(JSONObject metricsConfig, JSONObject oldMetricsConfig){
        // Basic
        if(StrUtil.isBlank(metricsConfig.getString("port")) || correctVal(metricsConfig.getString("port"))){
            metricsConfig.put("port",oldMetricsConfig.getInteger("port"));
        }
        if(StrUtil.isBlank(metricsConfig.getString("host")) || correctVal(metricsConfig.getString("host"))){
            metricsConfig.put("host",oldMetricsConfig.getString("host"));
        }
        // Metric relabel
        if(StrUtil.isNotBlank(metricsConfig.getString("relabel_config"))){
            List<Map> relabelConfig = JSONObject.parseArray(metricsConfig.getString("relabel_config"), Map.class);
            List<Map> newRelabelConfig = new ArrayList<>();
            for (Map abelConfig: relabelConfig) {
                String action = String.valueOf(abelConfig.get("action"));
                if(StrUtil.isNotBlank(action)){
                    Map oldAbelConfigMap = null;
                    if(StrUtil.isNotBlank(String.valueOf(oldMetricsConfig.get("relabel_config")))){
                        List<Map> relabelConfigMapList = JSONObject.parseArray(String.valueOf(oldMetricsConfig.get("relabel_config")), Map.class);
                        oldAbelConfigMap = relabelConfigMapList.stream().filter(oldAbelConfig -> {
                            return StrUtil.equals(action, String.valueOf(oldAbelConfig.get("action")));
                        }).findFirst().orElse(null);
                    }
                    if(StrUtil.equals("replace",action) || StrUtil.equals("keep",action) || StrUtil.equals("drop",action)){
                        displaceVal(abelConfig,oldAbelConfigMap,"source_labels");
                    }
                    if(StrUtil.equals("replace",action)){
                        displaceVal(abelConfig,oldAbelConfigMap,"target_label");
                    }
                    displaceVal(abelConfig,oldAbelConfigMap,"regex");
                    if(StrUtil.equals("replace",action) || StrUtil.equals("labelmap",action)){
                        displaceVal(abelConfig,oldAbelConfigMap,"replacement");
                    }
                    newRelabelConfig.add(abelConfig);
                }
            }
            metricsConfig.put("relabel_config",newRelabelConfig);
        }
        // Authentication
        if(StrUtil.isNotBlank(metricsConfig.getString("basic_auth"))){
            Map basicAuth = JSONObject.parseObject(metricsConfig.getString("basic_auth"), Map.class);
            Map oldBasicAuth = JSONObject.parseObject(oldMetricsConfig.getString("basic_auth"), Map.class);
            displaceVal(basicAuth,oldBasicAuth,"username");
            displaceVal(basicAuth,oldBasicAuth,"password");
            metricsConfig.put("basic_auth",basicAuth);
        }
        if(correctVal(metricsConfig.getString("bearer_token"))){
            metricsConfig.put("bearer_token",oldMetricsConfig.getString("bearer_token"));
        }
        return metricsConfig;
    }

    /**
     * 值是否为空或未被替换的表达式
     */
    private boolean correctVal(Object obj){
        String str = String.valueOf(obj);
        return (StrUtil.isBlank(str) || StrUtil.contains(str,"{{") || StrUtil.equals(str,"{}") || StrUtil.equals(str,"[]"));
    }

    /**
     * 置换值为原配置
     */
    private boolean displaceVal(Map abelConfig,Map oldAbelConfigMap,String key){
        if(correctVal(abelConfig.get(key))) {
            if (oldAbelConfigMap == null) {
                return true;
            }
            Object o = oldAbelConfigMap.get(key);
            if (ToolUtil.isEmpty(o)) {
                abelConfig.put(key, "");
            } else {
                abelConfig.put(key, o);
            }
        }
        return false;
    }

    /**
     * 置换JSON值为原配置
     */
    private Map<String, Object> displaceJson(Map<String, Object> map,Map oldMap){
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if(correctVal(entry.getValue())){
                if(oldMap != null && ToolUtil.isNotEmpty(oldMap.get(entry.getKey()))) {
                    map.put(entry.getKey(), oldMap.get(entry.getKey()));
                }else{
                    map.put(entry.getKey(), "");
                }
            }
        }
        return map;
    }

    private void assetGroupBysetNum(List<Map<String,Object>> allList,List<Map<String,Object>> groupList) {
    	if(Tool.CollUtil.isEmpty(groupList)) {
    		return;
    	}
    	Map<String,Object> groupMap = new HashMap<String, Object>();
    	for(Map<String,Object> m : groupList) {
    		groupMap.put(String.valueOf(m.get("id")), m.get("num"));
    	}
    	for(Map<String,Object> m : allList) {
    		m.put("num", groupMap.getOrDefault(String.valueOf(m.get("id")), 0));
    	}
    }

    @SuppressWarnings("unlikely-arg-type")
	private void handleResuleList(List<MonitorEndpoint> endpointList) {
        List<MonitorEndpointConfig> endpointConfigs = endpointConfigService.list();

        Map endpointMetricsStates = redisTemplate.opsForHash().entries(Constant.ENDPOINT_METRICS_STATE_CACHE_KEY);
        Map endpointLogsStates = redisTemplate.opsForHash().entries(Constant.ENDPOINT_LOGS_STATE_CACHE_KEY);
        // 获取agent
        Map endpointProm = redisTemplate.opsForHash().entries(Constant.REDIS_KEY_ENDPOINT_PROM);
        List<Agent> agentList = agentService.list();
        Map<Integer, Agent> agentMap = agentList.stream().collect(Collectors.toMap(Agent::getId, o -> o));

        for (MonitorEndpoint endpoint : endpointList) {
            List<MonitorEndpointConfig> configsByEndpointId = endpointConfigs.stream().filter(endpointConfig -> endpoint.getId().equals(endpointConfig.getEndpointId())).collect(Collectors.toList());
            for (MonitorEndpointConfig endpointConfig : configsByEndpointId) {
                endpointConfig.setVer(endpointConfig.getVersion());

                MonitorEndpointState endpointStateByConfigId = null;
                if(endpointConfig.getType().equals(Constant.MonitorConfType.METRICS.getValue())) {
                	if(ToolUtil.isNotEmpty(endpointMetricsStates)) {
                		Object object = endpointMetricsStates.get(endpointConfig.getEndpointId().toString());
                    	if(ToolUtil.isNotEmpty(object)) {
                    		endpointStateByConfigId = JSON.parseObject((String)object,MonitorEndpointState.class);
                        }
                	}
                }else {
                	if(ToolUtil.isNotEmpty(endpointLogsStates)) {
                		Object object = endpointLogsStates.get(endpointConfig.getEndpointId().toString());
                    	if(ToolUtil.isNotEmpty(object)) {
                    		endpointStateByConfigId = JSON.parseObject((String)object,MonitorEndpointState.class);
                        }
                	}
                }
                // 缓存中未取到值时 从数据库中获取
                if(endpointStateByConfigId == null) {
                    endpointStateByConfigId = endpointStateService.getOne(new LambdaQueryWrapper<MonitorEndpointState>().eq(MonitorEndpointState::getConfigId, endpointConfig.getId()));
                }
                if (endpointStateByConfigId == null)
                    continue;
                endpointConfig.setState(endpointStateByConfigId.getState());
                endpointConfig.setLastUpdate(endpointStateByConfigId.getTime());
            }
            endpoint.setConfigs(configsByEndpointId);
            if(ToolUtil.isNotEmpty(endpointProm)) {
                String agentId = ToolUtil.toStr(endpointProm.get("" + endpoint.getId()));
                if(StrUtil.isNotBlank(agentId))
                    endpoint.setAgent(agentMap.get(Integer.valueOf(agentId)));
            }
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
        List<MonitorEndpoint> endpointList = this.list(new LambdaQueryWrapper<MonitorEndpoint>().like(StringUtils.isNotEmpty(name), MonitorEndpoint::getName, Tool.StrUtil.likeEscape(name)));
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

    /**
     * 通过 module chart tepmlate 新增 endpoint charts
     * @param endpoint
     */
    public void saveEndpointChartsViaModuleChartTmpl(MonitorEndpoint endpoint) {
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

                saveChart.setVarType(2);
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

        //删除收藏
        starredService.remove(new LambdaQueryWrapper<SysUserStarredEntity>()
                .eq(SysUserStarredEntity::getType,"endpoint")
                .in(SysUserStarredEntity::getTid,ids)
        );

        // 删除 endpoint state
        endpointStateService.removeByIds(ids);
        List<String> deleteList = ids.stream().map(id-> id.toString()).collect(Collectors.toList());
        redisTemplate.opsForHash().delete(Constant.ENDPOINT_METRICS_STATE_CACHE_KEY, deleteList.toArray());
        redisTemplate.opsForHash().delete(Constant.ENDPOINT_LOGS_STATE_CACHE_KEY, deleteList.toArray());

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
    public void getTemplateByLanguage(HttpServletResponse response, String format) throws IOException {
        String sysHeaderJsonStr = sysConfService.getValue(Constant.SYSCONFIG_KEY_MONITORENDPOINT_EXPORT_HEADER);
        String fileName = "EndpointTemplate";
        basicImportAndExportServices.getTemplate(sysHeaderJsonStr, format, fileName, response);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
    public Map importEndpointData(MultipartFile multipartFile, Map<String, Object> params) throws IOException {
        String sysHeaderJsonStr = sysConfService.getValue(Constant.SYSCONFIG_KEY_MONITORENDPOINT_EXPORT_HEADER);
        Map<String, String> exportHeaderMap = basicImportAndExportServices.getI18nHeaderMap(sysHeaderJsonStr);
        List<Map<Integer, String>> importDataList = basicImportAndExportServices.getImportDataFromMultipartFile(multipartFile, sysHeaderJsonStr);

        String seq = UUID.randomUUID().toString();
        params.put("seq", seq);

        // 表头信息 国际化之后的
        List<String> headerInfos = new ArrayList<>(exportHeaderMap.keySet());

        int totalNum = importDataList.size();
        List<Map<String, String>> failDetailList = this.saveEndpointsFromImportDataList(importDataList, headerInfos, params);

        // 从错误信息中获取失败导入行数量
        int failNum = (int) failDetailList.stream().map(map -> Integer.valueOf(map.get("lineNo"))).distinct().count();

        Map resultMap = new HashMap(8);
        resultMap.put("seq", seq);
        resultMap.put("totalNum", totalNum);
        resultMap.put("successNum", totalNum - failNum);
        resultMap.put("failNum", failNum);
        resultMap.put("failDetail", failDetailList);
        return resultMap;
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

        PageUtils page = (PageUtils) this.queryPage(params).get("page");
        List<MonitorEndpoint> list = (List<MonitorEndpoint>) page.getList();

        List<List<String>> exportDataList = this.handleExportDataList(list);

        String sysHeaderJsonStr = sysConfService.getValue(Constant.SYSCONFIG_KEY_MONITORENDPOINT_EXPORT_HEADER);
        String fileName = "Endpoints";
        String formatStr = params.get("format").toString();
        basicImportAndExportServices.exportDataByFormat(response, exportDataList, sysHeaderJsonStr, formatStr, fileName);
    }

    private List<List<String>> handleExportDataList(List<MonitorEndpoint> list) {
        List<List<String>> exportDataList = new ArrayList<>();
        for (MonitorEndpoint endpoint : list) {
            List<String> tempList = new ArrayList<>();
            MonitorProject project = endpoint.getProject();
            MonitorModule module = endpoint.getModule();
            AssetAsset asset = endpoint.getAsset();

            // project name
            tempList.add(project.getName());
            // module name
            tempList.add(module.getName());
            // name
            tempList.add(endpoint.getName());
            // asset name
            tempList.add(asset.getName());
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

                tempList.add(JSONObject.toJSONString(tempConfigList));
            }
            exportDataList.add(tempList);
        }
        return exportDataList;
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
    public List<MonitorEndpoint> getEnableLogsEndpointInfoListForAgent(Integer version) {
        List<MonitorEndpoint> enableEndpointInfoList = monitorEndpointDao.getEnableLogsEndpointInfoListForAgent(version);
        // 如 asset 存在 parent , 则 set parent
        List<AssetAsset> assetList = assetService.list();
        Map<Integer, AssetAsset> assetIdAndEntityMap = assetList.stream().collect(Collectors.toMap(AssetAsset::getId, Function.identity()));
        for (MonitorEndpoint endpoint : enableEndpointInfoList) {
            AssetAsset relAsset = endpoint.getAsset();
            relAsset.setParent(assetIdAndEntityMap.get(relAsset.getPid()));
        }
        return enableEndpointInfoList;
    }

    private List<Map<String, String>> saveEndpointsFromImportDataList(List<Map<Integer, String>> importDataList, List<String> headerInfos, Map<String, Object> params) {
        // 已存在的内容处理方式，可选值：0：错误 error  1：覆盖 override   2：忽略 ignore
        String existed = Tool.ObjectUtil.defaultIfEmpty((String) params.get("existed"), "1");

        // 创建导入校验对象
        ImportValidateUtil validateUtil = ImportValidateUtil.builder()
                .existed(existed).headerInfos(headerInfos)
                .build();

        List<AssetAsset> assetList = assetService.list();
        List<MonitorProject> projectList = monitorProjectService.list();
        List<MonitorModule> moduleList = monitorModuleService.list();
        for (int i = 0; i < importDataList.size(); i++) {
            Integer lineNum = i + 1;
            Map<Integer, String> dataMap = importDataList.get(i);

            String projectName = dataMap.get(MonitorEndpointImportEnum.PROJECT.getIndex());
            validateUtil.validateRequired(projectName, lineNum, MonitorEndpointImportEnum.PROJECT.getIndex());
            Boolean projectExist = validateUtil.validateRelObjExits("monitor_project", "name", projectName, false, lineNum, MonitorEndpointImportEnum.PROJECT.getIndex());

            String moduleName = dataMap.get(MonitorEndpointImportEnum.MODULE.getIndex());
            validateUtil.validateRequired(moduleName, lineNum, MonitorEndpointImportEnum.MODULE.getIndex());
            Boolean moduleExist = validateUtil.validateRelObjExits("monitor_module", "name", moduleName, false, lineNum, MonitorEndpointImportEnum.MODULE.getIndex());

            // project & module 是否呼应
            if (BooleanUtil.and(projectExist, moduleExist)) {
                MonitorProject project = projectList.stream().filter(pojo -> StrUtil.equals(pojo.getName(), projectName)).findFirst().orElse(null);
                MonitorModule module = moduleList.stream().filter(pojo -> StrUtil.equals(pojo.getName(), moduleName)).findFirst().orElse(null);
                if (ObjectUtil.isAllNotEmpty(project, module) && ObjectUtil.notEqual(project.getId(), module.getProjectId())) {
                    validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_NOT_MATCH.setParam(MonitorEndpointImportEnum.PROJECT.getIndex(), MonitorEndpointImportEnum.MODULE.getIndex()));
                }
            }

            String assetName = dataMap.get(MonitorEndpointImportEnum.ASSET.getIndex());
            validateUtil.validateRequired(assetName, lineNum, MonitorEndpointImportEnum.ASSET.getIndex());
            Boolean assetExist = validateUtil.validateRelObjExits("asset_asset", "name", assetName, false, lineNum, MonitorEndpointImportEnum.ASSET.getIndex());

            // endpoint config jsonarray str
            String configStr = dataMap.get(MonitorEndpointImportEnum.CONFIGURATIONS.getIndex());
            boolean isJsonArray = validateUtil.validateJsonArray(configStr, lineNum, MonitorEndpointImportEnum.CONFIGURATIONS.getIndex());
            if (isJsonArray) {
                List<Map> configMapList = JSONObject.parseArray(configStr, Map.class);
                configMapList = Tool.CollUtil.defaultIfEmpty(configMapList, new ArrayList<>());

                int countByType = (int) configMapList.stream().map(map -> map.get("type")).distinct().count();
                boolean configDuplicateByType = (countByType == configMapList.size());
                if (!configDuplicateByType) // 每个 type 类型的配置有且只有一个, 当 type 符合要求才会校验 metrics & logs 详细信息
                    validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_DUPLICATE.setParam(MonitorEndpointImportEnum.CONFIGURATIONS.getIndex()));

                List<String> configTypeList = Arrays.asList(Constant.MonitorConfType.values()).stream().map(Constant.MonitorConfType::getValue).collect(Collectors.toList());
                for (Map map : configMapList) {
                    validateUtil.validateRequired(map.get("type"), lineNum, MonitorEndpointImportEnum.CONFIGURATIONS.getIndex());
                    validateUtil.validateEnumValArr(map.get("type").toString(), configTypeList, lineNum, MonitorEndpointImportEnum.CONFIGURATIONS.getIndex());
                }

                Map metricsConfigMap = configMapList.stream().filter(map -> {
                    String configType = (String) map.get("type");
                    Integer enable = (Integer) ObjectUtil.defaultIfNull(map.get("enable"), 1);
                    return BooleanUtil.and(StrUtil.equals(Constant.MonitorConfType.METRICS.getValue(), configType), enable == 1, configDuplicateByType);
                }).findFirst().orElse(null);
                if (Tool.CollUtil.isNotEmpty(metricsConfigMap)) {
                    List<String> protocolEnumValueList = Tool.ListUtil.of(Constant.MODULE_TYPE_HTTP, Constant.MODULE_TYPE_SNMP, Constant.MODULE_TYPE_HTTPS);
                    Map configMap = (Map) metricsConfigMap.get("config");
                    String protocol = (String) configMap.get("protocol");
                    validateUtil.validateEnumValArr(protocol, protocolEnumValueList, lineNum, MonitorEndpointImportEnum.CONFIGURATIONS.getIndex());

                    Object host = configMap.get("host");
                    if (ToolUtil.isNotEmpty(host))
                        validateUtil.validateIp(host.toString(), lineNum, MonitorEndpointImportEnum.CONFIGURATIONS.getIndex());

                    Object port = configMap.get("port");
                    if (ToolUtil.isNotEmpty(port))
                        validateUtil.validatePort(port.toString(), lineNum, MonitorEndpointImportEnum.CONFIGURATIONS.getIndex());

                    // snmp 类型校验 config
                    if (StrUtil.equals(Constant.MODULE_TYPE_SNMP, protocol)) {
                        Object snmpCredentialsId = configMap.get("snmpCredentialsId");
                        validateUtil.validateRequired(snmpCredentialsId, lineNum, MonitorEndpointImportEnum.CONFIGURATIONS.getIndex());
                        String snmpCredentialsIdStr = ObjectUtil.isNotEmpty(snmpCredentialsId) ? snmpCredentialsId.toString() : StrUtil.EMPTY;
                        validateUtil.validateRelObjExits("snmp_credential", "id", snmpCredentialsIdStr, false, lineNum, MonitorEndpointImportEnum.CONFIGURATIONS.getIndex());

                        Object walk = configMap.get("walk");
                        validateUtil.validateRequired(walk, lineNum, MonitorEndpointImportEnum.CONFIGURATIONS.getIndex());
                        validateUtil.validateJsonArray(walk != null ? JSONObject.toJSONString(walk) : "", lineNum, MonitorEndpointImportEnum.CONFIGURATIONS.getIndex());
                    }

                    Map<String, Object> labels = MapUtil.defaultIfEmpty((Map) configMap.get("labels"), Collections.emptyMap());
                    labels.keySet().stream().forEach(labelKey -> {
                        validateUtil.validateRegex(labelKey, Constant.PROMETHEUS_LABELNAME_EXPRESSION, lineNum, MonitorEndpointImportEnum.CONFIGURATIONS.getIndex());
                    });

                    List<Map> relabelConfigList = ObjectUtil.defaultIfNull((List) configMap.get("relabel_config"), Collections.emptyList());
                    for (Map map : relabelConfigList) {
                        List<String> sourceLabels = ObjectUtil.defaultIfNull((List) map.get("source_labels"), Collections.emptyList());
                        for (String sourceLabel : sourceLabels) {
                            validateUtil.validateRegex(sourceLabel, Constant.PROMETHEUS_LABELNAME_EXPRESSION, lineNum, MonitorEndpointImportEnum.CONFIGURATIONS.getIndex());
                        }
                        String targetLabel = (String) map.get("target_label");
                        if (StrUtil.isNotEmpty(targetLabel)) {
                            validateUtil.validateRegex(targetLabel, Constant.PROMETHEUS_LABELNAME_EXPRESSION, lineNum, MonitorEndpointImportEnum.CONFIGURATIONS.getIndex());
                        }
                    }
                }

                Map logsConfigMap = configMapList.stream().filter(map -> {
                    String configType = (String) map.get("type");
                    Integer enable = (Integer) ObjectUtil.defaultIfNull(map.get("enable"), 1);
                    return BooleanUtil.and(StrUtil.equals(Constant.MonitorConfType.LOGS.getValue(), configType), enable == 1, configDuplicateByType);
                }).findFirst().orElse(null);
                if (Tool.CollUtil.isNotEmpty(logsConfigMap) && ToolUtil.isNotEmpty(logsConfigMap.get("config"))) {
                    List<Map> configMaps = (List<Map>) logsConfigMap.get("config");
                    for (Map configMap : configMaps) {
                        if ("file".equals(configMap.get("type").toString())) {
                            validateUtil.validateRequired(configMap.get("fileName"), lineNum, MonitorEndpointImportEnum.CONFIGURATIONS.getIndex());
                        } else if ("journal".equals(configMap.get("type").toString())) {
                            validateUtil.validateRequired(configMap.get("unit"), lineNum, MonitorEndpointImportEnum.CONFIGURATIONS.getIndex());
                        } else if ("syslog".equals(configMap.get("type").toString())) {
                            validateUtil.validateRequired(configMap.get("appName"), lineNum, MonitorEndpointImportEnum.CONFIGURATIONS.getIndex());
                            validateUtil.validateRequired(configMap.get("listenAddress"), lineNum, MonitorEndpointImportEnum.CONFIGURATIONS.getIndex());
                        } else {
                            validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_ENUM_VALUE_ERROR.setParam(MonitorEndpointImportEnum.CONFIGURATIONS.getIndex()));
                        }
                    }
                }
            }

            String name = null;
            if (BooleanUtil.and(moduleExist, assetExist)) {
                AssetAsset asset = assetList.stream().filter(pojo -> StrUtil.equals(pojo.getName(), assetName)).findFirst().orElse(null);
                MonitorModule module = moduleList.stream().filter(pojo -> StrUtil.equals(pojo.getName(), moduleName)).findFirst().orElse(null);

                name = dataMap.get(MonitorEndpointImportEnum.NAME.getIndex());
                if (StrUtil.isEmpty(name)) {
                    AssetAsset assetInfo = assetService.queryAssetInfo(asset.getId());
                    MonitorModule moduleInfo = monitorModuleService.queryModuleInfo(module.getId());
                    name = Tool.NzExpressionUtil.renderExpression(module.getEndpointNameTmpl(), assetInfo, moduleInfo);
                }

                boolean indexDuplicate = validateUtil.validateIndexDuplicate("monitor_endpoint", "name", name, lineNum, MonitorEndpointImportEnum.NAME.getIndex());
                // 索引列重复 & 忽略标识，提示忽略本行
                if (BooleanUtil.and(indexDuplicate, StrUtil.equals("2", existed))) {
                    validateUtil.ignoreThisRow(lineNum, MonitorEndpointImportEnum.NAME.getIndex());
                    continue;
                }
            }

            List<Map<String, String>> errMsgForThisLine = validateUtil.getImportErrMsgForThisLine(lineNum);
            // 遇到错误是否继续导入，可选值： 0：不继续    1：继续    默认值：1
            String ignoreError = Tool.ObjectUtil.defaultIfEmpty((String) params.get("ignoreError"), "1");
            if (BooleanUtil.and(Tool.StrUtil.equals("0", ignoreError), Tool.CollUtil.isNotEmpty(errMsgForThisLine))) {
                break;
            }
            if (Tool.CollUtil.isEmpty(errMsgForThisLine)) {
                AssetAsset asset = assetList.stream().filter(pojo -> StrUtil.equals(pojo.getName(), assetName)).findFirst().orElse(null);
                MonitorModule module = moduleList.stream().filter(pojo -> StrUtil.equals(pojo.getName(), moduleName)).findFirst().orElse(null);
                MonitorEndpoint monitorEndpoint = new MonitorEndpoint();
                monitorEndpoint.setName(name);
                monitorEndpoint.setAssetId(asset.getId());
                monitorEndpoint.setModuleId(module.getId());
                monitorEndpoint.setConfigs(configStr);
                // config 如果为空 则设置 module configs
                if (StrUtil.isEmpty(configStr)) {
                    AssetAsset assetInfo = assetService.queryAssetInfo(asset.getId());
                    MonitorModule moduleInfo = monitorModuleService.queryModuleInfo(module.getId());
                    String configStrByTemplate = Tool.NzExpressionUtil.renderExpression(module.getConfigs(), assetInfo, moduleInfo);
                    monitorEndpoint.setConfigs(configStrByTemplate);
                }
                DefaultTransactionDefinition def = new DefaultTransactionDefinition();
                TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(def);
                try {
                    this.saveOrUpdateRowData(monitorEndpoint, params);
                    dataSourceTransactionManager.commit(transactionStatus);
                } catch (Exception e) {
                    dataSourceTransactionManager.rollback(transactionStatus);
                    validateUtil.recordImportAttrErr(lineNum, RCode.ERROR);
                    log.error("Endpoint import roll back the transaction failure", e);
                }
            }
        }
        // 返回错误信息
        return validateUtil.getImportErrMsg();
    }

    private void saveOrUpdateRowData(MonitorEndpoint endpoint, Map<String, Object> params) {
        String seq = params.get("seq").toString();
        endpoint.setSeq(seq);

        // 已存在的内容处理方式，可选值：0：错误 error  1：覆盖 override   2：忽略 ignore   在此只需要处理更新即可
        String existed = Tool.ObjectUtil.defaultIfEmpty((String) params.get("existed"), "1");
        MonitorEndpoint oldEndpoint = this.getOne(new LambdaQueryWrapper<MonitorEndpoint>().eq(MonitorEndpoint::getName, endpoint.getName()));

        if (StrUtil.equals("1", existed) && ObjectUtil.isNotEmpty(oldEndpoint)) {
            // update & sync
            endpoint.setId(oldEndpoint.getId());

            // endpoint
            this.updateById(endpoint);
            // 删除 state 数据
            this.removeEndpointStateById(endpoint.getId());

            // 是否同步更新关联的dashboard，可选值： 0：不同步  1：同步  默认值：1
            String syncDashboard = Tool.ObjectUtil.defaultIfEmpty((String) params.get("syncDashboard"), "1");
            if (StrUtil.equals("1", syncDashboard)) {
                // endpoint info charts && dashboard charts
                List<VisualChart> endpointChartList = visualChartService.list(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getVarType, 2).eq(VisualChart::getVarId, endpoint.getId()));
                if (Tool.CollUtil.isNotEmpty(endpointChartList)) {
                    List<Integer> endpointChartIdList = endpointChartList.stream().map(VisualChart::getId).collect(Collectors.toList());
                    Map<String, Object> map = Tool.MapUtil.newHashMap(2);
                    map.put("ids", endpointChartIdList);
                    visualChartService.syncTmpl(map);
                }
            }
        } else {
            // endpoint
            this.save(endpoint);
            // endpoint panel & info chart
            this.saveEndpointInfoPanel(endpoint);
            // 通过 module chart tepmlate 新增 endpoint charts
            this.saveEndpointChartsViaModuleChartTmpl(endpoint);
        }

        // endpoint config
        MonitorModule module = monitorModuleService.getById(endpoint.getModuleId());
        this.saveOrUpdateEndpointConfigs(endpoint, module);
    }

    /**
     * 删除 metrics & logs state 数据
     *
     * @param endpointId
     */
    private void removeEndpointStateById(Integer endpointId) {
        if (endpointId == null) return;

        endpointStateService.removeById(endpointId);

        redisTemplate.opsForHash().delete(Constant.ENDPOINT_METRICS_STATE_CACHE_KEY, endpointId.toString());
        redisTemplate.opsForHash().delete(Constant.ENDPOINT_LOGS_STATE_CACHE_KEY, endpointId.toString());
    }

    /**
     * 新增 endpoint penel & info chart
     *
     * @param endpoint
     */
    private void saveEndpointInfoPanel(MonitorEndpoint endpoint) {
        // endpoint config
        String sysEndpointChartTplStr = sysConfService.getValue(Constant.SYSCONFIG_KEY_ENDPOINT_CHART_TPL);
        VisualChart chartTemplate = JSONObject.parseObject(sysEndpointChartTplStr, VisualChart.class);
        // insert panel chart
        visualChartService.saveRelPanelAndChart(Constant.PanelType.ENDPOINT.getValue(), endpoint.getId(), endpoint.getSeq(), chartTemplate);
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
            validateEndpointMetrics(metricsConfigMap);

            Map logsConfigMap = configMapList.stream().filter(map -> {
                String configType = (String) map.get("type");
                return StrUtil.equals(Constant.MonitorConfType.LOGS.getValue(), configType);
            }).findFirst().orElse(null);
            validateEndpointLogs(logsConfigMap);
        }
    }

    private void validateEndpointLogs(Map logsConfigMap){
        if (logsConfigMap != null && 1 == (Integer) ObjectUtil.defaultIfNull(logsConfigMap.get("enable"), 1)) {
            if (ToolUtil.isNotEmpty(logsConfigMap.get("config"))) {
                List<Map> configMaps = JSONArray.parseArray(String.valueOf(logsConfigMap.get("config")), Map.class);
                for (Map configMap : configMaps) {
                    // Basic
                    if ("file".equals(configMap.get("type").toString()) && ToolUtil.isEmpty(configMap.get("fileName"))) {
                        throw new NZException(RCode.MODULE_CONFIG_FILENAME_ISNULL);
                    }else if ("journal".equals(configMap.get("type").toString()) && ToolUtil.isEmpty(configMap.get("unit"))) {
                        throw new NZException(RCode.MODULE_CONFIG_JOURNALUNIT_ISNULL);
                    }else if ("syslog".equals(configMap.get("type").toString())) {
                        if(ToolUtil.isEmpty(configMap.get("appName"))){
                            throw new NZException(RCode.MODULE_CONFIG_SYSLOG_APPNAME_ISNULL);
                        }else if(ToolUtil.isEmpty(configMap.get("listenAddress"))){
                            throw new NZException(RCode.MODULE_CONFIG_SYSLOG_ADDRESS_ISNULL);
                        }
                    }
                    // Pipeline
                    if(ToolUtil.isNotEmpty(configMap.get("pipeline"))){
                        List<Map> pipelineList = JSONObject.parseArray(String.valueOf(configMap.get("pipeline")), Map.class);
                        for (Map pipeline: pipelineList) {
                            String pipelType = String.valueOf(pipeline.get("type"));
                            if(StrUtil.equals("regex", pipelType) && ToolUtil.isEmpty(pipeline.get("expression"))){
                                throw new NZException(RCode.MODULE_CONFIG_PIPELINE_EXPRESSION_ISNULL);
                            }else if(StrUtil.equals("template", pipelType) && ToolUtil.isEmpty(pipeline.get("template"))){
                                throw new NZException(RCode.MODULE_CONFIG_PIPELINE_TEMPLATE_ISNULL);
                            }else if(StrUtil.equals("timestamp", pipelType) && ToolUtil.isEmpty(pipeline.get("format"))){
                                throw new NZException(RCode.MODULE_CONFIG_PIPELINE_FORMAT_ISNULL);
                            }else if(StrUtil.equals("json", pipelType)){
                                if(correctVal(pipeline.get("expressions"))){
                                    throw new NZException(RCode.MODULE_CONFIG_PIPELINE_EXPRESSIONS_ISNULL);
                                }else{
                                    Map<String, Object> expressions = ObjectUtil.defaultIfNull(JSONObject.parseObject(String.valueOf(pipeline.get("expressions")), Map.class), Collections.emptyMap());
                                    for (String expression : expressions.keySet()) {
                                        ValidateUtils.is(expression).regex(Constant.PROMETHEUS_LABELNAME_EXPRESSION, RCode.ENDPOINT_LABELNAME_FORMAT_ERROR);
                                    }
                                }
                            }
                            if(!StrUtil.equals("labels", pipelType) && ToolUtil.isEmpty(pipeline.get("source"))){
                                throw new NZException(RCode.MODULE_CONFIG_PIPELINE_SOURCE_ISNULL);
                            }
                        }
                    }
                }
            }
        }
    }

    private void validateEndpointMetrics(Map metricsConfigMap){
        if (metricsConfigMap != null && (1 == (Integer) ObjectUtil.defaultIfNull(metricsConfigMap.get("enable"), 1))) {
            Map configMap = JSONObject.parseObject(String.valueOf(metricsConfigMap.get("config")), Map.class);
            String protocol = (String) configMap.get("protocol");
            // 校验 type 类型是否在枚举范围内
            if (!StrUtil.equals(Constant.MODULE_TYPE_HTTP, protocol) && !StrUtil.equals(Constant.MODULE_TYPE_SNMP, protocol)
                    && !StrUtil.equals(Constant.MODULE_TYPE_HTTPS, protocol)) {
                throw new NZException(RCode.ENDPOINT_PROTOCOL_INVALID);
            }
            ValidateUtils.is(configMap.get("port")).notNull(RCode.ENDPOINT_PORT_ISNULL);
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
            // Metric relabel
            List<Map> relabelConfigList = ObjectUtil.defaultIfNull((List) configMap.get("relabel_config"), Collections.emptyList());
            for (Map map : relabelConfigList) {
                String action = String.valueOf(map.get("action"));
                if((StrUtil.equals("replace",action) || StrUtil.equals("keep",action) || StrUtil.equals("drop",action)) && correctVal(map.get("source_labels"))){
                    throw new NZException(RCode.MODULE_CONFIG_RELABEL_SOURCELABELS_ISNULL);
                }
                if(StrUtil.equals("replace",action) && ToolUtil.isEmpty(map.get("target_label"))){
                    throw new NZException(RCode.MODULE_CONFIG_RELABEL_TARGETLABEL_ISNULL);
                }
                ValidateUtils.is(map.get("regex")).notNull(RCode.MODULE_CONFIG_RELABEL_REGEX_ISNULL);
                if((StrUtil.equals("replace",action) || StrUtil.equals("labelmap",action)) && ToolUtil.isEmpty(map.get("replacement"))){
                    throw new NZException(RCode.MODULE_CONFIG_RELABEL_REPLACEMENT_ISNULL);
                }

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

        // endpoint panel & info chart
        this.saveEndpointInfoPanel(endpoint);

        // 通过 module chart tepmlate 新增 endpoint charts
        this.saveEndpointChartsViaModuleChartTmpl(endpoint);
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
                endpointConfig.setSeq(endpoint.getSeq());
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
        
        // 删除 state 数据
        this.removeEndpointStateById(endpoint.getId());
        return endpoint.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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

    @Override
    public Map<String, Object> diagnose(Integer id, HttpServletRequest request) throws IOException {
        MonitorEndpoint endpoint = this.getById(id);
        if(ToolUtil.isEmpty(endpoint)){
            throw new NZException(RCode.ENDPOINT_NOTFOUND);
        }
        Map<String, Object> resultMap = MapUtil.newHashMap();
        List<Map> resultList = Tool.ListUtil.list(false);
        // 获取 metrics 和 logs 状态
        Integer metricsState = null,logsState = null;
        MonitorEndpointConfig metricsConfigs = null;
        List<MonitorEndpointConfig> endpointConfigs = this.getEndpointConfigs(endpoint);
        for (MonitorEndpointConfig endpointConfig : endpointConfigs) {
            if (endpointConfig.getType().equals(Constant.MonitorConfType.METRICS.getValue())) {
                metricsConfigs = endpointConfig;
                metricsState = endpointConfig.getState();
                if(metricsState == null && endpointConfig.getEnable() == 0)
                    metricsState = 12;
            } else {
                logsState = endpointConfig.getState();
                if(logsState == null && endpointConfig.getEnable() == 0)
                    logsState = 12;
            }
        }
        // 获取 endpoint 对应的 agent
        Agent agent = null;
        HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
        if(ToolUtil.isNotEmpty(opsForHash)){
            agent = agentService.getById(opsForHash.get(Constant.REDIS_KEY_ENDPOINT_PROM, endpoint.getId().toString()));
        }

        resultList.add(this.metricDiagnose(metricsConfigs, metricsState, agent, request));
        resultList.add(this.logsDiagnose(endpoint, logsState));
        resultMap.put("list", resultList);
        return resultMap;
    }

    /**
     * endpoint metric 异常诊断
     */
    private Map<String, String> metricDiagnose(MonitorEndpointConfig metricsConfigs, Integer metricsState, Agent agent, HttpServletRequest request) throws IOException {
        Map<String, String> ambientMap = this.metricAmbientDiagnose(metricsState, metricsConfigs, agent);
        if(ambientMap != null)
            return ambientMap;
        // 构建请求参数,查询 agent targets 信息
        String dcId = agent.getDcId().toString();
        String agentId =agent.getId().toString();
        String endpointId = metricsConfigs.getEndpointId().toString();
        String type =agent.getType().toString();

        JSONObject resultData = promApiService.targets(dcId, agentId, endpointId, type, Tool.ListUtil.empty(), null);

        String activeTargetStr = JSONPath.read(resultData.toJSONString(), "data.activeTargets").toString();
        List<Map> activeTargets = JSONArray.parseArray(activeTargetStr, Map.class);
        // endpoint target 信息
        Map endpointTarget = activeTargets.stream().filter(
                activeTarget -> ToolUtil.equals(JSONObject.parseObject(ToolUtil.toStr(activeTarget.get("labels"))).getString("endpoint_id"), ""+metricsConfigs.getEndpointId())
        ).findFirst().orElse(null);

        if(endpointTarget == null){
            // 尚未配置下发
            return this.setI18nToMap("agent_wait_pull");
        }else{
            if(ToolUtil.equals(endpointTarget.get("health"), "up")){
                return this.setI18nToMap("mertic_up");
            }else{
                String lastError = endpointTarget.get("lastError").toString();
                if(lastError.contains("connect: no route to host")){
                    // 请求被防火墙阻止
                    return this.setI18nToMap("mertic_firewall");
                }else if(lastError.contains("context deadline exceeded") || lastError.contains("i/o timeout")){
                    // ping metric 网络
                    Integer ping = this.pingSingleIp(agent, JSONPath.read(metricsConfigs.getConfig(), "host"));
                    if(ping == 0){
                        return this.setI18nToMap("mertic_ping_down");
                    }
                    // 连接超时
                    return this.setI18nToMap("mertic_timeout");
                }else if(lastError.contains("server returned HTTP status 401 Unauthorized")){
                    // 身份验证失败
                    return this.setI18nToMap("mertic_unauthorized");
                }else if(lastError.contains("connect: connection refused")){
                    // 端口无服务监听
                    return this.setI18nToMap("mertic_nolistening");
                }else if(lastError.contains("server returned HTTP status 500 Internal Server Error")){
                    // SNMP获取失败
                    return this.setI18nToMap("mertic_snmp_error");
                }
            }
        }
        return null;
    }

    /**
     * agent、DataCenter等组件诊断
     */
    private Map<String, String> metricAmbientDiagnose(Integer metricsState, MonitorEndpointConfig metricsConfigs, Agent agent){
        // 从 EndpointStateJob 更新的 metricsState 判断
        if(Arrays.asList(new Integer[]{2, 8, 10}).contains(metricsState)){
            // asset 已停用
            return this.setI18nToMap("asset_stop_using");
        }else if(Arrays.asList(new Integer[]{4, 6}).contains(metricsState)){
            // DataCenter 已关闭
            return this.setI18nToMap("dc_disabled");
        }else if(ToolUtil.equals(12, metricsState)){
            // metrics 未启用
            return this.setI18nToMap("mertic_disable");
        }else if(ToolUtil.equals(1, metricsState)){
            // metrics 正常
            return this.setI18nToMap("mertic_up");
        }
        // 检查 agent
        if(ToolUtil.isNotEmpty(agent)){
            if(ToolUtil.equals(1, agent.getStatus())){
                String federation = sysConfService.getValueOrDefault(Constant.SYSCONFIG_KEY_PROM_FEDER_ENABLED, "1");
                if(StrUtil.equals("1", federation) && ToolUtil.equals(1, agent.getType())){
                    // 集群模式 global agent 不采集数据
                    return this.setI18nToMap("agent_federation_global");
                }else if(StrUtil.equals("2", federation) && ToolUtil.equals(2, agent.getType())){
                    // 非集群模式 PerDataCenter agent 不采集数据
                    return this.setI18nToMap("agent_unfederation_per");
                }
            }else{
                // agent DOWN
                return this.setI18nToMap("agent_down");
            }
        }else{
            // 无 agent 采集该数据中心
            return this.setI18nToMap("agent_undistributed");
        }
        if(ToolUtil.equals(null, metricsState)) {
            // endpoint 刚进行了修改，等待配置下发
            return this.setI18nToMap("agent_wait_pull");
        }
        return null;
    }

    /**
     * endpoint logs 异常诊断
     */
    private Map<String, String> logsDiagnose(MonitorEndpoint endpoint, Integer logsState){
        // 从 AssetTalonStatusJob 更新的 talonStatus 判断;状态246为数据中心关闭
        if(Arrays.asList(new Integer[]{8, 10}).contains(logsState)){
            // Asset Talon 未开启
            return this.setI18nToMap("logs_talon_disable");
        }else if(ToolUtil.equals(12, logsState)){
            // logs 未启用
            return this.setI18nToMap("logs_disable");
        }else if(ToolUtil.equals(1, logsState)){
            // logs 正常
            return this.setI18nToMap("logs_up");
        }
        // 判断 Asset Talon状态
        AssetTalonStatus assetTalon = assetTalonService.getById(endpoint.getAssetId());
        if (assetTalon.getStatus() == 4) {
            // 未知错误
            return this.setI18nToMap("logs_unknown_error");
        } else if (assetTalon.getStatus() == 3) {
            // 认证失败
            return this.setI18nToMap("logs_authentication_failed");
        } else if (assetTalon.getStatus() == 2) {
            // 连接超时
            return this.setI18nToMap("logs_timeout");
        } else if (assetTalon.getStatus() == 0) {
            // talon未开启
            return this.setI18nToMap("logs_talon_disable");
        } else {
            // 请检查配置
            return this.setI18nToMap("logs_config_error");
        }
    }

    private Map<String, String> setI18nToMap(String code){
        Map<String, String> map = MapUtil.newHashMap();
        map.put("reason", i18nService.queryValue(code + "_msg"));
        map.put("resolution", i18nService.queryValue(code + "_handle"));
        return map;
    }

    /**
     * 对单个 ip 进行 ping 查询
     * @return 0-失败，1-成功，2-超时
     */
    private Integer pingSingleIp(Agent agent, Object ip){
        if(ToolUtil.isNotEmpty(agent) && ToolUtil.isNotEmpty(ip)){
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
                Map<String, List<String>> body = Tool.MapUtil.of(Pair.of("ips", Tool.StrUtil.split(String.valueOf(ip), ',')));
                String resultJsonStr = AgentUtil.request(agent, HttpMethod.POST, "/cmd/ping", null, body, String.class);
                String pingMap = JSONPath.read(resultJsonStr, "data.list[0]").toString();
                return Integer.valueOf(JSONObject.parseObject(pingMap).get("state").toString());
            });
            try {
                // 超时时长
                Map<String, String> defaultScrapeTimeout = sysConfService.batchQuery("default_scrape_timeout");
                return future.get(Long.valueOf(defaultScrapeTimeout.get("default_scrape_timeout")), TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                return 0;
            } catch (TimeoutException e) {
                future.cancel(true);
                return 2;
            }
        }
        return 0;
    }

}
