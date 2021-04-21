package com.nis.modules.sys.controller;

import com.nis.common.utils.R;
import com.nis.modules.sys.entity.Timezone;
import com.nis.modules.sys.service.TimezoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("sys/timezone")
public class TimezoneController {

    @Autowired
    private TimezoneService timezoneService;

    @GetMapping
    public R timezone(@RequestParam(required = false) String name){
        List<Timezone> list = timezoneService.queryTimezone(name);
        return R.ok().putData("list",list);
    }
}
