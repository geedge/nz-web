package com.nis.modules.system.service.impl;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.system.dao.SystemDao;
import com.nis.modules.system.entity.SystemEntity;
import com.nis.modules.system.service.SystemService;

@Service("systemService")
public class SystemServiceImpl extends ServiceImpl<SystemDao, SystemEntity> implements SystemService {

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		String id = (String)params.get("id");
		String name = (String)params.get("name");
		String remark = (String)params.get("remark");
		
		IPage<SystemEntity> page = this.page(
			new Query<SystemEntity>().getPage(params),
			new QueryWrapper<SystemEntity>()
				.eq(StringUtils.isNotBlank(id),"id", id)
				.like(StringUtils.isNotBlank(name),"name", name)
				.like(StringUtils.isNotBlank(remark), "remark",remark)
		);
		return new PageUtils(page);
	}

}
