package com.nis.modules.sys.controller;

import com.nis.common.utils.R;
import com.nis.modules.sys.service.SysService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/sys")
public class SysController {

    @Autowired
    private SysService sysService;

    @GetMapping("/appearance")
    public R queryAppearance(){
        Map<String,String> result = sysService.queryAppearance();
        return R.ok(result);
    }
}
