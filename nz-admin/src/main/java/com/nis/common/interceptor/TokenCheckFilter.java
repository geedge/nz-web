package com.nis.common.interceptor;

import com.alibaba.fastjson.JSON;
import com.nis.common.utils.Constant;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.sys.dao.SysUserDao;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.web.filter.AccessControlFilter;
import org.springframework.data.redis.core.RedisTemplate;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

public class TokenCheckFilter extends AccessControlFilter {

    private Integer loginExpiration;
    private SysUserDao sysUserDao;
    private RedisTemplate<String, String> redisTemplate;

    public TokenCheckFilter(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public TokenCheckFilter(RedisTemplate redisTemplate, Integer loginExpiration) {
        this.redisTemplate = redisTemplate;
        this.loginExpiration = loginExpiration;
    }

    public TokenCheckFilter(RedisTemplate redisTemplate, Integer loginExpiration, SysUserDao sysUserDao) {
        this.redisTemplate = redisTemplate;
        this.loginExpiration = loginExpiration;
        this.sysUserDao = sysUserDao;
    }


//    @Override
//    protected boolean preHandle(ServletRequest req, ServletResponse resp) throws Exception {
//        HttpServletRequest request=(HttpServletRequest) req;
//        HttpServletResponse response=(HttpServletResponse)resp;
//        //跨域请求标志
//        String origin = request.getHeader("Origin");
//        if(StringUtils.isNotBlank(origin)){
//            response.setHeader("Access-Control-Allow-Origin",origin);
//            response.setHeader("Access-Control-Allow-Credentials","true");
//        }
//
//        return super.preHandle(request, response);
//    }

    @Override
    protected boolean isAccessAllowed(ServletRequest req, ServletResponse resp, Object o) throws Exception {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        String requestToken = request.getHeader(Constant.AUTH_TOKEN_CODE);
        if (StringUtils.isBlank(requestToken)) {
            requestToken = request.getParameter(Constant.AUTH_TOKEN_CODE);
        }
        if (StringUtils.isBlank(requestToken)) {
            response.getWriter().write(JSON.toJSON(R.error(RCode.SYS_LOGIN_REQUIRED)).toString());
            return false;
        }

        Boolean exist = redisTemplate.hasKey(ShiroUtils.REDIS_KEYPREFIX + requestToken);
        if (exist) {
            // 重新查询数据库
            SysUserEntity entity = this.sysUserDao.selectById(ShiroUtils.getUserEntity().getUserId());
            if (entity.getStatus() == 0) {
                response.getWriter().write(JSON.toJSON(R.error(RCode.SYS_LOGIN_LOCK)).toString());
                return false;
            }
            // key 存在 证明验证成功，刷新过期时间
            redisTemplate.expire(requestToken, this.loginExpiration, TimeUnit.MINUTES);
            return true;
        } else {
            response.getWriter().write(JSON.toJSON(R.error(RCode.SYS_LOGIN_REQUIRED)).toString());
            return false;
        }
    }

    @Override
    protected boolean onAccessDenied(ServletRequest req, ServletResponse resp) throws Exception {

        return false;
    }
}
