package com.nis.modules.module.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.module.dao.ModuleDao;
import com.nis.modules.module.entity.Module;
import com.nis.modules.module.service.ModuleService;

@Service("moduleService")
public class ModuleServiceImpl  extends ServiceImpl<ModuleDao, Module> implements ModuleService {
	
	@Autowired
	private ModuleDao moduleDao;
	
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
	}

}
