package com.nis.modules.asset.service.impl;



import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.asset.dao.AssetAccountRelDao;
import com.nis.modules.asset.entity.AssetAccountRel;
import com.nis.modules.asset.service.AssetAccountRelService;

import cn.hutool.log.Log;

@Service("assetAccountRelService")
public class AssetAccountRelServiceImpl extends ServiceImpl<AssetAccountRelDao,AssetAccountRel> implements AssetAccountRelService{

	private final static Log logger = Log.get();
	
	
}
