package com.nis.modules.sys.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.DateUtil;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.sys.dao.SysApiKeyDao;
import com.nis.modules.sys.entity.SysApiKey;
import com.nis.modules.sys.service.SysApiKeyService;
import com.nis.modules.sys.shiro.ShiroUtils;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;

@Service
public class SysApiKeyServiceImpl extends ServiceImpl<SysApiKeyDao,SysApiKey> implements SysApiKeyService{

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		IPage<SysApiKey> result = new Query<SysApiKey>().getPage(params);
		String ids = (String) params.get("ids");
        if(StrUtil.isNotBlank(ids)) {
        	List<String> listIds = Arrays.asList(ids.split(","));
        	params.put("ids",listIds);
        }
		List<SysApiKey> rows = this.baseMapper.queryList(result, params);
		result.setRecords(rows);
		return new PageUtils(result);
	}

	@Override
	public Integer addSysApiKey(SysApiKey sysApiKey) {
		// 校验名称不重复
		List<SysApiKey> sysApiKeys = this.list(new QueryWrapper<SysApiKey>().lambda().eq(SysApiKey::getName, sysApiKey.getName()));
		if(ToolUtil.isNotEmpty(sysApiKeys)) {
			throw new NZException(RCode.SYSAPIKEY_NAME_DUPLICATE);
		}
		
		// 生成唯一uuid
		String uuid = UUID.randomUUID().toString();
		
		sysApiKey.setToken(uuid);
		sysApiKey.setCreateBy(ShiroUtils.getUserId().intValue());
		sysApiKey.setCreateAt(DateUtil.getUTCTimeByConfigTimeZone());
		this.save(sysApiKey);
		return sysApiKey.getId();
	}

}
