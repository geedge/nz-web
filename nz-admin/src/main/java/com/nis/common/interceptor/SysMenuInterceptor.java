package com.nis.common.interceptor;

import cn.hutool.core.util.StrUtil;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysConfService;
import com.nis.modules.sys.shiro.ShiroUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class SysMenuInterceptor implements HandlerInterceptor {

    private SysConfService sysConfService;


    public SysMenuInterceptor(SysConfService sysConfService) {
        this.sysConfService = sysConfService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // GET 请求不拦截,不进行校验
        if(StrUtil.equals(request.getMethod(),HttpMethod.GET.toString())) {
            return true;
        } else {
            SysUserEntity user = ShiroUtils.getUserEntity();
            String developDdminConf = sysConfService.getValue("develop_admin");
            if (!StrUtil.equals(user.getUsername(), developDdminConf)) {
                response.sendError(HttpStatus.SC_NOT_FOUND, "Not Found");
                return false;
            }
            return true;
        }
    }
}