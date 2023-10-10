package com.nis.common.interceptor;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.shiro.ShiroUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class SysMenuInterceptor implements HandlerInterceptor {

    private SysConfigService sysConfigService;


    public SysMenuInterceptor(SysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // GET 请求不拦截,不进行校验
        if(StrUtil.equals(request.getMethod(),HttpMethod.GET.toString())) {
            return true;
        } else {
            SysUserEntity user = ShiroUtils.getUserEntity();
            SysConfigEntity developDdmin = sysConfigService.getOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, "develop_admin"));
            if(!StrUtil.equals(user.getUsername(),developDdmin.getParamValue())) {
                response.sendError(HttpStatus.SC_NOT_FOUND, "Not Found");
                return false;
            }
            return true;
        }
    }
}