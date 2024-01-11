package com.nis.modules.sys.utils;


import cn.hutool.cache.impl.TimedCache;
import com.alibaba.fastjson.JSON;
import com.nis.common.utils.Constant;
import com.nis.common.utils.LocalCacheManager;
import com.nis.common.utils.Tool;
import com.nis.modules.sys.entity.AuthTokenEntity;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * authToken Cache
 */
@Component
public class MfaTokenManager {
    public void saveOrUpdate(AuthTokenEntity token, Long expire, String keyTmp) {
        if (token == null) {
            return;
        }
        String key = this.getMfaKey(keyTmp);
        TimedCache<String, Object> cache = LocalCacheManager.getTimedCache(Constant.SYS_CACHE_PREFIX, Constant.SYS_MFA_CACHE_KEY);
        cache.put(key, toJson(token), TimeUnit.SECONDS.toMillis(expire));
    }

    public void delete(String keyTmp) {
        String key = this.getMfaKey(keyTmp);
        TimedCache<String, Object> cache = LocalCacheManager.getTimedCache(Constant.SYS_CACHE_PREFIX, Constant.SYS_MFA_CACHE_KEY);
        cache.remove(key);
    }

    public AuthTokenEntity get(String keyTmp) {
        String key = this.getMfaKey(keyTmp);
        TimedCache<String, Object> cache = LocalCacheManager.getTimedCache(Constant.SYS_CACHE_PREFIX, Constant.SYS_MFA_CACHE_KEY);
        String value = Tool.StrUtil.toStringOrNull(cache.get(key, false));
        return value == null ? null : fromJson(value, AuthTokenEntity.class);
    }

    public boolean hasKey(String keyTmp) {
        String key = this.getMfaKey(keyTmp);
        TimedCache<String, Object> cache = LocalCacheManager.getTimedCache(Constant.SYS_CACHE_PREFIX, Constant.SYS_MFA_CACHE_KEY);
        if (cache.containsKey(key)) {
            return true;
        }
        return false;
    }

    /**
     * Object转成JSON数据
     */
    private String toJson(Object object) {
        if (object instanceof Integer || object instanceof Long || object instanceof Float ||
                object instanceof Double || object instanceof Boolean || object instanceof String) {
            return String.valueOf(object);
        }
        return JSON.toJSONString(object);
    }

    /**
     * JSON数据，转成Object
     */
    private <T> T fromJson(String json, Class<T> clazz) {
        return JSON.parseObject(json, clazz);
    }

    /**
     * get mfa key
     *
     * @param key
     * @return
     */
    private String getMfaKey(String key) {
        return "mfa:" + key;
    }
}
