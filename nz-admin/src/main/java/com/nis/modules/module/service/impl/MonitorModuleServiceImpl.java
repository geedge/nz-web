package com.nis.modules.module.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetModelImportEnum;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.dashboard.entity.VisualDashboard;
import com.nis.modules.dashboard.service.VisualDashboardService;
import com.nis.modules.dashboard.service.VisualService;
import com.nis.modules.dashboard.utils.DashboardConstant;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.module.dao.MonitorModuleDao;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.module.entity.MonitorModuleImportEnum;
import com.nis.modules.module.service.MonitorModuleService;
import com.nis.modules.project.entity.MonitorProject;
import com.nis.modules.project.service.MonitorProjectService;
import com.nis.modules.project.service.TopoService;
import com.nis.modules.stat.service.StatService;
import com.nis.modules.sys.entity.SnmpCredential;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysUserStarredEntity;
import com.nis.modules.sys.service.BasicImportAndExportServices;
import com.nis.modules.sys.service.SnmpCredentialService;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserStarredService;
import com.nis.modules.sys.shiro.ShiroUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MonitorModuleServiceImpl extends ServiceImpl<MonitorModuleDao, MonitorModule> implements MonitorModuleService {

    protected Log log = Log.get();

    @Autowired
    private YamlUtil yamlUtil;

    @Autowired
    private TopoService topoService;

    @Autowired
    private MonitorModuleDao monitorModuleDao;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private MonitorProjectService monitorProjectService;

    @Autowired
    private MonitorEndpointService monitorEndpointService;

    @Autowired
    private AlertMessageService alertMessageService;

    @Autowired
    private SnmpCredentialService snmpCredentialService;

    @Autowired
    private VisualDashboardService dashboardService;

    @Autowired
    private VisualService visualService;

    @Autowired
    private StatService statService;

    @Autowired
    private BasicImportAndExportServices basicImportAndExportServices;

    @Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    private SysUserStarredService starredService;
    
    @Override
    public MonitorModule queryModuleInfo(Integer id) {
        MonitorModule module = this.getById(id);
        if (ToolUtil.isEmpty(module)) {
            throw new NZException(RCode.MODULE_NOTFOUND);
        }
        MonitorProject project = monitorProjectService.getById(module.getProjectId());
        module.setProject(project);

        List<MonitorEndpoint> endpointList = monitorEndpointService.list();
        endpointList = endpointList.stream().filter(endpoint -> module.getId().equals(endpoint.getModuleId())).collect(Collectors.toList());
        module.setEndpointNum(endpointList.size());

        long assetNum = endpointList.stream().map(MonitorEndpoint::getAssetId).distinct().count();
        module.setAssetNum(Integer.valueOf(String.valueOf(assetNum)));

        List<AlertMessageEntity> alertMessageList = alertMessageService.list(new LambdaQueryWrapper<AlertMessageEntity>().eq(AlertMessageEntity::getModuleId, module.getId()));
        module.setAlertNum(alertMessageList.size());
        List<Map<String, Object>> alert = alertMessageService.getAlertsStatBySeverity("module",id);
        module.setAlert(alert);

        Integer dashboardId = module.getDashboardId();
        if(ToolUtil.isNotEmpty(dashboardId)) {
            VisualDashboard dashboard = dashboardService.getById(dashboardId);
            module.setDashboard(dashboard);
        }

        // 收藏标识 0：未收藏 1：已收藏
        List<SysUserStarredEntity> tUserStatList = starredService.list(new LambdaQueryWrapper<SysUserStarredEntity>()
                .select(SysUserStarredEntity::getId)
                .eq(SysUserStarredEntity::getTid, module.getId())
                .eq(SysUserStarredEntity::getType, "module")
                .eq(SysUserStarredEntity::getUid, ShiroUtils.getUserId())
                .last("limit 1")
        );
        module.setStarred(CollUtil.isEmpty(tUserStatList) ? 0 : 1);
        return module;
    }

    /**
     * 处理请求参数
     *
     * @param params
     */
    private void handleQueryParam(Map<String, Object> params) {
        // ids
        if (ToolUtil.isNotEmpty(params.get("ids"))) {
            params.put("ids", String.valueOf(params.get("ids")).split(StringPool.COMMA));
        }
        // projectIds
        if (ToolUtil.isNotEmpty(params.get("projectIds"))) {
            params.put("projectIds", String.valueOf(params.get("projectIds")).split(StringPool.COMMA));
        }
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        // 处理请求参数
        this.handleQueryParam(params);

        //当前用id
        params.put("uid",ShiroUtils.getUserId().intValue());

        IPage page = new Query(MonitorModule.class).getPage(params);
        List<MonitorModule> datas = monitorModuleDao.queryPage(page, params);

        page.setRecords(datas);
        return new PageUtils(page);
    }

    @Override
    public List<MonitorProject> queryModuleTree(String name) {
        List<MonitorModule> moduleList = this.list(new LambdaQueryWrapper<MonitorModule>().like(StringUtils.isNotEmpty(name), MonitorModule::getName, Tool.StrUtil.likeEscape(name)));
        if (CollectionUtils.isEmpty(moduleList)) {
            return Collections.emptyList();
        }

        List<Integer> projectIdList = moduleList.stream().map(MonitorModule::getProjectId).distinct().collect(Collectors.toList());
        List<MonitorProject> projectList = monitorProjectService.list(new LambdaQueryWrapper<MonitorProject>().in(MonitorProject::getId, projectIdList));

        List<MonitorModule> projectModules;
        for (MonitorProject project : projectList) {
            projectModules = new ArrayList<>();
            for (MonitorModule module : moduleList) {
                if (project.getId().equals(module.getProjectId())) {
                    projectModules.add(module);
                }
            }
            project.setChildren(projectModules);
        }
        return projectList;
    }

    private void validateModuleInfo(MonitorModule module) {
        // 1. 校验 project 是否存在
        MonitorProject project = monitorProjectService.getById(module.getProjectId());
        if (ToolUtil.isEmpty(project)) {
            throw new NZException(RCode.PROJECT_NOTFOUND);
        }

        // 2. 名称不能重复
        List<MonitorModule> list = this.list(new QueryWrapper<MonitorModule>().lambda().eq(MonitorModule::getName, module.getName()));
        boolean flag = false;
        // 当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) ||
                (list != null && list.size() == 1 && list.get(0).getId().equals(module.getId()))) {
            flag = true;
        }

        if (!flag) {
            throw new NZException(RCode.MODULE_NAME_DUPLICATE);
        }

        // module dashboard template, type = template & varType = endpoint
        Integer dashboardId = module.getDashboardId();
        if (ToolUtil.isNotEmpty(dashboardId)) {
            VisualDashboard dashboard = dashboardService.getOne(new LambdaQueryWrapper<VisualDashboard>()
                    .eq(VisualDashboard::getId, dashboardId)
                    .eq(VisualDashboard::getType, DashboardConstant.Type.TEMPLATE.getValue())
                    .eq(VisualDashboard::getVarType, DashboardConstant.VariableType.ENDPOINT.getValue()));
            ValidateUtils.is(dashboard).notNull(RCode.DASHBOARD_NOT_EXIST);
        }

        // config 格式校验
        List<Map> configMapList;
        try {
            configMapList = JSONObject.parseArray(module.getConfigs(), Map.class);
        } catch (JSONException e) {
            throw new NZException(RCode.MODULE_CONFIGS_FORMAT);
        }

        if (Tool.CollectionUtil.isEmpty(configMapList)) {
            throw new NZException(RCode.MODULE_CONFIGS_ISNULL);
        }

        List<String> configTypeList = Arrays.asList(Constant.MonitorConfType.values()).stream().map(Constant.MonitorConfType::getValue).collect(Collectors.toList());
        for (Map map : configMapList) {
            if(ToolUtil.isEmpty(map.get("type"))){
                throw new NZException(RCode.MODULE_TYPE_ISNULL);
            }
            if(!configTypeList.contains(map.get("type").toString())){
                throw new NZException(RCode.MODULE_TYPE_INCONSISTENT);
            }
        }

        // 每个type 最多包含一个
        long countByType = configMapList.stream().map(map -> map.get("type")).distinct().count();
        if (countByType != configMapList.size()) {
            throw new NZException(RCode.MODULE_CONFIGS_TYPE_DUPLICATE);
        }

        Map metricsConfigMap = configMapList.stream().filter(map -> {
            String configType = (String) map.get("type");
            return StrUtil.equals(Constant.MonitorConfType.METRICS.getValue(), configType);
        }).findFirst().orElse(null);

        if (metricsConfigMap != null && (1 == (Integer) ObjectUtil.defaultIfNull(metricsConfigMap.get("enable"), 1))) {
            Map configMap = (Map) metricsConfigMap.get("config");
            // 端口校验
            Object port = configMap.get("port");
            if (ToolUtil.isNotEmpty(port)) {
                flag = CommonUtils.checkPort(port.toString());
                if (!flag) {
                    throw new NZException(RCode.MODULE_PORT_ERROR);
                }
            }

            String protocol = (String) configMap.get("protocol");

            // 校验 type 类型是否在枚举范围内
            if (!StrUtil.equalsIgnoreCase(Constant.MODULE_TYPE_HTTP, protocol) && !StrUtil.equalsIgnoreCase(Constant.MODULE_TYPE_SNMP, protocol)
            		&& !StrUtil.equalsIgnoreCase(Constant.MODULE_TYPE_HTTPS, protocol)) {
                throw new NZException(RCode.MODULE_PROTOCOL_INVALID);
            }

            // snmp 类型校验 config
            if (StrUtil.equalsIgnoreCase(Constant.MODULE_TYPE_SNMP, protocol)) {
                // snmpCredentialsId 必须存在
                Object snmpCredentialsId = configMap.get("snmpCredentialsId");
                ValidateUtils.is(snmpCredentialsId).notNull(RCode.SNMP_CREDENTIAL_ID_ISNULL);

                SnmpCredential snmpCredential = snmpCredentialService.getById(snmpCredentialsId.toString());
                ValidateUtils.is(snmpCredential).notNull(RCode.SNMP_CREDENTIAL_NOT_FOUND);

                Object walk = configMap.get("walk");
                ValidateUtils.is(walk).notNull(RCode.MODULE_SNMP_WALK_ISNULL);

                try {
                    JSONArray.parseArray(JSONObject.toJSONString(walk));
                } catch (JSONException e) {
                    throw new NZException(RCode.MODULE_SNMP_WALK_FORMAT);
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
                    String type = configMap.get("type").toString();
                    if (StrUtil.equals("file", type)) {
                        ValidateUtils.is(configMap.get("fileName")).notNull(RCode.MODULE_CONFIG_FILENAME_ISNULL);
                    } else if (StrUtil.equals("journal", type)) {
                        ValidateUtils.is(configMap.get("unit")).notNull(RCode.MODULE_CONFIG_JOURNALUNIT_ISNULL);
                    } else if (StrUtil.equals("syslog", type)) {
                        ValidateUtils.is(configMap.get("appName")).notNull(RCode.MODULE_CONFIG_SYSLOG_APPNAME_ISNULL)
                                .and(configMap.get("listenAddress")).notNull(RCode.MODULE_CONFIG_SYSLOG_ADDRESS_ISNULL);
                    } else if (StrUtil.equals("kubernetes", type)) {
                        List<String> roleList = ListUtil.of("endpoints", "service", "pod", "node", "ingress");
                        if (!roleList.contains(configMap.get("role").toString())) {
                            throw new NZException(RCode.MODULE_CONFIG_KUBERNETES_ROLE_INVALID);
                        }
                    }
                }
            }
        }
    }

    @Override
    public Integer saveModule(MonitorModule module) {
        // 1. 校验
        module.setName(StrUtil.trim(module.getName()));
        this.validateModuleInfo(module);

        List<Map> configMapList = JSONObject.parseArray(module.getConfigs(), Map.class);
        Map metricsConfigMap = configMapList.stream().filter(map -> {
            String configType = (String) map.get("type");
            return StrUtil.equals("metrics", configType);
        }).findFirst().orElse(null);

        if (metricsConfigMap != null) {
            Map configMap = (Map) metricsConfigMap.get("config");
            String protocol = (String) configMap.get("protocol");
            // 2. snmp 类型处理
            if (StrUtil.equalsIgnoreCase(Constant.MODULE_TYPE_SNMP, protocol)) {
                // port  缺省值
                Object port = configMap.get("port");
                if (ToolUtil.isEmpty(port)) {
                    configMap.put("port", Constant.SNMP_DEFAULT_PORT);
                    module.setConfigs(JSONObject.toJSONString(configMapList));
                }

                // 处理snmYml字段
                Object snmpCredentialsId = configMap.get("snmpCredentialsId");
                String snmpYml = this.generateYmlFromSnmpParam(snmpCredentialsId.toString(), module);
                module.setSnmpYml(snmpYml);
            } else {
                // http port 缺省值设置
                Object port = configMap.get("port");

                if (ToolUtil.isEmpty(port)) {
                    configMap.put("port", 9100);
                    module.setConfigs(JSONObject.toJSONString(configMapList));
                }
            }
        }

        // 3. endpointNameTmpl
        if (ToolUtil.isEmpty(module.getEndpointNameTmpl())) {
            module.setEndpointNameTmpl("{{module.name}}-{{asset.name}}");
        }

        // 保存
        this.save(module);
        return module.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer updateModule(MonitorModule moduleEntity) {
        // 1. 内置 moduel 不允许修改
        MonitorModule oldModule = this.getById(moduleEntity.getId());
        if (ToolUtil.isEmpty(oldModule)) {
            throw new NZException(RCode.MODULE_NOTFOUND);
        }

        if (oldModule.getBuildIn().equals(1)) {
            throw new NZException(RCode.MODULE_BUILDIN_CAN_NOT_REMOVE);
        }

        // 2. 校验
        moduleEntity.setName(StrUtil.trim(moduleEntity.getName()));
        this.validateModuleInfo(moduleEntity);

        // 3. snmp yml
        List<Map> configMapList = JSONObject.parseArray(moduleEntity.getConfigs(), Map.class);
        Map metricsConfigMap = configMapList.stream().filter(map -> {
            String configType = (String) map.get("type");
            return StrUtil.equals("metrics", configType);
        }).findFirst().get();

        if (metricsConfigMap != null) {
            Map configMap = (Map) metricsConfigMap.get("config");
            String protocol = (String) configMap.get("protocol");
            // snmp 类型 ， 处理 snmpYml 字段 , 如果 module 名称发生改动
            if (StrUtil.equalsIgnoreCase(Constant.MODULE_TYPE_SNMP, protocol)) {
                Object snmpCredentialsId = configMap.get("snmpCredentialsId");
                String snmpYml = this.generateYmlFromSnmpParam(snmpCredentialsId.toString(), moduleEntity);
                moduleEntity.setSnmpYml(snmpYml);
            } else {
                moduleEntity.setSnmpYml("{}");
            }
        }

        // 4. 修改了 project ， 修改 topo config
        if (ObjectUtil.notEqual(oldModule.getProjectId(), moduleEntity.getProjectId())) {
            List<MonitorModule> modules = new ArrayList();
            modules.add(oldModule);
            topoService.removeTopoConfigNodeAndLineByModules(modules);
        }

        // If the dashboard template is modified, the endpoint dashboard template ID will also be modified
        if (ObjectUtil.notEqual(oldModule.getDashboardId(), moduleEntity.getDashboardId())) {
            List<MonitorEndpoint> endpointList = monitorEndpointService.list(new LambdaQueryWrapper<MonitorEndpoint>().eq(MonitorEndpoint::getModuleId, moduleEntity.getId()));
            if (CollUtil.isNotEmpty(endpointList)) {
                List<Integer> endpointIdList = endpointList.stream().map(MonitorEndpoint::getId).collect(Collectors.toList());
                dashboardService.update(new LambdaUpdateWrapper<VisualDashboard>()
                        .set(VisualDashboard::getTmplId, ObjectUtil.defaultIfNull(moduleEntity.getDashboardId(), -1))
                        .in(VisualDashboard::getLink, endpointIdList)
                        .eq(VisualDashboard::getType, DashboardConstant.Type.ENDPOINT.getValue()));
            }
        }

        // 保存
        this.updateById(moduleEntity);
        return moduleEntity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R removeModules(Integer force, Integer... ids) {
        boolean forceData = false;
        List resultList = Tool.ListUtil.list(true);

        List<Integer> idList = new ArrayList<>(Arrays.asList(ids));
        List<MonitorModule> delModules = this.list(new LambdaQueryWrapper<MonitorModule>().in(MonitorModule::getId, idList));
        if (Tool.CollUtil.isEmpty(delModules)) {
            return R.ok().putData("list", resultList);
        }

        // 内置 module 不能删除
        List<Integer> buildInIdList = delModules.stream().filter(monitorModule -> monitorModule.getBuildIn().equals(1)).map(MonitorModule::getId).collect(Collectors.toList());
        if (Tool.CollUtil.isNotEmpty(buildInIdList)) {
            throw new NZException(RCode.MODULE_BUILDIN_CAN_NOT_REMOVE.setParam(buildInIdList.toString()));
        }

        for (MonitorModule moduleEntity : delModules) {
            Map<Object, Object> relObjectInfoMap = this.getModuleEntityDelRelObjectInfoMap(moduleEntity);
            if (Tool.CollUtil.isNotEmpty((List) relObjectInfoMap.get("relates"))) forceData = true;
            resultList.add(relObjectInfoMap);
        }

        force = Tool.ObjectUtil.defaultIfNull(force, 0);
        switch (force) {
            case 0: {// 0：存在关联数据时，删除失败
                if (forceData) {
                    return R.error(RCode.MODULE_RELOBJ_CAN_NOT_REMOVE).putData("list", resultList);
                }
            }
            case 1:  // 1：存在关联数据时，同时删除关联数据
                break;
            default:
                throw new NZException(RCode.ASSET_FORCE_INVALIDE);
        }

        // 删除自身和关联内容
        // module
        this.removeByIds(idList);

        // endpoint
        List<MonitorEndpoint> endpointList = monitorEndpointService.list(new LambdaQueryWrapper<MonitorEndpoint>().in(MonitorEndpoint::getModuleId, idList));
        if (Tool.CollUtil.isNotEmpty(endpointList)) {
            List<Integer> delEndpointIdList = endpointList.stream().map(MonitorEndpoint::getId).collect(Collectors.toList());
            monitorEndpointService.deleteEndpointByIds(delEndpointIdList);
        }

        // topo config
        topoService.removeTopoConfigNodeAndLineByModules(delModules);

        // starred
        starredService.remove(new LambdaQueryWrapper<SysUserStarredEntity>()
                .eq(SysUserStarredEntity::getType, "module")
                .in(SysUserStarredEntity::getTid, idList));

        // alert
        alertMessageService.removeByModuleIds(idList);
        return R.ok().putData("list", resultList);
    }

    /**
     * 获取 module 删除提示关联信息：
     * endpoint
     *
     * @return
     */
    private Map<Object, Object> getModuleEntityDelRelObjectInfoMap(MonitorModule module) {
        List<Map> relatesList = Tool.ListUtil.list(true);

        // endpoint
        List<MonitorEndpoint> endpointList = monitorEndpointService.list(new LambdaQueryWrapper<MonitorEndpoint>().eq(MonitorEndpoint::getModuleId, module.getId()));
        if (Tool.CollUtil.isNotEmpty(endpointList)) {
            for (MonitorEndpoint endpoint : endpointList) {
                Map<Object, Object> map = Tool.MapUtil.builder()
                        .put("id", endpoint.getId())
                        .put("name", endpoint.getName())
                        .put("type", "endpoint")
                        .build();
                relatesList.add(map);
            }
        }
        Map<Object, Object> delRelObjectMap = Tool.MapUtil.builder()
                .put("id", module.getId())
                .put("name", module.getName())
                .put("type", "module")
                .put("relates", relatesList)
                .build();

        return delRelObjectMap;
    }

    private Map handleGenerateParam(String snmpCredentialsId, String moduleConfigs) {
        SnmpCredential snmpCredential = snmpCredentialService.getById(snmpCredentialsId);
        Map moduleConfigMap = JSONObject.parseObject(moduleConfigs, Map.class);

        Map gengrateParams = new HashMap(4);
        gengrateParams.put("walk", moduleConfigMap.get("walk"));
        Integer type = snmpCredential.getType();
        gengrateParams.put("version", type);

        String snmpCredentialConfig = snmpCredential.getConfig();
        Map<String, Object> snmpCredentialConfigMap = JSONObject.parseObject(snmpCredentialConfig, Map.class);

        Map<String, Object> camelToUnderlineMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : snmpCredentialConfigMap.entrySet()) {
            // 驼峰转下划线
            String underlineKey = com.baomidou.mybatisplus.core.toolkit.StringUtils.camelToUnderline(entry.getKey());
            camelToUnderlineMap.put(underlineKey, entry.getValue());
        }

        if (Constant.SNMPV1.equals(type) || Constant.SNMPV2.equals(type)) {
            if (ToolUtil.isNotEmpty(camelToUnderlineMap.get("read_community"))) {
                camelToUnderlineMap.put("community", camelToUnderlineMap.get("read_community"));
            } else {
                camelToUnderlineMap.put("community", Constant.SNMP_DEFAULT_COMMUNITY);
            }
        }

        if (Constant.SNMPV3.equals(type)) {
            if (ToolUtil.isNotEmpty(camelToUnderlineMap.get("auth_pin"))) {
                camelToUnderlineMap.put("password", camelToUnderlineMap.get("auth_pin"));
            }
            if (ToolUtil.isNotEmpty(camelToUnderlineMap.get("priv_pin"))) {
                camelToUnderlineMap.put("priv_password", camelToUnderlineMap.get("priv_pin"));
                camelToUnderlineMap.put("priv_protocol", String.valueOf(camelToUnderlineMap.get("priv_protocol")).replace("-128", "").replace("-", ""));
            }
        }
        // contextname 最终需要格式为 context_name , 因原本不是 驼峰格式 ，这里特殊处理下
        if (camelToUnderlineMap.get("contextname") != null && StrUtil.isNotEmpty(camelToUnderlineMap.get("contextname").toString())) {
            camelToUnderlineMap.put("context_name", camelToUnderlineMap.get("contextname").toString());
        }
        // 删除多余的 keys
        camelToUnderlineMap.remove("auth_pin");
        camelToUnderlineMap.remove("priv_pin");
        camelToUnderlineMap.remove("read_community");
        camelToUnderlineMap.remove("write_community");
        camelToUnderlineMap.remove("contextname");

        gengrateParams.put("auth", camelToUnderlineMap);
        return gengrateParams;
    }

    public String generateYmlFromSnmpParam(String snmpCredentialsId, MonitorModule moduleEntity) {
        // 取出generator路径
        SysConfigEntity entity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_GENERATOR_PATH));
        String generatorPath = entity.getParamValue();

        Map modules = new HashMap();

        Map module = new HashMap();
        List<Map> configMapList = JSONObject.parseArray(moduleEntity.getConfigs(), Map.class);
        Map metricsConfigMap = configMapList.stream().filter(map -> {
            String configType = (String) map.get("type");
            return StrUtil.equals("metrics", configType);
        }).findFirst().get();
        module.put(moduleEntity.getName(), this.handleGenerateParam(snmpCredentialsId, metricsConfigMap.get("config").toString()));

        modules.put("modules", module);

        try {
            log.debug("开始将 snmp参数写入 generator.yml 文件");

            // 将json写入到generator.yml文件中
            yamlUtil.createYaml(generatorPath + File.separator + "generator.yml", JSONObject.toJSONString(modules));

            log.debug("将 snmp参数成功写入 generator.yml 文件");
        } catch (JsonProcessingException e) {
            log.error("将 snmp参数写入 generator.yml 文件失败，参数信息是:" + JSONObject.toJSONString(modules), e);
        } catch (IOException e) {
            log.error("将 snmp参数写入 generator.yml 文件失败，参数信息是:" + JSONObject.toJSONString(modules), e);
        }

        StringReader result = null;
        StringWriter sw = null;
        try {
            log.debug("开始生成 snmp.yml 文件");

            // 先设置环境变量 然后 赋可执行权限 最后 执行生成命令
            String[] envp = {"MIBDIRS=" + generatorPath + File.separator + "mibs"};
            String generatorFile = generatorPath + File.separator + "generator";
            String[] chmodCommand = {"chmod", "+x", generatorFile};
            String[] generateCommand = {generatorFile, "generate"};
            result = RuntimeUtil.run(envp, new File(generatorPath), chmodCommand, generateCommand);

            sw = new StringWriter();
            char[] buf = new char[1024];
            while (result.read(buf) != -1) {
                sw.write(buf);
            }

            log.debug("成功生成 snmp.yml 文件 {}", sw.toString());
        } catch (IOException e) {
            log.error("生成 snmp.yml 文件失败", e);
        } finally {
            if (sw != null) {
                IOUtils.closeQuietly(sw);
            }
            if (result != null) {
                IOUtils.closeQuietly(result);
            }
        }

        String snmpYml = null;

        try {
            // 读取生成之后的snmp.yml 并以json格式
            snmpYml = yamlUtil.yamlToJson(generatorPath + File.separator + "snmp.yml");
            log.debug("读取 snmp.yml 文件的内容成功");
        } catch (FileNotFoundException e) {
            log.error("无法读取snmp.yml文件的内容，找不到 snmp.yml 文件", e);
        } catch (RuntimeException e) {
            log.error("无法读取snmp.yml文件的内容", e);
        }
        return StrUtil.emptyToDefault(snmpYml, "{}");
    }

    @Override
    public void getTemplateByLanguage(HttpServletResponse response, String format) throws IOException {
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_MODULE_EXPORT_HEADER);
        String fileName = "ModuleTemplate";
        basicImportAndExportServices.getTemplate(sysHeaderJsonStr, format, fileName, response);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelImport(String seq) {
        // project
        monitorProjectService.remove(new LambdaQueryWrapper<MonitorProject>().eq(MonitorProject::getSeq, seq));

        // 删除 Module
        this.remove(new LambdaQueryWrapper<MonitorModule>().eq(MonitorModule::getSeq, seq));
    }

    @Override
    public Map importModuleData(MultipartFile multipartFile, Map<String, Object> params) throws IOException {
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_MODULE_EXPORT_HEADER);
        Map<String, String> exportHeaderMap = basicImportAndExportServices.getI18nHeaderMap(sysHeaderJsonStr);
        List<Map<Integer, String>> importDataList = basicImportAndExportServices.getImportDataFromMultipartFile(multipartFile, sysHeaderJsonStr);

        // 表头信息 国际化之后的
        List<String> headerInfos = new ArrayList<>(exportHeaderMap.keySet());

        String fileExtension = FilenameUtils.getExtension(multipartFile.getOriginalFilename());
        params.put("fileExtension", fileExtension);

        // 返回统计信息
        Map statMap = this.saveModulesFromImportDataList(importDataList, headerInfos, params);
        return statMap;
    }

    private Map saveModulesFromImportDataList(List<Map<Integer, String>> importDataList, List<String> headerInfos, Map<String, Object> params) {
        String seq = StrUtil.uuid();
        params.put("seq", seq);
        // 已存在的内容处理方式，可选值：0：错误 error  1：覆盖 override   2：忽略 ignore
        String existed = Tool.ObjectUtil.defaultIfEmpty((String) params.get("existed"), "1");

        // 创建导入校验对象
        ImportValidateUtil validateUtil = ImportValidateUtil.builder()
                .existed(existed).headerInfos(headerInfos)
                .build();
        Integer addNum = 0, updateNum = 0;
        // 记录导入失败内容
        List<Map<Integer, String>> failContentList = ListUtil.list(true);
        for (int i = 0; i < importDataList.size(); i++) {
            int lineNum = i + 1;
            Map saveEntitys = Tool.MapUtil.newHashMap();
            Map<Integer, String> dataMap = importDataList.get(i);

            String moduleName = dataMap.get(MonitorModuleImportEnum.NAME.getIndex());
            validateUtil.validateRequired(moduleName, lineNum, MonitorModuleImportEnum.NAME.getIndex());
            boolean indexDuplicate = validateUtil.validateIndexDuplicate("monitor_module", "name", moduleName, lineNum, MonitorModuleImportEnum.NAME.getIndex());
            // 索引列重复 & 忽略标识，提示忽略本行
            if (BooleanUtil.and(indexDuplicate, StrUtil.equals("2", existed))) {
                validateUtil.ignoreThisRow(lineNum, MonitorModuleImportEnum.NAME.getIndex());
                failContentList.add(dataMap);
                continue;
            }

            String projectName = dataMap.get(MonitorModuleImportEnum.PROJECT.getIndex());
            validateUtil.validateRequired(projectName, lineNum, MonitorModuleImportEnum.PROJECT.getIndex());
            Boolean projectExist = validateUtil.validateRelObjExits("monitor_project", "name", projectName, true, lineNum, MonitorModuleImportEnum.PROJECT.getIndex());
            if (!projectExist) {
                MonitorProject project = new MonitorProject();
                project.setName(projectName);
                saveEntitys.put("project", project);
            }

            String configStr = dataMap.get(MonitorModuleImportEnum.CONFIGURATIONS.getIndex());
            validateUtil.validateRequired(configStr, lineNum, MonitorModuleImportEnum.CONFIGURATIONS.getIndex());
            boolean isJsonArray = validateUtil.validateJsonArray(configStr, lineNum, MonitorModuleImportEnum.CONFIGURATIONS.getIndex());
            if (isJsonArray) {
                List<Map> configMapList = JSONObject.parseArray(configStr, Map.class);
                configMapList = Tool.CollUtil.defaultIfEmpty(configMapList, new ArrayList<>());

                int countByType = (int) configMapList.stream().map(map -> map.get("type")).distinct().count();
                boolean configDuplicateByType = (countByType == configMapList.size());
                if (!configDuplicateByType) {
                    validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_DUPLICATE.setParam(MonitorModuleImportEnum.CONFIGURATIONS.getIndex()));
                }

                List<String> configTypeList = Arrays.asList(Constant.MonitorConfType.values()).stream().map(Constant.MonitorConfType::getValue).collect(Collectors.toList());
                for (Map map : configMapList) {
                    validateUtil.validateRequired(map.get("type"), lineNum, MonitorModuleImportEnum.CONFIGURATIONS.getIndex());
                    validateUtil.validateEnumValArr(map.get("type").toString(), configTypeList, lineNum, MonitorModuleImportEnum.CONFIGURATIONS.getIndex());
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
                    validateUtil.validateEnumValArr(protocol, protocolEnumValueList, lineNum, MonitorModuleImportEnum.CONFIGURATIONS.getIndex());

                    Object port = configMap.get("port");
                    if (ToolUtil.isNotEmpty(port)) {
                        validateUtil.validatePort(port.toString(), lineNum, MonitorModuleImportEnum.CONFIGURATIONS.getIndex());
                    }

                    // snmp 类型校验 config
                    if (StrUtil.equals(Constant.MODULE_TYPE_SNMP, protocol)) {
                        Object snmpCredentialsId = configMap.get("snmpCredentialsId");
                        validateUtil.validateRequired(snmpCredentialsId, lineNum, MonitorModuleImportEnum.CONFIGURATIONS.getIndex());
                        String snmpCredentialsIdStr = ObjectUtil.isNotEmpty(snmpCredentialsId) ? snmpCredentialsId.toString() : StrUtil.EMPTY;
                        validateUtil.validateRelObjExits("snmp_credential", "id", snmpCredentialsIdStr, false, lineNum, MonitorModuleImportEnum.CONFIGURATIONS.getIndex());

                        Object walk = configMap.get("walk");
                        validateUtil.validateRequired(walk, lineNum, MonitorModuleImportEnum.CONFIGURATIONS.getIndex());
                        validateUtil.validateJsonArray(walk != null ? JSONObject.toJSONString(walk) : "", lineNum, MonitorModuleImportEnum.CONFIGURATIONS.getIndex());
                    }
                    Map<String, Object> labels = MapUtil.defaultIfEmpty((Map) configMap.get("labels"), Collections.emptyMap());
                    labels.keySet().stream().forEach(labelKey -> {
                        validateUtil.validateRegex(labelKey, Constant.PROMETHEUS_LABELNAME_EXPRESSION, lineNum, MonitorModuleImportEnum.CONFIGURATIONS.getIndex());
                    });

                    List<Map> relabelConfigList = ObjectUtil.defaultIfNull((List) configMap.get("relabel_config"), Collections.emptyList());
                    for (Map map : relabelConfigList) {
                        List<String> sourceLabels = ObjectUtil.defaultIfNull((List) map.get("source_labels"), Collections.emptyList());
                        for (String sourceLabel : sourceLabels) {
                            validateUtil.validateRegex(sourceLabel, Constant.PROMETHEUS_LABELNAME_EXPRESSION, lineNum, MonitorModuleImportEnum.CONFIGURATIONS.getIndex());
                        }
                        String targetLabel = (String) map.get("target_label");
                        if (StrUtil.isNotEmpty(targetLabel)) {
                            validateUtil.validateRegex(targetLabel, Constant.PROMETHEUS_LABELNAME_EXPRESSION, lineNum, MonitorModuleImportEnum.CONFIGURATIONS.getIndex());
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
                        String type = configMap.get("type").toString();
                        if (StrUtil.equals("file", type)) {
                            validateUtil.validateRequired(configMap.get("fileName"), lineNum, MonitorModuleImportEnum.CONFIGURATIONS.getIndex());
                        } else if (StrUtil.equals("journal", type)) {
                            validateUtil.validateRequired(configMap.get("unit"), lineNum, MonitorModuleImportEnum.CONFIGURATIONS.getIndex());
                        } else if (StrUtil.equals("syslog", type)) {
                            validateUtil.validateRequired(configMap.get("appName"), lineNum, MonitorModuleImportEnum.CONFIGURATIONS.getIndex());
                            validateUtil.validateRequired(configMap.get("listenAddress"), lineNum, MonitorModuleImportEnum.CONFIGURATIONS.getIndex());
                        } else if (StrUtil.equals("kubernetes", type)) {
                            List<String> roleList = ListUtil.of("endpoints", "service", "pod", "node", "ingress");
                            validateUtil.validateEnumValArr(configMap.get("role").toString(), roleList, lineNum, MonitorModuleImportEnum.CONFIGURATIONS.getIndex());
                        } else {
                            validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_ENUM_VALUE_ERROR.setParam(MonitorModuleImportEnum.CONFIGURATIONS.getIndex()));
                        }
                    }
                }
            }

            String dashboardTemplateName = dataMap.get(MonitorModuleImportEnum.DASHBOARD_TEMPLATE.getIndex());
            Integer dashboardTemplateId = null;
            if (StrUtil.isNotEmpty(dashboardTemplateName)) {
                // dashboard template, type = template  varType = endpoint
                VisualDashboard dashboardTemplate = dashboardService.getOne(new LambdaQueryWrapper<VisualDashboard>()
                        .eq(VisualDashboard::getName, dashboardTemplateName)
                        .eq(VisualDashboard::getType, DashboardConstant.Type.TEMPLATE.getValue())
                        .eq(VisualDashboard::getVarType, DashboardConstant.VariableType.ENDPOINT.getValue()));
                if (ObjectUtil.isNull(dashboardTemplate)) {
                    validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_RELATED_OBJ_NOT_EXIST.setParam(AssetModelImportEnum.DASHBOARD_TEMPLATE.getIndex()));
                } else {
                    dashboardTemplateId = dashboardTemplate.getId();
                }
            }

            List<Map<String, String>> errMsgForThisLine = validateUtil.getImportErrMsgForThisLine(lineNum);
            // 遇到错误是否继续导入，可选值： 0：不继续    1：继续    默认值：1
            String ignoreError = Tool.ObjectUtil.defaultIfEmpty((String) params.get("ignoreError"), "1");
            if (BooleanUtil.and(Tool.StrUtil.equals("0", ignoreError), Tool.CollUtil.isNotEmpty(errMsgForThisLine))) {
                // 结束本次导入，自导入行开始 到结尾数据行，提示错误信息
                for (int j = i; j < importDataList.size(); j++) {
                    validateUtil.recordImportAttrErr(j + 1, RCode.IMPORT_ERROR_STOP);
                    failContentList.add(importDataList.get(j));
                }
                break;
            }

            if (Tool.CollUtil.isEmpty(errMsgForThisLine)) {
                String endpointNameTmpl = StrUtil.emptyToDefault(dataMap.get(MonitorModuleImportEnum.ENDPOINT_NAME_TEMPLATE.getIndex()), "{{module.name}}-{{asset.name}}");
                String remark = dataMap.get(MonitorModuleImportEnum.REMARK.getIndex());

                MonitorModule module = new MonitorModule();
                if (saveEntitys.get("project") == null) {
                    MonitorProject project = monitorProjectService.getOne(new LambdaQueryWrapper<MonitorProject>().eq(MonitorProject::getName, projectName));
                    module.setProjectId(project.getId());
                }
                module.setName(moduleName);
                module.setEndpointNameTmpl(endpointNameTmpl);
                module.setRemark(remark);
                module.setDashboardId(dashboardTemplateId);
                module.setConfigs(configStr);
                List<Map> configMapList = JSONObject.parseArray(configStr, Map.class);
                Map metricsConfigMap = configMapList.stream().filter(map -> {
                    String configType = (String) map.get("type");
                    return StrUtil.equals(Constant.MonitorConfType.METRICS.getValue(), configType);
                }).findFirst().get();
                if (Tool.CollUtil.isNotEmpty(metricsConfigMap)) {
                    Map configMap = (Map) metricsConfigMap.get("config");
                    String protocol = (String) configMap.get("protocol");
                    // 补充 port 缺省值
                    Object port = configMap.get("port");
                    if (StrUtil.equals(Constant.MODULE_TYPE_SNMP, protocol)) {
                        if (ToolUtil.isEmpty(port)) {
                            configMap.put("port", 161);
                        }
                        // 处理 snmYml 字段
                        String snmpYml = this.generateYmlFromSnmpParam(configMap.get("snmpCredentialsId").toString(), module);
                        module.setSnmpYml(snmpYml);
                    } else {
                        if (ToolUtil.isEmpty(port)) {
                            configMap.put("port", 9100);
                        }
                    }
                }
                module.setConfigs(JSONObject.toJSONString(configMapList));
                saveEntitys.put("module", module);

                TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(new DefaultTransactionDefinition());
                try {
                    this.saveOrUpdateRowData(saveEntitys, params);
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
                    log.error("Module import roll back the transaction failure", e);
                }
            } else {
                failContentList.add(dataMap);
            }
        }
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_MODULE_EXPORT_HEADER);
        String failedContentFileBase64Str = basicImportAndExportServices.getFailedContentFileBase64Str(failContentList, sysHeaderJsonStr, MapUtil.getStr(params, "fileExtension"));
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

    private void saveOrUpdateRowData(Map saveEntitys, Map<String, Object> params) {
        MapUtil.removeAny(params, "save", "update");
        String seq = params.get("seq").toString();

        MonitorProject project = (MonitorProject) saveEntitys.get("project");
        if (project != null) {
            project.setSeq(seq);
            monitorProjectService.save(project);
        }

        MonitorModule module = (MonitorModule) saveEntitys.get("module");
        if (module.getProjectId() == null && project != null) {
            module.setProjectId(project.getId());
        }
        module.setSeq(seq);

        // 已存在的内容处理方式，可选值：0：错误 error  1：覆盖 override   2：忽略 ignore   在此只需要处理更新即可
        String existed = Tool.ObjectUtil.defaultIfEmpty((String) params.get("existed"), "1");
        MonitorModule oldModule = this.getOne(new LambdaQueryWrapper<MonitorModule>().eq(MonitorModule::getName, module.getName()));

        if (StrUtil.equals("1", existed) && oldModule != null) {
            params.put("update", true);
            // update & sync
            module.setId(oldModule.getId());

            // module
            this.updateById(module);

            // project topo node
            if (!oldModule.getProjectId().equals(module.getProjectId())) {
                List<MonitorModule> modules = Tool.ListUtil.of(oldModule);
                topoService.removeTopoConfigNodeAndLineByModules(modules);
            }

            // If the dashboard template is modified, the endpoint dashboard template ID will also be modified
            if (ObjectUtil.notEqual(oldModule.getDashboardId(), module.getDashboardId())) {
                List<MonitorEndpoint> endpointList = monitorEndpointService.list(new LambdaQueryWrapper<MonitorEndpoint>().eq(MonitorEndpoint::getModuleId, module.getId()));
                if (CollUtil.isNotEmpty(endpointList)) {
                    List<Integer> endpointIdList = endpointList.stream().map(MonitorEndpoint::getId).collect(Collectors.toList());
                    dashboardService.update(new LambdaUpdateWrapper<VisualDashboard>()
                            .set(VisualDashboard::getTmplId, ObjectUtil.defaultIfNull(module.getDashboardId(), -1))
                            .in(VisualDashboard::getLink, endpointIdList)
                            .eq(VisualDashboard::getType, DashboardConstant.Type.ENDPOINT.getValue()));
                }
            }

            // 是否同步更新关联的endpoint，可选值：  0：不同步  1：同步  默认值：1
            String syncEndpoint = Tool.ObjectUtil.defaultIfEmpty((String) params.get("syncEndpoint"), "1");
            if (StrUtil.equals("1", syncEndpoint)) {
                List<MonitorEndpoint> endpointList = monitorEndpointService.list(new LambdaQueryWrapper<MonitorEndpoint>().eq(MonitorEndpoint::getModuleId, module.getId()));
                if (Tool.CollUtil.isNotEmpty(endpointList)) {
                    List<Integer> syncEndpointIdList = endpointList.stream().map(MonitorEndpoint::getId).collect(Collectors.toList());
                    monitorEndpointService.syncConfigByIdList(syncEndpointIdList);
                }
            }

            // 是否同步更新关联的 dashboard，可选值： 0：不同步  1：同步  默认值：1
            String syncDashboard = Tool.ObjectUtil.defaultIfEmpty((String) params.get("syncDashboard"), "1");
            if (StrUtil.equals("1", syncDashboard) && ObjectUtil.isNotNull(module.getDashboardId())) {
                visualService.syncTemplateDashboard(null, null, null, module.getId());
            }
        } else {
            params.put("save", true);
            // insert
            this.save(module);
        }
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
        PageUtils page = this.queryPage(params);
        List<MonitorModule> list = (List<MonitorModule>) page.getList();

        List<List<String>> exportDataList = this.handleExportDataList(list);

        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_MODULE_EXPORT_HEADER);
        String fileName = "Modules";
        String formatStr = params.get("format").toString();
        basicImportAndExportServices.exportDataByFormat(response, exportDataList, sysHeaderJsonStr, formatStr, fileName);
    }

    @Override
    public List<AssetAsset> getAssetListByModuleId(Integer moduleId) {
        ValidateUtils.is(moduleId).notNull(RCode.MODULE_ID_ISNULL);

        List<AssetAsset> assetList = this.baseMapper.queryAssetListByModuleId(moduleId);
        return assetList;
    }

    @Override
    public List<Dc> getDcListByModuleId(Integer moduleId) {
        ValidateUtils.is(moduleId).notNull(RCode.MODULE_ID_ISNULL);

        List<Dc> dcList = this.baseMapper.queryDcListByModuleId(moduleId);
        return dcList;
    }

    private List<List<String>> handleExportDataList(List<MonitorModule> list) {
        List<List<String>> exportDataList = new ArrayList<>();
        for (MonitorModule module : list) {
            List<String> tempList = new ArrayList<>();
            tempList.add(module.getName());
            tempList.add(module.getProject().getName());
            tempList.add(ObjectUtil.defaultIfNull(module.getEndpointNameTmpl(), ""));
            tempList.add(ObjectUtil.defaultIfNull(module.getConfigs(), ""));
            VisualDashboard dashboard = module.getDashboard();
            tempList.add(ObjectUtil.isNotNull(dashboard) ? dashboard.getName() : StrUtil.EMPTY);
            tempList.add(ObjectUtil.defaultIfNull(module.getRemark(), "").toString());
            exportDataList.add(tempList);
        }
        return exportDataList;
    }

    @Override
    public String queryAssetMoudules(Integer assetId) {
        return this.baseMapper.queryAssetMoudules(assetId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncModuleEndpointAndDashboard(List<Integer> syncModuleIdList, Integer isSyncEndpoint, Integer isSyncDashboard) {
        log.info("[syncModuleEndpointAndDashboard] [begin] [syncModuleIdList: {}] [isSyncEndpoint: {}] [isSyncDashboard: {}]", JSONUtil.toJsonStr(syncModuleIdList), isSyncEndpoint, isSyncDashboard);
        if (ObjectUtil.equal(1, isSyncEndpoint)) {
            List<MonitorEndpoint> syncEndpointList = monitorEndpointService.list(new LambdaQueryWrapper<MonitorEndpoint>().in(MonitorEndpoint::getModuleId, syncModuleIdList));
            if (CollUtil.isEmpty(syncEndpointList)) {
                log.warn("[syncModuleEndpointAndDashboard] [module has no endpoint to sync] [syncModuleIdList: {}]", JSONUtil.toJsonStr(syncModuleIdList));
            } else {
                List<Integer> syncEndpointIdList = syncEndpointList.stream().map(MonitorEndpoint::getId).collect(Collectors.toList());
                monitorEndpointService.syncConfigByIdList(syncEndpointIdList);
            }
        }

        if (ObjectUtil.equal(1, isSyncDashboard)) {
            List<MonitorModule> moduleList = this.getBaseMapper().selectBatchIds(syncModuleIdList);
            ValidateUtils.is(moduleList).notNull(RCode.MODULE_NOTFOUND);

            for (MonitorModule module : moduleList) {
                visualService.syncTemplateDashboard(null, null, null, module.getId());
            }
        }
        log.info("[syncModuleEndpointAndDashboard] [finshed]");
    }

}
