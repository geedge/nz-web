package com.nis.modules.module.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.common.utils.Constant.AlertType;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.endpoint.service.EndpointService;
import com.nis.modules.module.dao.ModuleDao;
import com.nis.modules.module.entity.Module;
import com.nis.modules.module.service.ModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service("moduleService")
public class ModuleServiceImpl  extends ServiceImpl<ModuleDao, Module> implements ModuleService {

	@Autowired
	private ModuleDao moduleDao;
	@Autowired
	private AlertRuleService alertRuleService;
	@Autowired
	private AlertMessageService alertMessageService;
	@Autowired
	private EndpointService endpointService;

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
		ValidateUtils.is(moduleEntity.getProjectId()).notNull(RCode.MODULE_PROJECTID_ISNULL)
				.and(moduleEntity.getName()).notNull(RCode.MODULE_NAME_ISNULL)
				.and(type).notNull(RCode.MODULE_TYPE_ISNULL);

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
        List<Module> list = this.list(new QueryWrapper<Module>().eq("name", moduleEntity.getName()).eq("project_id", moduleEntity.getProjectId()));
        boolean flag = false;
        //当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) ||
                (list != null && list.size() == 1 && list.get(0).getId().equals(moduleEntity.getId()))) {
            flag = true;
        }
        if(!flag){
            throw new NZException(RCode.MODULE_NAME_DUPLICATE);
        }
		//ValidateUtils.is(moduleEntity.getPort()).notNull(RCode.MODULE_PORT_ISNULL);
		//ValidateUtils.is(moduleEntity.getPath()).notNull(RCode.MODULE_PATH_ISNULL);
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
					Map authInfo = JSONObject.parseObject(params.get("auth").toString(), Map.class);

					// 默认为noAuthNoPriv。可以是 noAuthNoPriv（不认证不加密），authNoPriv（认证不加密）或 authPriv（认证且加密）
					String securityLevel = (String) authInfo.get("security_level");

					String username = (String) authInfo.get("username");

					if (StringUtils.isEmpty(username)) {
						throw new NZException(RCode.MODULE_USERNAME_INVALID);
					}

					// 不为 null 时校验参数是否正确
					if (StringUtils.isNotEmpty(securityLevel)) {
						String password = (String) authInfo.get("password");

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
			}
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeHandle(List ids) {
		moduleDao.deleteBatchIds(ids);
		//同时删除endpoint信息
		endpointService.removeByModuleIds(ids);
		//删除alert相关信息
		alertRuleService.removeByTypeIds(AlertType.ALERT_TYPE_MODULE.getCode(), ids);
		alertMessageService.removeByTypeIds(AlertType.ALERT_TYPE_MODULE.getCode(), ids);
	}

	@Override
	public List queryModuleIdsByProjectIds(List projectIds) {
		return moduleDao.selectModuleIdsByProjectIds(projectIds);
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
