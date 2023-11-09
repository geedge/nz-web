package com.nis.modules.sys.shiro;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.SerializeUtil;
import cn.hutool.log.Log;
import com.nis.modules.sys.entity.SysLoginSession;
import com.nis.modules.sys.service.SysLoginSessionService;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 自定义 session 操作
 */
public class MySessionDao extends EnterpriseCacheSessionDAO {

    private Log log = Log.get();

    @Autowired
    private SysLoginSessionService sessionService;

    public MySessionDao() {
        super();
    }


    /**
     * create session
     * @param session
     * @return
     */
    @Override
    protected Serializable doCreate(Session session) {

        Serializable sessionId;
        //apikey 已存在 sessionId 无需生成
        if (ObjectUtil.isNotEmpty(session.getId())){
            sessionId = session.getId();
        }else {
            sessionId = this.generateSessionId(session);
            this.assignSessionId(session, sessionId);
        }

        SysLoginSession loginSession = SysLoginSession.builder()
                .sessionId(sessionId.toString())
                .sessionData(HexUtil.encodeHexStr(SerializeUtil.serialize(session)))
                .lasetActiveTs(System.currentTimeMillis())
                .build();
        sessionService.save(loginSession);
        return sessionId;
    }

    /**
     * read session
     * @param sessionId
     * @return
     */
    @Override
    protected Session doReadSession(Serializable sessionId) {

        SysLoginSession sysLoginSession = sessionService.getById(sessionId.toString());
        if (ObjectUtil.isNull(sysLoginSession)) {
            return null;
        }
        Session session = SerializeUtil.deserialize(HexUtil.decodeHex(sysLoginSession.getSessionData()));
        return session;

    }


    /**
     * update session
     * @param session
     */
    @Override
    protected void doUpdate(Session session) {
        SysLoginSession loginSession = SysLoginSession.builder()
                .sessionId(session.getId().toString())
                .sessionData(HexUtil.encodeHexStr(SerializeUtil.serialize(session)))
                .lasetActiveTs(System.currentTimeMillis())
                .build();
        sessionService.updateById(loginSession);
    }

    /**
     * delete session
     * @param session
     */
    @Override
    protected void doDelete(Session session) {
        sessionService.removeById(session.getId());
    }


    @Override
    public Collection<Session> getActiveSessions() {
        List<Session> sessions = ListUtil.list(false);
        List<SysLoginSession> loginSessionList = sessionService.list();
        for (SysLoginSession loginSession : loginSessionList) {
            Session session = SerializeUtil.deserialize(HexUtil.decodeHex(loginSession.getSessionData()));
            sessions.add(session);
        }
        return sessions;
    }
}
