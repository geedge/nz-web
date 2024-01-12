package com.nis.modules.asset.service.impl;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Constant;
import com.nis.common.utils.JSONUtil;
import com.nis.common.utils.LocalCacheManager;
import com.nis.modules.asset.dao.AssetPingDao;
import com.nis.modules.asset.entity.AssetPing;
import com.nis.modules.asset.service.AssetPingService;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author shizhendong
 * @date 2019-11-21
 */
@Service("assetPingService")
public class AssetPingServiceImpl extends ServiceImpl<AssetPingDao, AssetPing> implements AssetPingService {

    private static final Log log = Log.get();

    @Override
    public void syncAssetPingStateCache(String assetPingJsonStr) {
        try {
            // remove
            LocalCacheManager.removeCache(Constant.SYS_STATE_CACHE_PREFIX, Constant.SYSCONFIG_KEY_ASSET_PING);
            // add
            TimedCache<String, Object> cache = LocalCacheManager.getTimedCache(Constant.SYS_STATE_CACHE_PREFIX, Constant.SYSCONFIG_KEY_ASSET_PING);
            Map<String, String> dataMap = JSONUtil.toBean(assetPingJsonStr, Map.class);
            for (Map.Entry<String, String> entry : dataMap.entrySet()) {
                cache.put(entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            log.error(e, "[syncAssetPingCache] [error]");
        }
    }

}