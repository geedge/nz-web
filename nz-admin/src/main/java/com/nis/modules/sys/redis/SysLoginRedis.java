package com.nis.modules.sys.redis;


import com.alibaba.fastjson.JSON;
import com.nis.common.utils.RedisKeys;
import com.nis.modules.sys.entity.AuthTokenEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * authToken  Redis
 *

 */
@Component
public class SysLoginRedis {
	@Autowired
    private RedisTemplate redisTemplate;
    
    public void saveOrUpdate(AuthTokenEntity token,Long expire,String keyTmp) {
        if(token == null){
            return ;
        }
        String key = RedisKeys.getMfaKey(keyTmp);
        ValueOperations<String,String> ops = redisTemplate.opsForValue();
		ops.set(key,toJson(token),expire,TimeUnit.SECONDS);
    }

    public void delete(String keyTmp) {
    	String key = RedisKeys.getMfaKey(keyTmp);
    	redisTemplate.delete(key);
    }

    public AuthTokenEntity get(String keyTmp){
    	String key = RedisKeys.getMfaKey(keyTmp);
    	ValueOperations<String,String> ops = redisTemplate.opsForValue();
    	String value = ops.get(key);
    	return value == null ? null : fromJson(value, AuthTokenEntity.class);
    }
    
    public boolean hasKey(String keyTmp){
    	String key = RedisKeys.getMfaKey(keyTmp);
    	if(redisTemplate.hasKey(key)) {
    		return true;
    	}
    	return false;
    }
    
    /**
     * Object转成JSON数据
     */
    private String toJson(Object object){
        if(object instanceof Integer || object instanceof Long || object instanceof Float ||
                object instanceof Double || object instanceof Boolean || object instanceof String){
            return String.valueOf(object);
        }
        return JSON.toJSONString(object);
    }
    
    /**
     * JSON数据，转成Object
     */
    private <T> T fromJson(String json, Class<T> clazz){
        return JSON.parseObject(json, clazz);
    }
    	
}
