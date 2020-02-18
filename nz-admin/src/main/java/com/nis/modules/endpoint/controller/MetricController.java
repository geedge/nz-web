package com.nis.modules.endpoint.controller;

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.endpoint.entity.MetadataMetric;
import com.nis.modules.endpoint.service.MetadataLabelService;
import com.nis.modules.endpoint.service.MetadataMetricService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
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

        List<Map<String,String>> list=labelService.queryLabels(metric,name,"name");
        Map<String, List<Map<String, String>>> map = new HashMap<>();
        map.put("list",list);
        return R.ok(map);
    }

    @GetMapping("/labelVal")
    @ResponseBody
    public R queryLabelValues(@RequestParam(value = "metric")String metric,@RequestParam(value = "name") String name){

        List<Map<String,String>> list=labelService.queryLabels(metric,name,"value");
        Map<String, List<Map<String, String>>> map = new HashMap<>();
        map.put("list",list);
        return R.ok(map);
    }

    /**
     * 元数据查询
     *
     * @param endpointId
     * @return
     */
    @GetMapping("/metadata")
    @ResponseBody
    public R queryMetadata(Integer endpointId) {
        ValidateUtils.is(endpointId).notNull(RCode.ENDPOINT_ID_ISNULL);

        List<MetadataMetric> metadata = metricService.metadata(endpointId);
        Map data = new HashMap();
        data.put("list", metadata);
        return R.ok(data);
    }

    /**
     * series查询
     *
     * @param match
     * @param start
     * @param end
     * @return
     */
    @GetMapping("/series")
    @ResponseBody
    public R querySeries(String match, String start, String end) {
        ValidateUtils.is(match).notNull(RCode.PROMSERVER_PARAM_MATCH_ISNULL);

        Map series = metricService.series(match, start, end);
        return R.ok(series);
    }

}
