package com.nis.modules.sys.service.impl;

import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.sys.dao.SysComponentDao;
import com.nis.modules.sys.entity.SysComponent;
import com.nis.modules.sys.service.SysComponentService;

@Service
public class SysComponentServiceImpl extends ServiceImpl<SysComponentDao, SysComponent> implements SysComponentService {

	@Autowired
	private SysComponentDao componentDao;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateComponent(SysComponent component) {
		// 先删除 后添加
 		componentDao.deleteById(component.getServerid());
		componentDao.insert(component);
	}

	@Override
	public SysComponent findByUrl(String alertApi) {
		if (Tool.StrUtil.isBlank(alertApi) || !Pattern.matches("http[s]?://.+:[\\d]+", alertApi)) {
			return null;
		}
		String[] split = alertApi.split("://");
		String protocol = split[0];
		split = split[1].split(":");
		String host = split[0];
		Integer port = Integer.valueOf(split[1]);
		return this.getOne(Wrappers.lambdaQuery(new SysComponent())
				.eq(SysComponent::getComponent, Constant.NZ_WEB_COMPONENT_NAME).eq(SysComponent::getHost, host)
				.eq(SysComponent::getPort, port).eq(SysComponent::getProtocol, protocol));
	}
	
}
