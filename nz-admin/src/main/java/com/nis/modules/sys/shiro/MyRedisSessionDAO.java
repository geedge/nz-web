package com.nis.modules.sys.shiro;

import cn.hutool.log.Log;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.crazycake.shiro.IRedisManager;
import org.crazycake.shiro.RedisSessionDAO;
import org.crazycake.shiro.common.SessionInMemory;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.Serializable;
import java.util.*;

public class MyRedisSessionDAO extends RedisSessionDAO implements SessionDAO {

    private static final Log logger = Log.get();

    private static final String DEFAULT_SESSION_KEY_PREFIX = "shiro:session:";
    private String keyPrefix = "shiro:session:";
    private static final long DEFAULT_SESSION_IN_MEMORY_TIMEOUT = 1000L;
    private long sessionInMemoryTimeout = 1000L;
    private static final boolean DEFAULT_SESSION_IN_MEMORY_ENABLED = true;
    private boolean sessionInMemoryEnabled = true;
    private static ThreadLocal sessionsInThread = new ThreadLocal();
    private static final int DEFAULT_EXPIRE = -2;
    private static final int NO_EXPIRE = -1;
    private int expire = -2;
    private static final int MILLISECONDS_IN_A_SECOND = 1000;
    private MyRedisManager redisManager;

    public MyRedisSessionDAO() {
    }

    @Override
    public void update(Session session) throws UnknownSessionException {
        if (this.sessionInMemoryEnabled) {
            this.removeExpiredSessionInMemory();
        }

        this.saveSession(session);
        if (this.sessionInMemoryEnabled) {
            this.setSessionToThreadLocal(session.getId(), session);
        }

    }

    private void saveSession(Session session) throws UnknownSessionException {
        if (session != null && session.getId() != null) {
            String key;
            Session value;
            try {
                key = this.getRedisSessionKey(session.getId());
                value = session;
            } catch (SerializationException var5) {
                logger.error("serialize session error. session id=" + session.getId());
                throw new UnknownSessionException(var5);
            }

            if (this.expire == -2) {
                this.redisManager.set(key, value, (int) (session.getTimeout() / 1000L));
            } else {
                if (this.expire != -1 && (long) (this.expire * 1000) < session.getTimeout()) {
                    logger.warn("Redis session expire time: " + this.expire * 1000 + " is less than Session timeout: " + session.getTimeout() + " . It may cause some problems.");
                }

                this.redisManager.set(key, value, this.expire);
            }
        } else {
            logger.error("session or session id is null");
            throw new UnknownSessionException("session or session id is null");
        }
    }

    @Override
    public void delete(Session session) {
        if (this.sessionInMemoryEnabled) {
            this.removeExpiredSessionInMemory();
        }

        if (session != null && session.getId() != null) {
            if (this.sessionInMemoryEnabled) {
                this.delSessionFromThreadLocal(session.getId());
            }

            try {
                this.redisManager.del(this.getRedisSessionKey(session.getId()));
            } catch (SerializationException var3) {
                logger.error("delete session error. session id=" + session.getId());
            }

        } else {
            logger.error("session or session id is null");
        }
    }

    @Override
    public Collection<Session> getActiveSessions() {
        if (this.sessionInMemoryEnabled) {
            this.removeExpiredSessionInMemory();
        }

        HashSet sessions = new HashSet();

        try {
            Set<String> keys = this.redisManager.keys(this.keyPrefix + "*");
            if (keys != null && keys.size() > 0) {
                Iterator<String> var3 = keys.iterator();
                while (var3.hasNext()) {
                    String key = var3.next();
                    Session s = (Session) this.redisManager.get(key);
                    sessions.add(s);
                }
            }
        } catch (SerializationException var6) {
            logger.error("get active sessions error.");
        }

        return sessions;
    }

    @Override
    protected Serializable doCreate(Session session) {
        if (this.sessionInMemoryEnabled) {
            this.removeExpiredSessionInMemory();
        }

        if (session == null) {
            logger.error("session is null");
            throw new UnknownSessionException("session is null");
        } else {
            Serializable sessionId = this.generateSessionId(session);
            this.assignSessionId(session, sessionId);
            this.saveSession(session);
            return sessionId;
        }
    }

    @Override
    protected Session doReadSession(Serializable sessionId) {
        if (this.sessionInMemoryEnabled) {
            this.removeExpiredSessionInMemory();
        }

        if (sessionId == null) {
            logger.warn("session id is null");
            return null;
        } else {
            Session session;
            if (this.sessionInMemoryEnabled) {
                session = this.getSessionFromThreadLocal(sessionId);
                if (session != null) {
                    return session;
                }
            }

            session = null;

            try {
                String sessionRedisKey = this.getRedisSessionKey(sessionId);
                logger.debug("read session: " + sessionRedisKey + " from Redis");
                session = (Session) this.redisManager.get(sessionRedisKey);
                if (this.sessionInMemoryEnabled) {
                    this.setSessionToThreadLocal(sessionId, session);
                }
            } catch (SerializationException var4) {
                logger.error("read session error. sessionId: " + sessionId);
            }

            return session;
        }
    }

    private void setSessionToThreadLocal(Serializable sessionId, Session session) {
        this.initSessionsInThread();
        Map<Serializable, SessionInMemory> sessionMap = (Map) sessionsInThread.get();
        sessionMap.put(sessionId, this.createSessionInMemory(session));
    }

    private void delSessionFromThreadLocal(Serializable sessionId) {
        Map<Serializable, SessionInMemory> sessionMap = (Map) sessionsInThread.get();
        if (sessionMap != null) {
            sessionMap.remove(sessionId);
        }
    }

    private SessionInMemory createSessionInMemory(Session session) {
        SessionInMemory sessionInMemory = new SessionInMemory();
        sessionInMemory.setCreateTime(new Date());
        sessionInMemory.setSession(session);
        return sessionInMemory;
    }

    private void initSessionsInThread() {
        Map<Serializable, SessionInMemory> sessionMap = (Map) sessionsInThread.get();
        if (sessionMap == null) {
            sessionMap = new HashMap();
            sessionsInThread.set(sessionMap);
        }

    }

    private void removeExpiredSessionInMemory() {
        Map<Serializable, SessionInMemory> sessionMap = (Map) sessionsInThread.get();
        if (sessionMap != null) {
            Iterator it = sessionMap.keySet().iterator();

            while (it.hasNext()) {
                Serializable sessionId = (Serializable) it.next();
                SessionInMemory sessionInMemory = (SessionInMemory) sessionMap.get(sessionId);
                if (sessionInMemory == null) {
                    it.remove();
                } else {
                    long liveTime = this.getSessionInMemoryLiveTime(sessionInMemory);
                    if (liveTime > this.sessionInMemoryTimeout) {
                        it.remove();
                    }
                }
            }

            if (sessionMap.size() == 0) {
                sessionsInThread.remove();
            }

        }
    }

    private Session getSessionFromThreadLocal(Serializable sessionId) {
        if (sessionsInThread.get() == null) {
            return null;
        } else {
            Map<Serializable, SessionInMemory> sessionMap = (Map) sessionsInThread.get();
            SessionInMemory sessionInMemory = (SessionInMemory) sessionMap.get(sessionId);
            if (sessionInMemory == null) {
                return null;
            } else {
                logger.debug("read session from memory");
                return sessionInMemory.getSession();
            }
        }
    }

    private long getSessionInMemoryLiveTime(SessionInMemory sessionInMemory) {
        Date now = new Date();
        return now.getTime() - sessionInMemory.getCreateTime().getTime();
    }

    private String getRedisSessionKey(Serializable sessionId) {
        return this.keyPrefix + sessionId;
    }

    @Override
    public IRedisManager getRedisManager() {
        return this.redisManager;
    }

    public void setRedisManager(MyRedisManager redisManager) {
        this.redisManager = redisManager;
    }

    @Override
    public String getKeyPrefix() {
        return this.keyPrefix;
    }

    @Override
    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }


    @Override
    public long getSessionInMemoryTimeout() {
        return this.sessionInMemoryTimeout;
    }

    @Override
    public void setSessionInMemoryTimeout(long sessionInMemoryTimeout) {
        this.sessionInMemoryTimeout = sessionInMemoryTimeout;
    }

    @Override
    public int getExpire() {
        return this.expire;
    }

    @Override
    public void setExpire(int expire) {
        this.expire = expire;
    }

    @Override
    public boolean getSessionInMemoryEnabled() {
        return this.sessionInMemoryEnabled;
    }

    @Override
    public void setSessionInMemoryEnabled(boolean sessionInMemoryEnabled) {
        this.sessionInMemoryEnabled = sessionInMemoryEnabled;
    }

    public static ThreadLocal getSessionsInThread() {
        return sessionsInThread;
    }
}
