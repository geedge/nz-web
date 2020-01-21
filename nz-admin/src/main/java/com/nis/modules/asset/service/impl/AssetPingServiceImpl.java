package com.nis.modules.asset.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.asset.dao.AssetPingDao;
import com.nis.modules.asset.entity.AssetPing;
import com.nis.modules.asset.service.AssetPingService;
import org.springframework.stereotype.Service;

/**
 * @author shizhendong
 * @date 2019-11-21
 */
@Service("assetPingService")
public class AssetPingServiceImpl extends ServiceImpl<AssetPingDao, AssetPing> implements AssetPingService {

}