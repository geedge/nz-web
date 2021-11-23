package com.nis.modules.sys.controller;

import com.nis.common.utils.R;
import com.nis.modules.sys.service.SysAppearanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/sys/appearance")
public class SysAppearanceController {

    @Autowired
    private SysAppearanceService sysAppearanceService;

    @GetMapping
    public R queryAppearance(){
        Map<String,String> result = sysAppearanceService.queryAppearance();
        return R.ok(result);
    }
}
