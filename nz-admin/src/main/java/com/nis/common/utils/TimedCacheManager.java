package com.nis.common.utils;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TimedCacheManager {

    /**
     * 默认超时时间,2分钟
     */
    private static final Integer DEFAULT_TIMEOUT = 2 * 60;

    /**
     * 默认定时周期,每 1分钟清理过期数据
     */
    private static final Integer DEFAULT_SCHEDULE_PRUNE = 60;


    private static ConcurrentHashMap<String, TimedCache<String, Object>> CACHE_MAP = new ConcurrentHashMap<>();

    /**
     * getCache
     *
     * @param name
     * @return
     */
    public static TimedCache<String, Object> getCache(String prefix, String name) {
        TimedCache<String, Object> cache = CACHE_MAP.computeIfAbsent(Tool.StrUtil.concat(true, prefix, name), s -> {
            TimedCache<String, Object> timedCache = CacheUtil.newTimedCache(TimeUnit.SECONDS.toMillis(DEFAULT_TIMEOUT));
            timedCache.schedulePrune(TimeUnit.SECONDS.toMillis(DEFAULT_SCHEDULE_PRUNE));
            return timedCache;
        });
        return cache;
    }

    /**
     * getCache
     *
     * @param name
     * @param timeout
     * @param delay
     * @return
     */
    public static TimedCache<String, Object> getCache(String prefix, String name, Integer timeout, Integer delay) {
        TimedCache<String, Object> cache = CACHE_MAP.computeIfAbsent(Tool.StrUtil.concat(true, prefix, name), s -> {
            TimedCache<String, Object> timedCache = CacheUtil.newTimedCache(TimeUnit.SECONDS.toMillis(timeout));
            if (Tool.ObjectUtil.notEqual(0, timeout)) {
                timedCache.schedulePrune(TimeUnit.SECONDS.toMillis(delay));
            }
            return timedCache;
        });
        return cache;
    }

    /**
     * get never expire cache
     *
     * @param prefix
     * @param name
     * @return
     */
    public static TimedCache<String, Object> getNeverExpireCache(String prefix, String name) {
        TimedCache<String, Object> cache = CACHE_MAP.computeIfAbsent(Tool.StrUtil.concat(true, prefix, name), s -> {
            TimedCache<String, Object> timedCache = CacheUtil.newTimedCache(0);
            return timedCache;
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
        TimedCache<String, Object> cache = getCache(prefix, name);
        cache.clear();
        CACHE_MAP.remove(Tool.StrUtil.concat(true, prefix, name));
    }

}
