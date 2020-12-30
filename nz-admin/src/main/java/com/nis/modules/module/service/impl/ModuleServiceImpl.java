package com.nis.modules.module.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.asset.dao.AssetDao;
import com.nis.modules.endpoint.dao.EndpointDao;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.endpoint.service.EndpointService;
import com.nis.modules.module.dao.ModuleDao;
import com.nis.modules.module.entity.Module;
import com.nis.modules.module.service.ModuleService;
import com.nis.modules.overview.service.OverviewService;
import com.nis.modules.project.service.TopoService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Service("moduleService")
public class ModuleServiceImpl  extends ServiceImpl<ModuleDao, Module> implements ModuleService {

    protected Log logger = Log.get();

	@Autowired
	private ModuleDao moduleDao;
	@Autowired
	private AlertMessageService alertMessageService;
	@Autowired
	private EndpointService endpointService;
    @Autowired
    private YamlUtil yamlUtil;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private TopoService topoService;

    @Autowired
    private AssetDao assetDao;

    @Autowired
    private OverviewService overviewService;

    @Autowired
    private EndpointDao endpointDao;

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		IPage<Module> page = new Query<Module>().getPage(params);
		List<Module> datas = moduleDao.queryPage(page, params);
        page.setRecords(datas);
        return new PageUtils(page);
	}

	@Override
	public void validateParam(Module moduleEntity) {
		String type = moduleEntity.getType();
		// 默认type为http
		if(StringUtils.isEmpty(type)){
			type = Constant.MODULE_TYPE_HTTP;
			moduleEntity.setType(type);
		}
		ValidateUtils.is(moduleEntity.getProjectId()).notNull(RCode.MODULE_PROJECTID_ISNULL)
				.and(moduleEntity.getName()).notNull(RCode.MODULE_NAME_ISNULL);
				// .and(type).notNull(RCode.MODULE_TYPE_ISNULL);

		// 校验 type 类型是否在枚举范围内
		if (Constant.MODULE_TYPE_HTTP.equals(type)) {
			moduleEntity.setSnmpParam(null);
		}else if(Constant.MODULE_TYPE_SNMP.equals(type)){
			if(moduleEntity.getPort() == null){
				moduleEntity.setPort(Constant.SNMP_DEFAULT_PORT);
			}
		}else{
			throw new NZException(RCode.MODULE_TYPE_INVALID);
		}

        // 名称不能重复
        List<Module> list = this.list(new QueryWrapper<Module>().lambda().eq(Module::getName, moduleEntity.getName()));
        boolean flag = false;
        //当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) ||
                (list != null && list.size() == 1 && list.get(0).getId().equals(moduleEntity.getId()))) {
            flag = true;
        }
        if(!flag){
            throw new NZException(RCode.MODULE_NAME_DUPLICATE);
        }

		ValidateUtils.is(moduleEntity.getName()).maxLength(64,RCode.MODULE_NAME_TOO_LONG);
		ValidateUtils.is(moduleEntity.getRemark()).maxLength(1024,RCode.MODULE_REMARK_TOO_LONG);
		if (moduleEntity.getPort() != null) {
		    flag = CommonUtils.checkPort(moduleEntity.getPort().toString());
            if(!flag) {
                throw new NZException(RCode.MODULE_PORT_ERROR);
            }
        }

		// 校验 snmpParam参数是否合法
		String snmpParam = moduleEntity.getSnmpParam();
		if (StringUtils.isNotEmpty(snmpParam)) {

			// snmp param 参数
			Map params = JSONObject.parseObject(snmpParam, Map.class);

			// 注： snmp version 实际版本 为 1，2，3 ，此处只做 2，3 版本限制
			Integer version = (Integer) params.get("version");
			if (version != null) {
				if (version < 2 || version > 3) {
					// 版本不在可选范围内
					throw new NZException(RCode.MODULE_SNMPVERSION_INVALID);
				}

                if (version.equals(3)) {
                    this.ValidateAuthForSnmpV3(params.get("auth").toString());
                }
            }
        }

		// 校验 labels 格式
        String labels = moduleEntity.getLabels();
        if (StringUtils.isNotEmpty(labels)) {
            Map labelMap;
            try {
                labelMap = JSONObject.parseObject(labels, Map.class);
            } catch (Exception e) {
                throw new NZException(RCode.MODULE_LABELS_FORMAT);
            }

            labelMap.keySet().stream().forEach(key -> {
                if (!Constant.PROMETHEUS_LABELSKEY_PATTERN.matcher(key.toString()).matches()) {
                    throw new NZException("labels's key " + key + " format is incorrect", RCode.MODULE_LABELS_KEY_FORMAT.getCode());
                }
            });
        }
    }

    // 当snmp版本为V3时，需要校验用户信息
    @Override
    public void ValidateAuthForSnmpV3(String auth) {
        Map authInfo = JSONObject.parseObject(auth, Map.class);

        // 默认为noAuthNoPriv。可以是 noAuthNoPriv（不认证不加密），authNoPriv（认证不加密）或 authPriv（认证且加密）
        String securityLevel = (String) authInfo.get("security_level");

        String username = (String) authInfo.get("username");

        if (StringUtils.isEmpty(username)) {
            throw new NZException(RCode.MODULE_USERNAME_INVALID);
        }

        // 不为 null 时校验参数是否正确
        if (StringUtils.isNotEmpty(securityLevel)) {
            String password = StrUtil.str((String)authInfo.get("password"));

            String privPassword = (String) authInfo.get("priv_password");
            String privProtocol = (String) authInfo.get("priv_protocol");
            String authProtocol = (String) authInfo.get("auth_protocol");


            if (securityLevel.equals("authPriv")) { // 认证且加密
                if (StringUtils.isEmpty(privPassword)) {
                    throw new NZException(RCode.MODULE_PRIVPASSWORD_ISNULL);
                }
                // 不为 null 则进行枚举校验
                if (StringUtils.isNotEmpty(privProtocol) && !privProtocol.equals("DES") && !privProtocol.equals("AES")) {
                    throw new NZException(RCode.MODULE_PRIVPROTOCOL_INVALID);
                }

                if (StringUtils.isEmpty(password)) {
                    throw new NZException(RCode.MODULE_PASSWORD_INVALID);
                }
                // 不为 null 则进行枚举校验
                if (StringUtils.isNotEmpty(authProtocol) && !authProtocol.equals("MD5") && !authProtocol.equals("SHA")) {
                    throw new NZException(RCode.MODULE_AUTHPROTOCOL_INVALID);
                }
            } else if (securityLevel.equals("authNoPriv")) { // 认证不加密
                if (StringUtils.isEmpty(password)) {
                    throw new NZException(RCode.MODULE_PASSWORD_INVALID);
                }
                // 不为 null 则进行枚举校验
                if (StringUtils.isNotEmpty(authProtocol) && !authProtocol.equals("MD5") && !authProtocol.equals("SHA")) {
                    throw new NZException(RCode.MODULE_AUTHPROTOCOL_INVALID);
                }
            } else if (securityLevel.equals("noAuthNoPriv")) {
                // do nothhing
            } else {
                // 不在枚举范围内
                throw new NZException(RCode.MODULE_AUTHSECURITYLEVEL_INVALID);
            }
        }
    }

    @Override
    public Map getModuleStatById(Integer moduleId) {
        if (this.getById(moduleId) == null) {
            throw new NZException(RCode.MODULE_NOTFOUND);
        }
        Map resultMap = new HashMap(4);

        List<Endpoint> endpointList = endpointService.list(new QueryWrapper<Endpoint>().lambda().eq(Endpoint::getModuleId, moduleId));
        Set<Integer> assetIds = endpointList.stream().map(Endpoint::getAssetId).collect(Collectors.toSet());
        resultMap.put("assetStat", this.getAssetUpStatByAssetIds(assetIds));

        Set<Integer> endpointIds = endpointList.stream().map(Endpoint::getId).collect(Collectors.toSet());
        resultMap.put("endpointStat", this.getEndpointUpStatByEndpointIds(endpointIds));

        resultMap.put("alertStat", this.getAlertStatByModuleId(moduleId));
        return resultMap;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateModule(Module moduleEntity) {
        // 校验
        this.validateBulitinRule(Arrays.asList(moduleEntity.getId()));
        this.validateParam(moduleEntity);

        // 处理snmYml字段 , 如果module名称发生改动 并且 snmpParam不为空则重新生成snmpYml字段
        Module module = this.getById(moduleEntity.getId());
        String snmpParam = moduleEntity.getSnmpParam();
        if (StringUtils.isNotEmpty(snmpParam) || !moduleEntity.getName().equals(module.getName())) {
            String snmpYml = this.generateYmlFromSnmpParam(JSONObject.parseObject(snmpParam, Map.class), moduleEntity);
            moduleEntity.setSnmpYml(snmpYml);
        }
        this.updateById(moduleEntity);

        // 修改了 project ， 修改 topo config
        if (!module.getProjectId().equals(moduleEntity.getProjectId())) {
            List modules = new ArrayList();
            modules.add(module);
            topoService.removeNodeAndLineByModules(modules);
        }
    }

    @Override
    public void saveModule(Module moduleEntity) {
        this.validateParam(moduleEntity);

        // 设置默认路径
        if (Constant.MODULE_TYPE_HTTP.equals(moduleEntity.getType()) && ToolUtil.isEmpty(moduleEntity.getPath())) {
            moduleEntity.setPath(Constant.MODULE_HTTP_DEFAULT_PATH);
        }

        // 处理snmYml字段
        String snmpParam = moduleEntity.getSnmpParam();
        if (StringUtils.isNotEmpty(snmpParam)) {
            String snmpYml = this.generateYmlFromSnmpParam(JSONObject.parseObject(snmpParam, Map.class), moduleEntity);
            moduleEntity.setSnmpYml(snmpYml);
        }
        this.save(moduleEntity);
    }

    private Map getAlertStatByModuleId(Integer moduleId) {
        Map alertStatMap = new HashMap(4);
        List<AlertMessageEntity> alertsByModuleId = alertMessageService.list(new QueryWrapper<AlertMessageEntity>().lambda().eq(AlertMessageEntity::getState, 1).eq(AlertMessageEntity::getModuleId, moduleId));
        if (CollectionUtils.isEmpty(alertsByModuleId)) {
            alertStatMap.put(Constant.ALERT_SEVERITY_HIGH, 0);
            alertStatMap.put(Constant.ALERT_SEVERITY_MEDIUM, 0);
            alertStatMap.put(Constant.ALERT_SEVERITY_LOW, 0);
            return alertStatMap;
        }
        long high = alertsByModuleId.stream().filter(alert -> Constant.ALERT_SEVERITY_HIGH.equals(alert.getSeverity())).count();
        long medium = alertsByModuleId.stream().filter(alert -> Constant.ALERT_SEVERITY_MEDIUM.equals(alert.getSeverity())).count();
        long low = alertsByModuleId.stream().filter(alert -> Constant.ALERT_SEVERITY_LOW.equals(alert.getSeverity())).count();
        alertStatMap.put(Constant.ALERT_SEVERITY_HIGH, high);
        alertStatMap.put(Constant.ALERT_SEVERITY_MEDIUM, medium);
        alertStatMap.put(Constant.ALERT_SEVERITY_LOW, low);
        return alertStatMap;
    }

    private Map getEndpointUpStatByEndpointIds(Set<Integer> endpointIds) {
        Map endpointStatMap = new HashMap<>(4);
        if (CollectionUtils.isEmpty(endpointIds)) {
            endpointStatMap.put("up", 0);
            endpointStatMap.put("down", 0);
            return endpointStatMap;
        }
        // 根据 asset 停用状态 获取对应 endpoint ids
        List<Integer> suspendedEndpointIds = endpointDao.getSuspendedEndpointIdsByAssetState();
        // 停用的 endpoint ids
        List<Integer> disabledEndpointIds = endpointDao.getDisabledEndpointIds();
        suspendedEndpointIds.addAll(disabledEndpointIds);
        suspendedEndpointIds = suspendedEndpointIds.stream().distinct().collect(Collectors.toList());

        // 健康的 endpoint ids
        Set<Integer> upStatusEndpointIds = overviewService.getUpStatusEndpointIds(suspendedEndpointIds);
        int totalEndpointNum = endpointIds.size();
        Set<Integer> tempSet = new HashSet<>(endpointIds);
        endpointIds.retainAll(upStatusEndpointIds);
        tempSet.retainAll(suspendedEndpointIds);
        endpointStatMap.put("up", endpointIds.size());
        endpointStatMap.put("suspended", tempSet.size());
        endpointStatMap.put("down", totalEndpointNum - endpointIds.size() - tempSet.size());
        return endpointStatMap;
    }

    private Map getAssetUpStatByAssetIds(Set<Integer> assetIds) {
        Map assetStatMap = new HashMap<>(4);
        SysConfigEntity pingSwitchConfig = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_ASSET_PING_SWITCH));
        if (CollectionUtils.isEmpty(assetIds) || !Constant.ASSET_PING_OPEN_SWITCH.equals(pingSwitchConfig.getParamValue())) {
            assetStatMap.put("up", 0);
            assetStatMap.put("down", 0);
            return assetStatMap;
        }

        List<Integer> pingStatusByAssetIds = assetDao.getPingStatusByAssetIds(assetIds);
        long up = pingStatusByAssetIds.stream().filter(status -> status != null && status == 1).count();
        long down = pingStatusByAssetIds.stream().filter(status -> status != null && status == 0).count();
        assetStatMap.put("up", up);
        assetStatMap.put("down", down);
        return assetStatMap;
    }

    // 根据snmp param生成
    @Override
    public String generateYmlFromSnmpParam(Map snmpParam, Module moduleEntity) {
        // 取出generator路径
        SysConfigEntity entity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_GENERATOR_PATH));
        String generatorPath = entity.getParamValue();

        Map modules = new HashMap();

        Map module = new HashMap();
        module.put(moduleEntity.getName(), snmpParam);

        modules.put("modules", module);

        try {
            logger.debug("Start writing snmp param to the generator.yml file");

            // 将json写入到generator.yml文件中
            yamlUtil.createYaml(generatorPath + File.separator + "generator.yml", JSONObject.toJSONString(modules));

            logger.debug("Write snmp param to generator.yml file succeeded");
        } catch (JsonProcessingException e) {
            logger.error("Failed to write snmp param to generator.yml file",e);
        } catch (IOException e) {
            logger.error("Failed to write snmp param to generator.yml file",e);
        }

        StringReader result = null;
        StringWriter sw = null;
        try {
            logger.debug("Start generate snmp.yml file");

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

            logger.debug("Generate snmp.yml file succeeded {}", sw.toString());
        } catch (Exception e) {
            logger.error("Failed to generate snmp.yml file",e);
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
            logger.debug("Read the contents of snmp.yml file succeeded");
        } catch (FileNotFoundException e) {
            logger.error("Failed to read the contents of snmp.yml file, because the snmp.yml file not found",e);
        } catch (Exception e) {
            logger.error("Failed to read the contents of snmp.yml file",e);
        }

        return snmpYml;
    }

	@Override
    @Transactional(rollbackFor = Exception.class)
    public void removeHandle(List ids) {
    	//查询是否有endpoint信息 有的话不允许删除
    	List<Endpoint> endpoints = endpointService.list(new QueryWrapper<Endpoint>().lambda().in(Endpoint::getModuleId, ids));    	
    	if(ToolUtil.isEmpty(endpoints)) {
            // 删除 module 时 删除topo图中 节点和连线
            List<Module> modules = this.list(new QueryWrapper<Module>().lambda().in(Module::getId, ids));
            if (CollectionUtils.isEmpty(modules)) {
                return;
            }
            topoService.removeNodeAndLineByModules(modules);

    		moduleDao.deleteBatchIds(ids);
            //删除alert相关信息
            alertMessageService.removeByModuleIds(ids);
    	}else {
    		Set<Integer> moduleIds = endpoints.stream().map(Endpoint::getModuleId).collect(Collectors.toSet());
    		List errorIds = new ArrayList();
        	for(Object id : ids) {
        		if(moduleIds.contains(id)) {
        			errorIds.add(id);
        		};
        	}
        	List<Module> modules = this.list(new QueryWrapper<Module>().lambda().in(Module::getId, errorIds));
        	Set<String> moduleNames = modules.stream().map(Module::getName).collect(Collectors.toSet());
    		throw new NZException("These modules contains endpoint can't remove, modules info : "+moduleNames.toString(),RCode.MODULE_REMOVE_ERROR.getCode());
    	}
    }

	@Override
	public void validateBulitinRule(List<Integer> ids) {
		List<Module> objs = moduleDao.selectBatchIds(ids);
		for(Module obj : objs) {
			if(obj!=null&&obj.getBuildIn().equals(1)){
				throw new NZException(RCode.MODULE_BUILDIN_CAN_NOT_REMOVE);
			}
		}

	}

}
