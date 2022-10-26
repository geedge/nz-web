package com.nis.modules.sys.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.sys.dao.SysApiKeyDao;
import com.nis.modules.sys.entity.SysApiKey;
import com.nis.modules.sys.entity.SysUserRoleEntity;
import com.nis.modules.sys.service.SysApiKeyService;
import com.nis.modules.sys.service.SysUserRoleService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SysApiKeyServiceImpl extends ServiceImpl<SysApiKeyDao,SysApiKey> implements SysApiKeyService{

	@Autowired
	private SysUserRoleService sysUserRoleService;

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		IPage result = new Query(SysApiKey.class).getPage(params);
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
		sysApiKey.setName(StrUtil.trim(sysApiKey.getName()));
		List<SysApiKey> sysApiKeys = this.list(new QueryWrapper<SysApiKey>().lambda().eq(SysApiKey::getName, sysApiKey.getName()));
		if(ToolUtil.isNotEmpty(sysApiKeys)) {
			throw new NZException(RCode.SYSAPIKEY_NAME_DUPLICATE);
		}

		// 生成唯一uuid
		String uuid = UUID.randomUUID().toString();

		sysApiKey.setToken(uuid);
		sysApiKey.setCreateBy(ShiroUtils.getUserId().intValue());
		sysApiKey.setCreateAt(new Date());
		this.save(sysApiKey);
		return sysApiKey.getId();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeSysApiKeys(String ids) {
		List<SysApiKey> sysApiKeys = this.list(new QueryWrapper<SysApiKey>().lambda().in(SysApiKey::getId, Arrays.asList(ids.split(","))));
		List<String> tokens = new ArrayList<String>();
		for(SysApiKey sysApiKey : sysApiKeys) {
			tokens.add(StrUtil.str(ShiroUtils.REDIS_KEYPREFIX+sysApiKey.getToken()));
		}
		ShiroUtils.removeRedisCacheByToken(tokens);
		this.removeByIds(Arrays.asList(ids.split(",")));
	}

	@Override
	public PageUtils querySelfApiKeys(Map<String, Object> params) {
		String ids = (String) params.get("ids");
		if (StrUtil.isNotBlank(ids)) {
			List<String> listIds = Arrays.asList(ids.split(","));
			params.put("ids", listIds);
		}

		// 当前登录用户
		params.put("currentLoginUserID", ShiroUtils.getUserId().intValue());

		IPage result = new Query(SysApiKey.class).getPage(params);
		List<SysApiKey> rows = this.baseMapper.querySelfApiKeys(result, params);
		result.setRecords(rows);
		return new PageUtils(result);
	}

	@Override
	public Map<String, Object> addSelfApiKey(SysApiKey sysApiKey) {
		// 名称唯一
		sysApiKey.setName(StrUtil.trim(sysApiKey.getName()));
		List<SysApiKey> sysApiKeys = this.list(new LambdaQueryWrapper<SysApiKey>().eq(SysApiKey::getName, sysApiKey.getName()));
		if (ToolUtil.isNotEmpty(sysApiKeys)) {
			throw new NZException(RCode.SYSAPIKEY_NAME_DUPLICATE);
		}

		// token 唯一
		sysApiKey.setToken(UUID.randomUUID().toString());

		// 根据当前用户 角色 补充 roleId
		SysUserRoleEntity userRoleEntity = sysUserRoleService.getOne(new LambdaQueryWrapper<SysUserRoleEntity>().eq(SysUserRoleEntity::getUserId, ShiroUtils.getUserId().intValue()));
		sysApiKey.setRoleId(userRoleEntity.getRoleId());

		sysApiKey.setCreateBy(ShiroUtils.getUserId().intValue());
		sysApiKey.setCreateAt(new Date());
		this.save(sysApiKey);

		Map<String, Object> resultMap = Tool.MapUtil.newHashMap(4);
		resultMap.put("id", sysApiKey.getId());
		resultMap.put("token", sysApiKey.getToken());
		return resultMap;
	}

	@Override
	public void removeSelfApiKeys(String ids) {
		// 查询当前用户创建的 API KEYS
		List<SysApiKey> sysApiKeys = this.list(
				new LambdaQueryWrapper<SysApiKey>().in(SysApiKey::getId, Arrays.asList(ids.split(",")))
						.eq(SysApiKey::getCreateBy, ShiroUtils.getUserId().intValue())
		);
		// 删除 redis 中 api key
		List<String> tokens = sysApiKeys.stream().map(k -> ShiroUtils.REDIS_KEYPREFIX + k.getToken()).collect(Collectors.toList());
		ShiroUtils.removeRedisCacheByToken(tokens);

		List<Integer> removeIds = sysApiKeys.stream().map(SysApiKey::getId).collect(Collectors.toList());
		this.removeByIds(removeIds);
	}

	@Override
    @Transactional(rollbackFor = Exception.class)
	public void updateRoleIdByUser(Integer userId, Integer roleId) {
		this.getBaseMapper().updateRoleIdByCreateBy(userId, roleId);
	}

}
