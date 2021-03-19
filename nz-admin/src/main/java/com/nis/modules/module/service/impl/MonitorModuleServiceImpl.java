package com.nis.modules.module.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.module.dao.MonitorModuleDao;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.module.service.MonitorModuleService;
import com.nis.modules.project.entity.MonitorProject;
import com.nis.modules.project.service.MonitorProjectService;
import com.nis.modules.project.service.TopoService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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


    @Override
    public MonitorModule queryModuleEntity(Integer id) {
        MonitorModule module = this.getById(id);
        if (ToolUtil.isEmpty(module)) {
            throw new NZException(RCode.MODULE_NOTFOUND);
        }
        MonitorProject project = monitorProjectService.getById(module.getProjectId());
        module.setProject(project);
        return module;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MonitorModule> page = new Query<MonitorModule>().getPage(params);
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

        // 3.校验 type 类型是否在枚举范围内
        String type = module.getType();
        if (!StrUtil.equals(Constant.MODULE_TYPE_HTTP, type) && !StrUtil.equals(Constant.MODULE_TYPE_SNMP, type)) {
            throw new NZException(RCode.MODULE_TYPE_INVALID);
        }

        // 4. 端口校验
        if (module.getPort() != null) {
            flag = CommonUtils.checkPort(module.getPort().toString());
            if (!flag) {
                throw new NZException(RCode.MODULE_PORT_ERROR);
            }
        }

        // 5. config 格式校验
        Map configMap;
        try {
            configMap = JSONObject.parseObject(module.getConfigs(), Map.class);
        } catch (JSONException e) {
            throw new NZException(RCode.MODULE_CONFIGS_FORMAT);
        }

        // snmp 类型校验 config
        if (StrUtil.equals(Constant.MODULE_TYPE_SNMP, type)) {
            // 注： snmp version 实际版本 为 1，2，3 ，此处只做 2，3 版本限制
            Integer version = (Integer) configMap.get("version");
            if (version != null) {
                if (version < 2 || version > 3) {
                    // 版本不在可选范围内
                    throw new NZException(RCode.MODULE_SNMPVERSION_INVALID);
                }

                if (version.equals(3)) {
                    SnmpUtil.ValidateAuthForSnmpV3((Map<String, String>) configMap.get("auth"));
                }
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
            if (ToolUtil.isEmpty(module.getPort())) {
                module.setPort(Constant.SNMP_DEFAULT_PORT);
            }

            // 处理snmYml字段
            String configs = module.getConfigs();
            String snmpYml = this.generateYmlFromSnmpParam(JSONObject.parseObject(configs, Map.class), module);
            module.setSnmpYml(snmpYml);
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
            String snmpYml = this.generateYmlFromSnmpParam(JSONObject.parseObject(moduleEntity.getConfigs(), Map.class), moduleEntity);
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

        // 删除
        this.removeByIds(Arrays.asList(ids));
    }

    public String generateYmlFromSnmpParam(Map snmpParam, MonitorModule moduleEntity) {
        // 取出generator路径
        SysConfigEntity entity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_GENERATOR_PATH));
        String generatorPath = entity.getParamValue();

        Map modules = new HashMap();

        Map module = new HashMap();
        module.put(moduleEntity.getName(), snmpParam);

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
        } catch (Exception e) {
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
        } catch (Exception e) {
            log.error("无法读取snmp.yml文件的内容", e);
        }
        return snmpYml;
    }

}
