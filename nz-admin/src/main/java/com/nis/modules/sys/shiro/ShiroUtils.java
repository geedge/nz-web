package com.nis.modules.sys.shiro;

import cn.hutool.log.Log;
import com.nis.common.utils.SpringContextUtils;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.sys.dao.SysLoginSessionDao;
import com.nis.modules.sys.entity.SysUserEntity;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.DefaultSessionKey;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.DefaultSubjectContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Shiro工具类
 */
public class ShiroUtils {

	private static final Log log = Log.get();

	/**  加密算法 */
	public final static String hashAlgorithmName = "SHA-256";

	/**  循环次数 */
	public final static int hashIterations = 16;

	public static String sha256(String pin, String salt) {
		return new SimpleHash(hashAlgorithmName, pin, salt, hashIterations).toString();
	}

	public static Session getSession() {
		return SecurityUtils.getSubject().getSession();
	}

	public static Session getSessionById(String sessionId) {
		try {
			return SecurityUtils.getSecurityManager().getSession(new DefaultSessionKey(sessionId));
		} catch (Exception e) {
			return null;
		}
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
	
	
	public static void save(Session session) {
		SessionDAO sessionDao = SpringContextUtils.getBean("sessionDAO", SessionDAO.class);
		sessionDao.create(session);
}
	
	
	public static List<SysUserEntity> getActiveUsers() {
		SessionDAO sessionDao = SpringContextUtils.getBean("sessionDAO", SessionDAO.class);
		Collection<Session> sessions = sessionDao.getActiveSessions();
		List<SysUserEntity> userList = Tool.CollUtil.newArrayList();
		for (Session session : sessions) {
			PrincipalCollection primaryPrincipal = (PrincipalCollection) session.getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY);
			if(ToolUtil.isNotEmpty(primaryPrincipal)) {
				userList.add((SysUserEntity) primaryPrincipal.getPrimaryPrincipal());
			}
		}
		return userList;
	}


	/**
	 * getActiveSessions
	 *
	 * @return
	 */
	public static Map<String, SysUserEntity> getShiroSessionInfoMap() {
		SessionDAO sessionDao = SpringContextUtils.getBean("sessionDAO", SessionDAO.class);
		Collection<Session> sessions = sessionDao.getActiveSessions();
		Map<String, SysUserEntity> resultMap = Tool.MapUtil.newHashMap(sessions.size());
		for (Session session : sessions) {
			PrincipalCollection primaryPrincipal = (PrincipalCollection) session.getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY);
			if(ToolUtil.isNotEmpty(primaryPrincipal)) {
				resultMap.put(session.getId().toString(), (SysUserEntity) primaryPrincipal.getPrimaryPrincipal());
			}
		}
		return resultMap;
	}

	public static void deleteSession(List<String> sessionIds) {
		SysLoginSessionDao sysLoginSessionDao = SpringContextUtils.getBean("sysLoginSessionDao", SysLoginSessionDao.class);
		for (String sessionId : sessionIds) {
			sysLoginSessionDao.deleteById(sessionId);
		}
	}
}
