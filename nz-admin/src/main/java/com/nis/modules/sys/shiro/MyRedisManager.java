package com.nis.modules.sys.shiro;

import com.nis.common.utils.Tool;
import org.apache.shiro.session.Session;
import org.crazycake.shiro.IRedisManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 自定义 redis manager
 *
 * @author ThinkPad
 */
public class MyRedisManager implements IRedisManager {

    private RedisTemplate<String, Object> redisTemplate;

    public MyRedisManager(RedisConnectionFactory factory) {
        redisTemplate = new RedisTemplate<>();

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());

        redisTemplate.setConnectionFactory(factory);
        redisTemplate.afterPropertiesSet();
    }

    @Override
    public byte[] get(byte[] key) {
        return null;
    }

    @Override
    public byte[] set(byte[] key, byte[] value, int expire) {
        return null;
    }

    @Override
    public void del(byte[] key) {
    }

    @Override
    public Long dbSize(byte[] pattern) {
        return 16L;
    }

    @Override
    public Set<byte[]> keys(byte[] pattern) {
        return Tool.CollUtil.newHashSet();
    }

    public Object get(String key) {
        ValueOperations<String, Object> valueOperations = this.redisTemplate.opsForValue();
        return valueOperations.get(key);
    }

    public Session set(String key, Session value, int expire) {
        ValueOperations<String, Object> opsForValue = this.redisTemplate.opsForValue();
        opsForValue.set(key, value, expire, TimeUnit.SECONDS);
        return value;
    }

    public void del(String key) {
        this.redisTemplate.delete(key);
    }

    public Set<String> keys(String pattern) {
        return this.redisTemplate.keys(pattern);
    }
}
