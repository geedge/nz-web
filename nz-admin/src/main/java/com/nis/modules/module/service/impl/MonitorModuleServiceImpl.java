package com.nis.modules.module.service.impl;

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
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.module.dao.MonitorModuleDao;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.module.service.MonitorModuleService;
import com.nis.modules.project.entity.MonitorProject;
import com.nis.modules.project.service.MonitorProjectService;
import com.nis.modules.project.service.TopoService;
import com.nis.modules.sys.entity.SnmpCredential;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SnmpCredentialService;
import com.nis.modules.sys.service.SysConfigService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


    @Override
    public MonitorModule queryModuleEntity(Integer id) {
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

        int alertNum = alertMessageService.count(new LambdaQueryWrapper<AlertMessageEntity>().eq(AlertMessageEntity::getModuleId, module.getId()).eq(AlertMessageEntity::getState, 1));
        module.setAlertNum(Integer.valueOf(String.valueOf(alertNum)));
        return module;
    }

    /**
     * 处理请求参数
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

        IPage<MonitorModule> page = new Query<MonitorModule>().getPage(params);
        List<MonitorModule> datas = monitorModuleDao.queryPage(page, params);

        if (CollectionUtils.isNotEmpty(datas)) {
            // 处理返回结果
            this.handleResuleList(datas);
        }

        page.setRecords(datas);
        return new PageUtils(page);
    }

    private void handleResuleList(List<MonitorModule> moduleList) {
        List<MonitorEndpoint> endpointList = monitorEndpointService.list();
        List<AlertMessageEntity> alertList = alertMessageService.list(new LambdaQueryWrapper<AlertMessageEntity>().eq(AlertMessageEntity::getState, 1));

        for (MonitorModule module : moduleList) {
            List<MonitorEndpoint> endpoints = endpointList.stream().filter(endpoint -> module.getId().equals(endpoint.getModuleId())).collect(Collectors.toList());
            module.setEndpointNum(endpoints.size());

            long assetNum = endpoints.stream().map(MonitorEndpoint::getAssetId).distinct().count();
            module.setAssetNum(Integer.valueOf(String.valueOf(assetNum)));

            long alertNum = alertList.stream().filter(alertMessageEntity -> module.getId().equals(alertMessageEntity.getModuleId())).count();
            module.setAlertNum(Integer.valueOf(String.valueOf(alertNum)));
        }
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

        // 3.校验 type 类型是否在枚举范围内
        String type = module.getType();
        if (!StrUtil.equals(Constant.MODULE_TYPE_HTTP, type) && !StrUtil.equals(Constant.MODULE_TYPE_SNMP, type)) {
            throw new NZException(RCode.MODULE_TYPE_INVALID);
        }

        // 4. config 格式校验
        Map configMap;
        try {
            configMap = JSONObject.parseObject(module.getConfigs(), Map.class);
        } catch (JSONException e) {
            throw new NZException(RCode.MODULE_CONFIGS_FORMAT);
        }

        // 5. 端口校验
        Object port = configMap.get("port");
        if (ToolUtil.isNotEmpty(port)) {
            flag = CommonUtils.checkPort(port.toString());
            if (!flag) {
                throw new NZException(RCode.MODULE_PORT_ERROR);
            }
        }

        // snmp 类型校验 config
        if (StrUtil.equals(Constant.MODULE_TYPE_SNMP, type)) {
            // snmpCredentialsId 必须存在
            Object snmpCredentialsId = configMap.get("snmpCredentialsId");
            ValidateUtils.is(snmpCredentialsId).notNull(RCode.SNMP_CREDENTIAL_ID_ISNULL);

            SnmpCredential snmpCredential = snmpCredentialService.getById(snmpCredentialsId.toString());
            ValidateUtils.is(snmpCredential).notNull(RCode.SNMP_CREDENTIAL_NOT_FOUND);

            Object walk = configMap.get("walk");
            ValidateUtils.is(walk).notNull(RCode.MODULE_SNMP_WALK_ISNULL);

            try {
                JSONArray.parseArray(JSONObject.toJSONString(walk));
            } catch (Exception e) {
                throw new NZException(RCode.MODULE_SNMP_WALK_FORMAT);
            }
        }
    }

    @Override
    public Integer saveModule(MonitorModule module) {
        // 1. 校验
        this.validateModuleInfo(module);

        // 2. snmp 类型处理
        if (StrUtil.equals(Constant.MODULE_TYPE_SNMP, module.getType())) {
            // port  缺省值
            Map configMap = JSONObject.parseObject(module.getConfigs(), Map.class);
            Object port = configMap.get("port");

            if (ToolUtil.isEmpty(port)) {
                configMap.put("port", Constant.SNMP_DEFAULT_PORT);
                module.setConfigs(JSONObject.toJSONString(configMap));
            }

            // 处理snmYml字段
            Object snmpCredentialsId = configMap.get("snmpCredentialsId");
            String snmpYml = this.generateYmlFromSnmpParam(snmpCredentialsId.toString(), module);
            module.setSnmpYml(snmpYml);
        } else {
            // http port 缺省值设置
            Map configMap = JSONObject.parseObject(module.getConfigs(), Map.class);
            Object port = configMap.get("port");

            if (ToolUtil.isEmpty(port)) {
                configMap.put("port", 9100);
                module.setConfigs(JSONObject.toJSONString(configMap));
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

        // 3. snmp 类型 ， 处理 snmpYml 字段 , 如果 module 名称发生改动
        if (StrUtil.equals(Constant.MODULE_TYPE_SNMP, moduleEntity.getType())) {
            Map configMap = JSONObject.parseObject(moduleEntity.getConfigs(), Map.class);
            Object snmpCredentialsId = configMap.get("snmpCredentialsId");
            String snmpYml = this.generateYmlFromSnmpParam(snmpCredentialsId.toString(), moduleEntity);
            moduleEntity.setSnmpYml(snmpYml);
        }else{
            moduleEntity.setSnmpYml("{}");
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

        if (Constant.SNMPV3.equals(type)) {
            if (ToolUtil.isNotEmpty(camelToUnderlineMap.get("auth_pin"))) {
                camelToUnderlineMap.put("password", camelToUnderlineMap.get("auth_pin"));
            }
            if (ToolUtil.isNotEmpty(camelToUnderlineMap.get("priv_pin"))) {
                camelToUnderlineMap.put("priv_password", camelToUnderlineMap.get("priv_pin"));
            }
        }
        // 删除多余的 keys
        camelToUnderlineMap.remove("auth_pin");
        camelToUnderlineMap.remove("priv_pin");

        gengrateParams.put("auth", camelToUnderlineMap);
        return gengrateParams;
    }

    public String generateYmlFromSnmpParam(String snmpCredentialsId, MonitorModule moduleEntity) {
        // 取出generator路径
        SysConfigEntity entity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_GENERATOR_PATH));
        String generatorPath = entity.getParamValue();

        Map modules = new HashMap();

        Map module = new HashMap();
        module.put(moduleEntity.getName(), this.handleGenerateParam(snmpCredentialsId, moduleEntity.getConfigs()));

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
        return snmpYml;
    }

}
