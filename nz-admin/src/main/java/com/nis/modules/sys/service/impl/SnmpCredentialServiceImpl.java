package com.nis.modules.sys.service.impl;

import java.util.Arrays;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.asset.entity.AssetFieldGroup;
import com.nis.modules.asset.entity.AssetFieldMeta;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.sys.dao.SnmpCredentialDao;
import com.nis.modules.sys.entity.SnmpCredential;
import com.nis.modules.sys.service.SnmpCredentialService;

import cn.hutool.core.util.ObjectUtil;

@Service
public class SnmpCredentialServiceImpl extends ServiceImpl<SnmpCredentialDao,SnmpCredential> implements SnmpCredentialService{

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		String ids = (String) params.get("ids");
		String name = (String) params.get("name");
		String types = (String) params.get("types");
		
		IPage<SnmpCredential> result = this.page(new Query<SnmpCredential>().getPage(params), new QueryWrapper<SnmpCredential>().lambda()
				.in(ToolUtil.isNotEmpty(ids), SnmpCredential::getId, ToolUtil.isNotEmpty(ids) ? Arrays.asList(ids.toString().split(",")) : null)
				.like(ToolUtil.isNotEmpty(name), SnmpCredential::getName, name)
				.in(ToolUtil.isNotEmpty(types), SnmpCredential::getType, ToolUtil.isNotEmpty(types) ? Arrays.asList(types.toString().split(",")) : null));
		
		return new PageUtils(result);
	}

}
