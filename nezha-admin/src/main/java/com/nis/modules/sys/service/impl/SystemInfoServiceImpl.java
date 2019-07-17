package com.nis.modules.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.sys.dao.SystemInfoDao;
import com.nis.modules.sys.entity.SystemInfoEntity;
import com.nis.modules.sys.service.SystemInfoService;

import java.util.List;

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
}
