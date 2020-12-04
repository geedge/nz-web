package com.nis.common.interceptor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.http.HttpStatus;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SysMenuInterceptor extends HandlerInterceptorAdapter {

    private SysConfigService sysConfigService;

    public SysMenuInterceptor() {
    }

    public SysMenuInterceptor(SysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        SysUserEntity user = ShiroUtils.getUserEntity();
        SysConfigEntity developDdmin = sysConfigService.getOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, "develop_admin"));
        if (!user.getUsername().equals(developDdmin.getParamValue())) {
            response.sendError(HttpStatus.SC_NOT_FOUND, "Not Found");
            return false;
        }
        return true;
    }
}