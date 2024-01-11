package com.nis.common.utils;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.FIFOCache;
import cn.hutool.cache.impl.TimedCache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class LocalCacheManager {

    /**
     * 默认超时时间,2分钟
     */
    private static final Integer DEFAULT_TIMEOUT = 2 * 60;

    /**
     * 默认定时周期,每 1分钟清理过期数据
     */
    private static final Integer DEFAULT_SCHEDULE_PRUNE = 60;

    private static ConcurrentHashMap<String, Cache<String, Object>> CACHE_MAP = new ConcurrentHashMap<>();

    /**
     * TimedCache
     *
     * @param name
     * @return
     */
    public static TimedCache<String, Object> getTimedCache(String prefix, String name) {
        String key = Tool.StrUtil.concat(true, prefix, name);
        TimedCache<String, Object> cache = (TimedCache<String, Object>) CACHE_MAP.computeIfAbsent(key, s -> {
            TimedCache<String, Object> timedCache = CacheUtil.newTimedCache(TimeUnit.SECONDS.toMillis(DEFAULT_TIMEOUT));
            timedCache.schedulePrune(TimeUnit.SECONDS.toMillis(DEFAULT_SCHEDULE_PRUNE));
            return timedCache;
        });
        return cache;
    }

    /**
     * TimedCache,NeverExpire
     *
     * @param prefix
     * @param name
     * @return
     */
    public static TimedCache<String, Object> getNeverExpireTimedCache(String prefix, String name) {
        String key = Tool.StrUtil.concat(true, prefix, name);
        TimedCache<String, Object> cache = (TimedCache<String, Object>) CACHE_MAP.computeIfAbsent(key, s -> {
            TimedCache<String, Object> timedCache = CacheUtil.newTimedCache(0);
            timedCache.schedulePrune(TimeUnit.SECONDS.toMillis(DEFAULT_SCHEDULE_PRUNE));
            return timedCache;
        });
        return cache;
    }

    /**
     * FIFOCache
     *
     * @param prefix
     * @param name
     * @param capacity
     * @return
     */
    public static FIFOCache<String, Object> getFIFOCache(String prefix, String name, int capacity) {
        String key = Tool.StrUtil.concat(true, prefix, name);
        FIFOCache<String, Object> cache = (FIFOCache<String, Object>) CACHE_MAP.computeIfAbsent(key, s -> {
            FIFOCache<String, Object> fifoCache = Tool.CacheUtil.newFIFOCache(capacity);
            return fifoCache;
        });
        return cache;
    }

    /**
     * removeCache
     *
     * @param prefix
     * @param name
     */
    public static void removeCache(String prefix, String name) {
        String key = Tool.StrUtil.concat(true, prefix, name);
        Cache<String, Object> cache = CACHE_MAP.get(key);
        if (cache != null) {
            cache.clear();
            CACHE_MAP.remove(Tool.StrUtil.concat(true, prefix, name));
        }
    }

}
