package com.nis.modules.asset.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.asset.dao.AssetFeatureDao;
import com.nis.modules.asset.entity.AssetFeature;
import com.nis.modules.asset.service.AssetFeatureService;
/**
 * 
 * @author Th
 *
 */
@Service("assetFeatureService")
public class AssetFeatureServiceImpl extends ServiceImpl<AssetFeatureDao,AssetFeature> implements AssetFeatureService{

}
