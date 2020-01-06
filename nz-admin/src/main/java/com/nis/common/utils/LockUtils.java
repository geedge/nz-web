package com.nis.common.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * redis 分布式锁
 */
@Component
public class LockUtils {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private DefaultRedisScript<Long> redisScript;

    // 超时时间 50分钟
    private final Long TIME_OUT = 50L;

    private final Long RELEASE_SUCCESS = 1L;


    /**
     * 加锁
     *
     * @param key
     * @return
     */
    public boolean lock(String key, String value) {
        // 加锁并设置过期时间
        return stringRedisTemplate.opsForValue().setIfAbsent(key, value, TIME_OUT, TimeUnit.MINUTES);
    }

    /**
     * 释放锁
     *
     * @param key
     * @return
     */
    public boolean unlock(String key, String value) {
        // 使用 lua 脚本：先判断是否是自己设置的锁，再执行删除
        Long result = (Long) stringRedisTemplate.execute(redisScript, Arrays.asList(key, value));
        return RELEASE_SUCCESS.equals(result);
    }


    /**
     * 注入redis script 在上面释放锁的时候使用
     *
     * @return
     */
    @Bean
    public DefaultRedisScript<Long> defaultRedisScript() {
        DefaultRedisScript<Long> defaultRedisScript = new DefaultRedisScript<>();
        defaultRedisScript.setResultType(Long.class);
        // 使用脚本的方式执行 两步合成一步 保证了操作原子性
        defaultRedisScript.setScriptText("if redis.call('get', KEYS[1]) == KEYS[2] then return redis.call('del', KEYS[1]) else return 0 end");
        return defaultRedisScript;
    }


}
