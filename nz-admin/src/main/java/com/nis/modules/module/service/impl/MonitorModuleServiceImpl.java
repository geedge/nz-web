package com.nis.modules.module.service.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.module.dao.MonitorModuleDao;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.module.service.MonitorModuleService;
import com.nis.modules.panel.entity.VisualChart;
import com.nis.modules.panel.service.VisualChartService;
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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
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
    private VisualChartService visualChartService;

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

        Map<Integer, Long> ruleIdAndAlertCountMap = alertMessageList.parallelStream().collect(Collectors.groupingBy(AlertMessageEntity::getRuleId, Collectors.counting()));
        List<Map<String, Object>> alert = statService.getSeverityStatByRuleMsgCount(Tool.MapUtil.empty(), ruleIdAndAlertCountMap);
        module.setAlert(alert);

        String chartIds = module.getChartIds();
        if (StrUtil.isNotBlank(chartIds)) {
            List<VisualChart> charts = (List<VisualChart>) visualChartService.listByIds(Arrays.asList(chartIds.split(",")));
            module.setCharts(charts);
        }

        // 当前用户收藏状态  1：收藏  0：未收藏
        int count = starredService.count(new LambdaQueryWrapper<SysUserStarredEntity>()
                .eq(SysUserStarredEntity::getTid, module.getId())
                .eq(SysUserStarredEntity::getType, "module")
                .eq(SysUserStarredEntity::getUid, ShiroUtils.getUserId().intValue())
        );

        module.setStarred(count == 0 ? 0 : 1);
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
                    if ("file".equals(configMap.get("type").toString()) && ToolUtil.isEmpty(configMap.get("fileName"))) {
                        throw new NZException(RCode.MODULE_CONFIG_FILENAME_ISNULL);
                    }
                }
            }
        }
    }

    @Override
    public Integer saveModule(MonitorModule module) {
        // 1. 校验
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
        if (!oldModule.getProjectId().equals(moduleEntity.getProjectId())) {
            List<MonitorModule> modules = new ArrayList();
            modules.add(oldModule);
            topoService.removeTopoConfigNodeAndLineByModules(modules);
        }

        // 保存
        this.updateById(moduleEntity);
        return moduleEntity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Map> removeModules(Integer force, Integer... ids) {
        List<Map> delRelObjectMsgList = Tool.ListUtil.list(true);

        List<Integer> idList = new ArrayList<>(Arrays.asList(ids));
        List<MonitorModule> delModules = this.list(new LambdaQueryWrapper<MonitorModule>().in(MonitorModule::getId, idList));
        if (Tool.CollUtil.isEmpty(delModules)) {
            return delRelObjectMsgList;
        }

        // 内置 module 不能删除
        List<Integer> buildInIdList = delModules.stream().filter(monitorModule -> monitorModule.getBuildIn().equals(1)).map(MonitorModule::getId).collect(Collectors.toList());
        if (Tool.CollUtil.isNotEmpty(buildInIdList)) {
            throw new NZException(RCode.MODULE_BUILDIN_CAN_NOT_REMOVE.setParam(buildInIdList.toString()));
        }

        delModules.forEach(moduleEntity -> {
            Map<Object, Object> relObjectInfoMap = this.getModuleEntityDelRelObjectInfoMap(moduleEntity);
            if (Tool.CollUtil.isNotEmpty(relObjectInfoMap)) {
                delRelObjectMsgList.add(relObjectInfoMap);
            }
        });

        force = Tool.ObjectUtil.defaultIfNull(force, 0);
        switch (force) {
            case 0: {// 0：存在关联数据时，删除失败
                if (Tool.CollUtil.isNotEmpty(delRelObjectMsgList)) {
                    return delRelObjectMsgList;
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
        return delRelObjectMsgList;
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
        if (Tool.CollUtil.isNotEmpty(relatesList)) {
            Map<Object, Object> delRelObjectMap = Tool.MapUtil.builder()
                    .put("id", module.getId())
                    .put("name", module.getName())
                    .put("type", "module")
                    .put("relates", relatesList)
                    .build();

            return delRelObjectMap;
        }
        return Tool.MapUtil.empty();
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
    public Map importModuleData(MultipartFile multipartFile) throws IOException {
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_MODULE_EXPORT_HEADER);
        Map<String, String> exportHeaderMap = basicImportAndExportServices.getI18nHeaderMap(sysHeaderJsonStr);
        List<Map<Integer, String>> importDataList = basicImportAndExportServices.getImportDataFromMultipartFile(multipartFile, sysHeaderJsonStr);

        List<Map> failDetails = new ArrayList<>();
        String seqUUID = UUID.randomUUID().toString();

        int successNum = this.saveModulesFromImportDataList(importDataList, failDetails, new ArrayList<>(exportHeaderMap.keySet()), seqUUID);
        int totalNum = importDataList.size();

        Map resultMap = new HashMap(8);
        resultMap.put("seq", seqUUID);
        resultMap.put("totalNum", totalNum);
        resultMap.put("successNum", successNum);
        resultMap.put("failNum", totalNum - successNum);
        resultMap.put("failDetail", CommonUtils.handleImportFailDetails(failDetails));
        return resultMap;
    }

    private int saveModulesFromImportDataList(List<Map<Integer, String>> importDataList, List<Map> failDetails, List<String> headerInfos, String seqUUID) {
        int successNum = 0;
        for (int i = 0; i < importDataList.size(); i++) {
            int dataFromExcelRowNum = i + 1;
            boolean saveFlag = true;
            Map<Integer, String> dataMap = importDataList.get(i);

            String moduleName = dataMap.get(0);
            if (StringUtils.isEmpty(moduleName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.MODULE_NAME_ISNULL.getMsg());
            } else {
                int countByName = this.count(new QueryWrapper<MonitorModule>().lambda().eq(MonitorModule::getName, moduleName));
                if (countByName > 0) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.MODULE_NAME_DUPLICATE.getMsg());
                }
            }

            String projectName = dataMap.get(1);
            if (StringUtils.isEmpty(projectName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.PROJECT_NAME_ISNULL.getMsg());
            }

            String configStr = dataMap.get(3);
            if (StringUtils.isEmpty(configStr)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.MODULE_CONFIGS_ISNULL.getMsg());
            } else {
                List<Map> configMapList = null;
                try {
                    configMapList = JSONObject.parseArray(configStr, Map.class);
                } catch (JSONException e) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.MODULE_CONFIGS_FORMAT.getMsg());
                }
                if (saveFlag && Tool.CollectionUtil.isEmpty(configMapList)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.MODULE_CONFIGS_ISNULL.getMsg());
                }else if(saveFlag && Tool.CollectionUtil.isNotEmpty(configMapList)){
                    long countByType = configMapList.stream().map(map -> map.get("type")).distinct().count();
                    if (countByType != configMapList.size()) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.MODULE_CONFIGS_TYPE_DUPLICATE.getMsg());
                    }
                    Map metricsConfigMap = configMapList.stream().filter(map -> {
                        String configType = (String) map.get("type");
                        return StrUtil.equals("metrics", configType);
                    }).findFirst().get();
                    if (metricsConfigMap != null && (1 == (Integer) ObjectUtil.defaultIfNull(metricsConfigMap.get("enable"), 1))) {
                        Map configMap = (Map) metricsConfigMap.get("config");
                        Object port = configMap.get("port");
                        if (ToolUtil.isNotEmpty(port)) {
                            if (!CommonUtils.checkPort(port.toString())) {
                                saveFlag = false;
                                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.MODULE_PORT_ERROR.getMsg());
                            }
                        }
                        String protocol = (String) configMap.get("protocol");
                        if (!StrUtil.equalsIgnoreCase(Constant.MODULE_TYPE_HTTP, protocol) && !StrUtil.equalsIgnoreCase(Constant.MODULE_TYPE_SNMP, protocol)
                        		&& !StrUtil.equalsIgnoreCase(Constant.MODULE_TYPE_HTTPS, protocol)) {
                            saveFlag = false;
                            this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.MODULE_PROTOCOL_INVALID.getMsg());
                        }

                        if (StrUtil.equalsIgnoreCase(Constant.MODULE_TYPE_SNMP, protocol)) {
                            Object snmpCredentialsId = configMap.get("snmpCredentialsId");
                            if (ToolUtil.isEmpty(snmpCredentialsId)) {
                                saveFlag = false;
                                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.SNMP_CREDENTIAL_ID_ISNULL.getMsg());
                            }

                            SnmpCredential snmpCredential = snmpCredentialService.getById(snmpCredentialsId.toString());
                            if (ToolUtil.isEmpty(snmpCredential)) {
                                saveFlag = false;
                                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.SNMP_CREDENTIAL_NOT_FOUND.getMsg());
                            }

                            Object walk = configMap.get("walk");
                            if (ToolUtil.isEmpty(walk)) {
                                saveFlag = false;
                                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.MODULE_SNMP_WALK_ISNULL.getMsg());
                            }
                            try {
                                JSONArray.parseArray(JSONObject.toJSONString(walk));
                            } catch (JSONException e) {
                                saveFlag = false;
                                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.MODULE_SNMP_WALK_FORMAT.getMsg());
                            }
                        }

                        Map<String, Object> labels = MapUtil.defaultIfEmpty((Map) configMap.get("labels"), Collections.emptyMap());
                        for (String labelKey : labels.keySet()) {
                            if (!ReUtil.isMatch(Constant.PROMETHEUS_LABELNAME_EXPRESSION, labelKey)) {
                                saveFlag = false;
                                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.PROMETHEUS_LABELNAME_FORMAT_ERROR.getMsg());
                            }
                        }

                        List<Map> relabelConfigList = ObjectUtil.defaultIfNull((List) configMap.get("relabel_config"), Collections.emptyList());
                        for (Map map : relabelConfigList) {
                            List<String> sourceLabels = ObjectUtil.defaultIfNull((List) map.get("source_labels"), Collections.emptyList());
                            for (String sourceLabel : sourceLabels) {
                                if (!ReUtil.isMatch(Constant.PROMETHEUS_LABELNAME_EXPRESSION, sourceLabel)) {
                                    saveFlag = false;
                                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.PROMETHEUS_LABELNAME_FORMAT_ERROR.getMsg());
                                }
                            }
                            String targetLabel = (String) map.get("target_label");
                            if (StrUtil.isNotEmpty(targetLabel)) {
                                if (!ReUtil.isMatch(Constant.PROMETHEUS_LABELNAME_EXPRESSION, targetLabel)) {
                                    saveFlag = false;
                                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.PROMETHEUS_LABELNAME_FORMAT_ERROR.getMsg());
                                }
                            }
                        }
                    }
                }
            }

            String chartNames = dataMap.get(4);
            StringBuilder relChartTemplateIds = new StringBuilder();
            if (StringUtils.isNotEmpty(chartNames)) {
                String[] split = chartNames.split(",");
                for (String chartName : split) {
                    List<VisualChart> list = visualChartService.list(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getPanelId, 0).eq(VisualChart::getName, chartName));
                    // VarType 2 为  endpoint
                    list = list.stream().filter(chart -> chart.getVarType() == 2).collect(Collectors.toList());
                    if (Tool.CollectionUtil.isEmpty(list)) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.CHART_NOTEXSITS.getMsg());
                    } else if (list.size() == 1) {
                        // 类型为 endpoint ,且通过名称只关联一个
                        relChartTemplateIds.append(list.get(0).getId());
                        relChartTemplateIds.append(",");
                    } else if (list.size() > 1) {
                        saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.CHART_NAME_DUPLICATE_NOTLINK.getMsg());
                    }
                }
            }

            if (saveFlag) {
                String endpointNameTmpl = dataMap.get(2);
                String remark = dataMap.get(5);

                DefaultTransactionDefinition def = new DefaultTransactionDefinition();
                TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(def);

                MonitorModule module = new MonitorModule();
                try {
                    MonitorProject project = monitorProjectService.getOne(new LambdaQueryWrapper<MonitorProject>().eq(MonitorProject::getName, projectName));
                    if (project == null) {
                        project = new MonitorProject();
                        project.setSeq(seqUUID);
                        project.setName(projectName);
                        monitorProjectService.save(project);
                    }

                    module.setProjectId(project.getId());
                    module.setName(moduleName);
                    endpointNameTmpl = StrUtil.isEmpty(endpointNameTmpl) ? "{{module.name}}-{{asset.name}}" : endpointNameTmpl;
                    module.setEndpointNameTmpl(endpointNameTmpl);
                    module.setRemark(remark);
                    String chartIds = relChartTemplateIds.toString();
                    module.setChartIds(StrUtil.isNotEmpty(chartIds) ? chartIds.substring(0, chartIds.length() - 1) : "");
                    module.setSeq(seqUUID);
                    module.setConfigs(configStr);

                    List<Map> configMapList = JSONObject.parseArray(module.getConfigs(), Map.class);
                    Map metricsConfigMap = configMapList.stream().filter(map -> {
                        String configType = (String) map.get("type");
                        return StrUtil.equals("metrics", configType);
                    }).findFirst().get();

                    Map configMap = (Map) metricsConfigMap.get("config");
                    String protocol = (String) configMap.get("protocol");
                    if (StrUtil.equals(Constant.MODULE_TYPE_SNMP, protocol)) {
                        Object port = configMap.get("port");
                        if (ToolUtil.isEmpty(port)) {
                            configMap.put("port", Constant.SNMP_DEFAULT_PORT); // port  缺省值
                            module.setConfigs(JSONObject.toJSONString(configMapList));
                        }

                        // 处理snmYml字段
                        Object snmpCredentialsId = configMap.get("snmpCredentialsId");
                        String snmpYml = this.generateYmlFromSnmpParam(snmpCredentialsId.toString(), module);
                        module.setSnmpYml(snmpYml);
                    } else {
                        Object port = configMap.get("port");
                        if (ToolUtil.isEmpty(port)) {
                            configMap.put("port", 9100); // http port 缺省值设置
                            module.setConfigs(JSONObject.toJSONString(configMapList));
                        }
                    }

                    this.save(module);

                    dataSourceTransactionManager.commit(transactionStatus);
                    successNum++;
                } catch (RuntimeException e) {
                    dataSourceTransactionManager.rollback(transactionStatus);
                    log.error(String.format("module 导入保存失败,实体信息是：%s,错误信息是：%s", JSONObject.toJSONString(module), e.getMessage()), e);
                    this.addFailDetail(dataFromExcelRowNum, failDetails, "System", RCode.EXCELFILE_IMPORT_ERROR.getMsg());
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
            String chartIds = module.getChartIds();
            String chartNames = "";
            if (StrUtil.isNotEmpty(chartIds)) {
                List<VisualChart> chartList = visualChartService.list(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getPanelId, 0).in(VisualChart::getId, Arrays.asList(chartIds.split(","))));
                for (VisualChart chart : chartList) {
                    chartNames += chart.getName() + ",";
                }
                chartNames = StrUtil.isNotEmpty(chartNames) ? chartNames.substring(0, chartNames.length() - 1) : chartNames;
            }
            tempList.add(ObjectUtil.defaultIfNull(chartNames, "").toString());
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
    public void deleteModuleChartTemps(List<Integer> chartIdList) { monitorModuleDao.deleteModuleChartTemps(chartIdList); }
}
