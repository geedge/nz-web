package com.nis.modules.module.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant.AlertType;
import com.nis.common.utils.CommonUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.endpoint.service.EndpointService;
import com.nis.modules.module.dao.ModuleDao;
import com.nis.modules.module.entity.Module;
import com.nis.modules.module.service.ModuleService;

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
		ValidateUtils.is(moduleEntity.getProjectId()).notNull(RCode.MODULE_PROJECTID_ISNULL);
		ValidateUtils.is(moduleEntity.getName()).notNull(RCode.MODULE_NAME_ISNULL);
		ValidateUtils.is(moduleEntity.getPort()).notNull(RCode.MODULE_PORT_ISNULL);
		ValidateUtils.is(moduleEntity.getPath()).notNull(RCode.MODULE_PATH_ISNULL);
		boolean flag = CommonUtils.checkPort(moduleEntity.getPort().toString());
		if(!flag) {
			throw new NZException(RCode.MODULE_PORT_ERROR);
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

}
