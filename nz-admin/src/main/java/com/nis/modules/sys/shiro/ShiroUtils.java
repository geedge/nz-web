package com.nis.modules.sys.shiro;

import com.nis.common.exception.NZException;
import com.nis.common.utils.SpringContextUtils;
import com.nis.modules.sys.entity.SysUserEntity;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;

import java.util.*;

/**
 * Shiro工具类
 */
public class ShiroUtils {
	/**  加密算法 */
	public final static String hashAlgorithmName = "SHA-256";
	/**  循环次数 */
	public final static int hashIterations = 16;

	// redis 中 shiro key 前缀
	public final static String REDIS_KEYPREFIX = "shiro_redis_session:";

	public static String sha256(String password, String salt) {
		return new SimpleHash(hashAlgorithmName, password, salt, hashIterations).toString();
	}

	public static Session getSession() {
		return SecurityUtils.getSubject().getSession();
	}

	public static Subject getSubject() {
		return SecurityUtils.getSubject();
	}

	public static SysUserEntity getUserEntity() {
		return (SysUserEntity)SecurityUtils.getSubject().getPrincipal();
	}

	public static Long getUserId() {
		return getUserEntity().getUserId();
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
	
	public static String getKaptcha(String key) {
		Object kaptcha = getSessionAttribute(key);
		if(kaptcha == null){
			throw new NZException("验证码已失效");
		}
		getSession().removeAttribute(key);
		return kaptcha.toString();
	}


	/**
	 * 从 redis 中获取 token 和 用户信息
	 *
	 * @return
	 */
	public static Map<String, SysUserEntity> getShiroSessionInfoMap() {
		RedisTemplate redisTemplate = (RedisTemplate) SpringContextUtils.getBean("redisTemplate");

		redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
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
			SimplePrincipalCollection primaryPrincipal = (SimplePrincipalCollection) session.getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY);
			resultMap.put(entry.getKey(), (SysUserEntity) primaryPrincipal.getPrimaryPrincipal());
		}
		return resultMap;
	}
}
