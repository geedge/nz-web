package com.nis.common.interceptor;

import com.alibaba.fastjson.JSON;
import com.nis.common.utils.Constant;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.AccessControlFilter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class TokenCheckFilter extends AccessControlFilter {
    private RedisTemplate<String,String> redisTemplate;

    public TokenCheckFilter(RedisTemplate redisTemplate){
        this.redisTemplate=redisTemplate;
    }


    @Override
    protected boolean preHandle(ServletRequest req, ServletResponse resp) throws Exception {
        HttpServletRequest request=(HttpServletRequest) req;
        HttpServletResponse response=(HttpServletResponse)resp;
        //跨域请求标志
        String origin = request.getHeader("Origin");
        if(StringUtils.isNotBlank(origin)){
            response.setHeader("Access-Control-Allow-Origin",origin);
            response.setHeader("Access-Control-Allow-Credentials","true");
        }

        return super.preHandle(request, response);
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest req, ServletResponse resp, Object o) throws Exception {
        HttpServletRequest request=(HttpServletRequest) req;
        HttpServletResponse response=(HttpServletResponse)resp;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        String requestToken=request.getHeader(Constant.AUTH_TOKEN_CODE);
        if(StringUtils.isBlank(requestToken)){
            requestToken=request.getParameter(Constant.AUTH_TOKEN_CODE);
        }
        if(StringUtils.isBlank(requestToken)){
            response.getWriter().write(JSON.toJSON(R.error(RCode.SYS_LOGIN_REQUIRED)).toString());
            return false;
        }

        String value=ops.get(requestToken);

        if(StringUtils.isBlank(value)){
            response.getWriter().write(JSON.toJSON(R.error(RCode.SYS_LOGIN_REQUIRED)).toString());
            return false;
        }
        Subject subject = ShiroUtils.getSubject();
        String json= new String(Base64.getDecoder().decode(value),"UTF-8");
        Map<String,String> map=JSON.parseObject(json, HashMap.class);
        String username=map.get("username");
        String password=map.get("password");
        UsernamePasswordToken token = new UsernamePasswordToken(username, password);
        subject.login(token); //登录
        return true;
    }

    @Override
    protected boolean onAccessDenied(ServletRequest req, ServletResponse resp) throws Exception {

        return false;
    }
}
