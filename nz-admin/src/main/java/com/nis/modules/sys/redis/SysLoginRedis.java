package com.nis.modules.sys.redis;


import com.nis.common.utils.RedisKeys;
import com.nis.common.utils.RedisUtils;
import com.nis.modules.sys.entity.AuthTokenEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * authToken  Redis
 *

 */
@Component
public class SysLoginRedis {
    @Autowired
    private RedisUtils redisUtils;
    
    public void saveOrUpdate(AuthTokenEntity token,Long expire,String keyTmp) {
        if(token == null){
            return ;
        }
        String key = RedisKeys.getMfaKey(keyTmp);
        redisUtils.set(key, token, expire);
    }

    public void delete(String keyTmp) {
    	String key = RedisKeys.getMfaKey(keyTmp);
        redisUtils.delete(key);
    }

    public AuthTokenEntity get(String keyTmp){
    	String key = RedisKeys.getMfaKey(keyTmp);
        return redisUtils.get(key, AuthTokenEntity.class);
    }
}
