package com.nis.modules.sys.shiro;

import cn.hutool.log.Log;
import com.nis.common.utils.SpringContextUtils;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.sys.entity.SysUserEntity;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.DefaultSessionKey;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.*;

/**
 * Shiro工具类
 */
public class ShiroUtils {
	private static final Log log = Log.get();
	/**  加密算法 */
	public final static String hashAlgorithmName = "SHA-256";
	/**  循环次数 */
	public final static int hashIterations = 16;

	// redis 中 shiro key 前缀
	public final static String REDIS_KEYPREFIX = "shiro_redis_session:";

	public static String sha256(String pin, String salt) {
		return new SimpleHash(hashAlgorithmName, pin, salt, hashIterations).toString();
	}

	public static Session getSession() {
		return SecurityUtils.getSubject().getSession();
	}

	public static Session getSessionById(String sessionId) {
		return SecurityUtils.getSecurityManager().getSession(new DefaultSessionKey(sessionId));
	}
	
	public static SysUserEntity getUserEntityBySessionId(String sessionId){
		SysUserEntity result = null;
        try{
            Session se = SecurityUtils.getSecurityManager().getSession(new DefaultSessionKey(sessionId));
            Object obj = se.getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY);
            SimplePrincipalCollection coll = (SimplePrincipalCollection) obj;
            result = (SysUserEntity)coll.getPrimaryPrincipal();
        }catch (UnknownSessionException e){
            log.error("There is no session with id [{}]",sessionId);
        }
        return result;
    }
	
	public static Subject getSubject() {
		return SecurityUtils.getSubject();
	}

	public static SysUserEntity getUserEntity() {
		return (SysUserEntity)SecurityUtils.getSubject().getPrincipal();
	}

	public static Long getUserId() {
		return getUserEntity().getId();
	}
	
	public static void setSessionAttribute(Object key, Object value) {
		getSession().setAttribute(key, value);
	}

	public static Object getSessionAttribute(Object key) {
		return getSession().getAttribute(key);
	}

	public static boolean isLogin() {
		return SecurityUtils.getSubject().getPrincipal() != null;
	}

	public static void logout() {
		SecurityUtils.getSubject().logout();
	}
	
	/**
	 * 从 redis 中获取 token 和 用户信息
	 *
	 * @return
	 */
	public static Map<String, SysUserEntity> getShiroSessionInfoMap() {
		RedisConnectionFactory factory = SpringContextUtils.getBean("redisConnectionFactory", RedisConnectionFactory.class);
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		redisTemplate.setConnectionFactory(factory);

		redisTemplate.afterPropertiesSet();

		ValueOperations ops = redisTemplate.opsForValue();

		Set keys = redisTemplate.keys(ShiroUtils.REDIS_KEYPREFIX + "*");
		List<String> keyList = new ArrayList<>(keys);
		List<Object> values = ops.multiGet(keyList);
		Map<String, Object> map = new HashMap();

		int size = keyList.size();
		for (int i = 0; i < size; i++) {
			map.put(keyList.get(i), values.get(i));
		}

		Map<String, SysUserEntity> resultMap = new HashMap(map.size());
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			Object obj = entry.getValue();
			SimpleSession session = (SimpleSession) obj;
			if (ToolUtil.isEmpty(session.getAttributes()))
				continue;
			SimplePrincipalCollection primaryPrincipal = (SimplePrincipalCollection) session.getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY);
			if(ToolUtil.isNotEmpty(primaryPrincipal)) {
				resultMap.put(entry.getKey(), (SysUserEntity) primaryPrincipal.getPrimaryPrincipal());
			}
		}
		return resultMap;
	}
	
	public static void removeRedisCacheByToken(List<String> tokens) {
		RedisConnectionFactory factory = SpringContextUtils.getBean("redisConnectionFactory", RedisConnectionFactory.class);
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
		redisTemplate.setConnectionFactory(factory);

		redisTemplate.afterPropertiesSet();
		redisTemplate.delete(tokens);
	}
}
