package com.nis.modules.module.service.impl;

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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.service.AlertMessageService;
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
import com.nis.modules.sys.entity.SnmpCredential;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SnmpCredentialService;
import com.nis.modules.sys.service.SysConfigService;
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
import java.io.*;
import java.net.URLEncoder;
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
    private DataSourceTransactionManager dataSourceTransactionManager;

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

        Integer alertNum = alertMessageService.getAactiveAlertCountByModuleId(module.getId());
        module.setAlertNum(alertNum);

        String chartIds = module.getChartIds();
        if (StrUtil.isNotBlank(chartIds)) {
            List<VisualChart> charts = (List<VisualChart>) visualChartService.listByIds(Arrays.asList(chartIds.split(",")));
            module.setCharts(charts);
        }
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

        IPage page = new Query(MonitorModule.class).getPage(params);
        List<MonitorModule> datas = monitorModuleDao.queryPage(page, params);

        page.setRecords(datas);
        return new PageUtils(page);
    }

    @Override
    public List<MonitorProject> queryModuleTree(String name) {
        List<MonitorModule> moduleList = this.list(new LambdaQueryWrapper<MonitorModule>().like(StringUtils.isNotEmpty(name), MonitorModule::getName, name));
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
            module.setEndpointNameTmpl("{{asse.host}}-{{module.name}}");
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
    public void removeModules(Integer... ids) {
        List<MonitorModule> list = this.list();

        // 1. 内置的 配置不允许删除
        List<Integer> buildInIdList = list.stream().filter(assetTypeConf -> assetTypeConf.getBuildIn().equals(1)).map(MonitorModule::getId).collect(Collectors.toList());
        buildInIdList.retainAll(Arrays.asList(ids));
        if (CollectionUtils.isNotEmpty(buildInIdList)) {
            throw new NZException("These modules are built-in and cannot be deleted. Details: " + buildInIdList.toString(), RCode.MODULE_BUILDIN_CAN_NOT_REMOVE.getCode());
        }

        // 2. 含有 endpoint 的 module 不允许删除
        List<MonitorEndpoint> monitorEndpointList = monitorEndpointService.list();
        List<Integer> userModuleIdList = monitorEndpointList.stream().map(MonitorEndpoint::getModuleId).distinct().collect(Collectors.toList());
        userModuleIdList.retainAll(Arrays.asList(ids));
        if (CollectionUtils.isNotEmpty(userModuleIdList)) {
            throw new NZException("These modules have been used by endpoints and cannot be deleted. Details: " + userModuleIdList.toString(), RCode.MODULE_REMOVE_ERROR.getCode());
        }

        // 删除 module 时 删除topo图中 节点和连线
        List<MonitorModule> modules = this.list(new QueryWrapper<MonitorModule>().lambda().in(MonitorModule::getId, ids));
        if (CollectionUtils.isEmpty(modules)) {
            return;
        }
        topoService.removeTopoConfigNodeAndLineByModules(modules);

        // 删除
        this.removeByIds(Arrays.asList(ids));

        //删除alert相关信息
        alertMessageService.removeByModuleIds(Arrays.asList(ids));
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
    public void getExportTemplateByLanguage(HttpServletResponse response, String language) {
        SysConfigEntity configEntity = sysConfigService.getOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_MODULE_EXPORT_HEADER));
        Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);
        Map<String, String> exportHeaderMap;
        if (map.get(language) != null) {
            exportHeaderMap = (Map) map.get(language);
        } else {
            throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
        }
        SXSSFWorkbook workbook = null;
        try {
            workbook = ExcelUtils.createExcel07(exportHeaderMap, "Module", true);
            String fileName = "ModuleExportTemplate.xlsx";
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            log.error("Module 模板生成失败，错误信息是：" + e.getMessage(), e);
        } catch (RuntimeException e) {
            log.error("获取 Module 导入模板失败，表头语言是：" + language + " ，错误信息是：" + e.getMessage(), e);
        } finally {
            if (workbook != null) {
                IOUtils.closeQuietly(workbook);
            }
        }
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
    public Map importDatacentersFromExcel(MultipartFile excelFile, String language) {
        Workbook work = ExcelUtils.getWorkBookFromMultipartFile(excelFile);

        Sheet sheet = work.getSheetAt(0);

        // 表头校验
        SysConfigEntity config = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_MODULE_EXPORT_HEADER));
        HashMap<String, Object> map = JSONObject.parseObject(config.getParamValue(), HashMap.class, Feature.OrderedField);
        boolean headerFlag = ExcelUtils.validateExcelHeader(sheet, sheet.getFirstRowNum(), map.values());
        if (!headerFlag) {
            throw new NZException(RCode.EXCELFILE_HEADER_TEMPLATE_ERROR);
        }

        Map<String, String> exportHeader = (Map) map.get(language);
        List<String> headerInfos = new ArrayList(exportHeader.keySet());

        List<Map> failDetails = new ArrayList<>();
        String seqUUID = UUID.randomUUID().toString();

        int successNum = this.saveModulesFromSheet(sheet, failDetails, headerInfos, seqUUID);
        int totalNum = sheet.getLastRowNum() - sheet.getFirstRowNum();

        Map resultMap = new HashMap(8);
        resultMap.put("seq", seqUUID);
        resultMap.put("totalNum", totalNum);
        resultMap.put("successNum", successNum);
        resultMap.put("failNum", totalNum - successNum);
        resultMap.put("failDetail", CommonUtils.handleImportFailDetails(failDetails));
        return resultMap;
    }

    private int saveModulesFromSheet(Sheet sheet, List<Map> failDetails, List<String> headerInfos, String seqUUID) {
        int successNum = 0;
        for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            int dataFromExcelRowNum = i + 1;
            boolean saveFlag = true;

            String moduleName = ExcelUtils.getCellValue(row.getCell(0));
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

            String projectName = ExcelUtils.getCellValue(row.getCell(1));
            if (StringUtils.isEmpty(projectName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.PROJECT_NAME_ISNULL.getMsg());
            }

            String configStr = ExcelUtils.getCellValue(row.getCell(3));
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

            String chartNames = ExcelUtils.getCellValue(row.getCell(4));
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
                String endpointNameTmpl = ExcelUtils.getCellValue(row.getCell(2));
                String remark = ExcelUtils.getCellValue(row.getCell(5));

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
                    endpointNameTmpl = StrUtil.isEmpty(endpointNameTmpl) ? "{{asse.host}}-{{module.name}}" : endpointNameTmpl;
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
    public void export(HttpServletResponse response, Map<String, Object> params) {
        SysConfigEntity configEntity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_MODULE_EXPORT_HEADER));
        Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);

        PageUtils page = this.queryPage(params);
        List<MonitorModule> list = (List<MonitorModule>) page.getList();

        SXSSFWorkbook workbook = null;
        try {
            // 添加标题行 默认语言 en
            String language = (String) params.get("language");
            language = ToolUtil.isEmpty(language) ? "en" : language;

            String sheetName = "Monitor modules";
            workbook = ExcelUtils.createExcel07((Map) map.get(language), sheetName, false);
            SXSSFSheet sheet = workbook.getSheet(sheetName);

            this.fillData(list, 1, sheet, workbook);

            // 响应为流
            String fileName = "Modules.xlsx";
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            log.error("Modules 导出文件生成失败，错误信息是：" + e.getMessage(), e);
        } catch (RuntimeException e) {
            log.error("Dc 导出失败，参数是：" + params.toString() + " ，错误信息是：" + e.getMessage(), e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }

    private void fillData(List<MonitorModule> list, int rowNum, SXSSFSheet sheet, SXSSFWorkbook workbook) {
        CellStyle dataStyle = ExcelUtils.createCellStyle(workbook, (short) 13, false);
        for (MonitorModule module : list) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(module.getName());
            row.createCell(1).setCellValue(module.getProject().getName());
            row.createCell(2).setCellValue(ObjectUtil.defaultIfNull(module.getEndpointNameTmpl(), ""));
            row.createCell(3).setCellValue(ObjectUtil.defaultIfNull(module.getConfigs(), ""));
            String chartIds = module.getChartIds();
            String chartNames = "";
            if (StrUtil.isNotEmpty(chartIds)) {
                List<VisualChart> chartList = visualChartService.list(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getPanelId, 0).in(VisualChart::getId, Arrays.asList(chartIds.split(","))));
                for (VisualChart chart : chartList) {
                    chartNames += chart.getName() + ",";
                }
                chartNames = StrUtil.isNotEmpty(chartNames) ? chartNames.substring(0, chartNames.length() - 1) : chartNames;
            }
            row.createCell(4).setCellValue(ObjectUtil.defaultIfNull(chartNames, "").toString());
            row.createCell(5).setCellValue(ObjectUtil.defaultIfNull(module.getRemark(), "").toString());

            // 设置单元格样式
            ExcelUtils.setDataCellStyle(dataStyle, row, 5);
        }
    }

}
