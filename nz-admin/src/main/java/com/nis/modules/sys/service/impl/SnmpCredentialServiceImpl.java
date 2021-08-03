package com.nis.modules.sys.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.common.utils.SnmpUtil;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetFieldGroup;
import com.nis.modules.asset.entity.AssetFieldMeta;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.sys.dao.SnmpCredentialDao;
import com.nis.modules.sys.entity.SnmpCredential;
import com.nis.modules.sys.service.SnmpCredentialService;

import cn.hutool.core.util.ObjectUtil;

@Service
public class SnmpCredentialServiceImpl extends ServiceImpl<SnmpCredentialDao,SnmpCredential> implements SnmpCredentialService{

	@Autowired
	private AssetAssetService assetAssetService;
	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		String ids = (String) params.get("ids");
		String name = (String) params.get("name");
		String types = (String) params.get("types");
		
		IPage result = this.page(new Query(SnmpCredential.class).getPage(params), new QueryWrapper<SnmpCredential>().lambda()
				.in(ToolUtil.isNotEmpty(ids), SnmpCredential::getId, ToolUtil.isNotEmpty(ids) ? Arrays.asList(ids.toString().split(",")) : null)
				.like(ToolUtil.isNotEmpty(name), SnmpCredential::getName, name)
				.in(ToolUtil.isNotEmpty(types), SnmpCredential::getType, ToolUtil.isNotEmpty(types) ? Arrays.asList(types.toString().split(",")) : null));
		
		return new PageUtils(result);
	}

	@Override
	public Integer saveSnmpCredential(SnmpCredential snmpCredential) {
		this.validateSnmpCredential(snmpCredential);
		this.save(snmpCredential);
		return snmpCredential.getId();
	}

	@Override
	public Integer modifySnmpCredential(SnmpCredential snmpCredential) {
		ValidateUtils.is(snmpCredential.getId()).notNull(RCode.SNMP_CREDENTIAL_ID_ISNULL);
		this.validateSnmpCredential(snmpCredential);
		this.updateById(snmpCredential);
		return snmpCredential.getId();
	}

	@Override
	public void validateSnmpCredential(SnmpCredential snmpCredential) {
		ValidateUtils.is(snmpCredential.getName()).notNull(RCode.SNMP_CREDENTIAL_NAME_ISNULL).maxLength(64)
		.and(snmpCredential.getType()).notNull(RCode.SNMP_CREDENTIAL_TYPE_ISNULL)
		.and(snmpCredential.getConfig()).notNull(RCode.SNMP_CREDENTIAL_CONFIG_ISNULL).maxLength(1024)
		.and(snmpCredential.getPort()).notNull(RCode.SNMP_CREDENTIAL_PORT_ISNULL)
		.and(snmpCredential.getRemark()).maxLength(256);
		// 当snmp为v3版本时进行参数校验
		if(ToolUtil.equals(snmpCredential.getType(), 3)) {
			SnmpUtil.ValidateAuthForSnmpV3((Map)JSON.parseObject(snmpCredential.getConfig()));
		}
		// 验证name不重复
		List<SnmpCredential> datas = this.list(new QueryWrapper<SnmpCredential>().lambda().eq(SnmpCredential::getName, snmpCredential.getName()).ne(ObjectUtil.isNotEmpty(snmpCredential.getId()),SnmpCredential::getId, snmpCredential.getId()));
		if(ToolUtil.isNotEmpty(datas)) {
			throw new NZException(RCode.SNMP_CREDENTIAL_NAME_DUPLICATE);
		}
	}

	@Override
	public void removeSnmpCredential(List<String> ids) {
		// 是否关联资产如果关联不允许删除
		List<AssetAsset> assets = assetAssetService.list(new QueryWrapper<AssetAsset>().lambda().in(AssetAsset::getSnmpCredentialId, ids));
		if(ToolUtil.isNotEmpty(assets)) {
			throw new NZException(RCode.SNMP_CREDENTIAL_ASSETS_EXISTS);
		}
		this.removeByIds(ids);
	}

}
