package com.nis.common.interceptor;

import com.alibaba.fastjson.JSON;
import com.nis.common.utils.Constant;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.web.filter.AccessControlFilter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TokenCheckFilter extends AccessControlFilter {
    private RedisTemplate<String,String> redisTemplate;

    public TokenCheckFilter(RedisTemplate redisTemplate){
        this.redisTemplate=redisTemplate;
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest req, ServletResponse resp, Object o) throws Exception {
        HttpServletRequest request=(HttpServletRequest) req;
        HttpServletResponse response=(HttpServletResponse)resp;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        String requestToken=request.getParameter(Constant.AUTH_TOKEN_CODE);
        if(StringUtils.isBlank(requestToken)){
            requestToken=request.getHeader(Constant.AUTH_TOKEN_CODE);
        }

        String value=ops.get(requestToken);

        if(StringUtils.isBlank(value)){
            response.getWriter().write(JSON.toJSON(R.error(RCode.SYS_LOGIN_REQUIRED)).toString());
            return false;
        }
        return true;
    }

    @Override
    protected boolean onAccessDenied(ServletRequest req, ServletResponse resp) throws Exception {

        return false;
    }
}
