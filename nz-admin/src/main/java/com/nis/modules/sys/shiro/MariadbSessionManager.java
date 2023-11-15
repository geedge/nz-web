package com.nis.modules.sys.shiro;

import com.nis.common.utils.Constant;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.shiro.web.servlet.ShiroHttpServletRequest;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.util.StringUtils;

import java.io.Serializable;

public class MariadbSessionManager extends DefaultWebSessionManager {

    private static final String REFERENCED_SESSION_ID_SOURCE = "Stateless request";

    public MariadbSessionManager() {
        super();
    }

    @Override
    protected Serializable getSessionId(ServletRequest request, ServletResponse response) {
        HttpServletRequest req = (HttpServletRequest) request;
        String token = req.getHeader(Constant.AUTH_TOKEN_CODE);
        if (StringUtils.isEmpty(token)) {
            token = request.getParameter(Constant.AUTH_TOKEN_CODE);
        }
        // 如果请求头中有 token 则其值为 sessionId
        if (!StringUtils.isEmpty(token)) {
            request.setAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID_SOURCE, REFERENCED_SESSION_ID_SOURCE);
            request.setAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID, token);
            request.setAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID_IS_VALID, Boolean.TRUE);
            return token;
        } else {
            // 否则按默认规则从cookie取sessionId
            return super.getSessionId(request, response);
        }
    }
}