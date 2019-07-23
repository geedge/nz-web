package com.nis.modules.sys.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.sys.dao.SystemInfoDao;
import com.nis.modules.sys.entity.SystemInfoEntity;
import com.nis.modules.sys.service.SystemInfoService;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;


/**
 * 业务系统
 *
 */
@Service("systemInfoService")
public class SystemInfoServiceImpl extends ServiceImpl<SystemInfoDao, SystemInfoEntity> implements SystemInfoService {

	@Override
	public List<SystemInfoEntity> querySystemInfosByUserId(Long userId) {
		List<SystemInfoEntity> systemInfos = baseMapper.querySystemInfosByUserId(userId);
		return systemInfos;
	}

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		String usergroupIdStr = (String)params.get("usergroupId");
		Long id = (Long)params.get("id");
		SystemInfoEntity se = new SystemInfoEntity();
		IPage<SystemInfoEntity> page = new Query<SystemInfoEntity>().getPage(params);
		if (!StringUtils.isEmpty(usergroupIdStr)) {
			se.setUsergroupId(Long.valueOf(usergroupIdStr));
		}
		if (id != null) {
			se.setId(id);
		}
		
		List<SystemInfoEntity> list = baseMapper.list(page, se);
		page.setRecords(list);
		return new PageUtils(page);
	}

	@Override
	public SystemInfoEntity queryDetail(Long id) {
		SystemInfoEntity detail = baseMapper.detail(id);
		return detail;
	}
	
	
}
