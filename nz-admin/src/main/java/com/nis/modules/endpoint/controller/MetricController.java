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
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/metric")
public class MetricController {

    @Autowired
    private MetadataMetricService metricService;

    @Autowired
    private MetadataLabelService labelService;

    @GetMapping
    @ResponseBody
    public R list(@RequestParam Map<String,Object> params){
        PageUtils page=metricService.queryPage(params);
        return R.ok(page);
    }

    @GetMapping("/labelName")
    @ResponseBody
    public R queryLabelNames(@RequestParam(value = "metric")String metric,@RequestParam(value = "name",required = false) String name){

        List<Map<String,String>> list=labelService.queryLabels(metric,name);

        return R.ok(list);
    }

    @GetMapping("/labelVal")
    @ResponseBody
    public R queryLabelValues(@RequestParam(value = "metric")String metric,@RequestParam(value = "name") String name){

        List<Map<String,String>> list=labelService.queryLabels(metric,name);

        return R.ok(list);
    }

}
