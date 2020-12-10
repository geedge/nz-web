package com.nis.modules.sys.controller;

import com.nis.common.utils.R;
import com.nis.modules.sys.service.SysInfoService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 系统基本信息
 */

@RestController
public class SysInfoController {

    @Autowired
    private SysInfoService sysInfoService;

    @GetMapping("/healthy")
    public R healthy() {
        return R.ok();
    }

    @GetMapping("/about")
    @RequiresPermissions("sys:about:info")
    public R about() {
        Map aboutInfo = sysInfoService.getAboutInfo();
        return R.ok(aboutInfo);
    }
}
