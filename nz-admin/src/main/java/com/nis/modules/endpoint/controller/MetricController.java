package com.nis.modules.endpoint.controller;

import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.modules.endpoint.service.MetadataLabelService;
import com.nis.modules.endpoint.service.MetadataMetricService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping("/metric")
public class MetricController {

    @Autowired
    private MetadataMetricService metricService;
    @Autowired
    private MetadataLabelService labelService;

    @GetMapping("/")
    public R list(@RequestParam Map<String,Object> params){
        PageUtils page=metricService.queryPage(params);
        return R.ok();
    }

}
