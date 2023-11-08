package com.nis.modules.endpoint.controller;

import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.endpoint.entity.MetadataMetric;
import com.nis.modules.endpoint.service.MetadataMetricService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/metric")
public class MetricController {

    @Autowired
    private MetadataMetricService metricService;

    /**
     * 元数据查询
     *
     * @param endpointId
     * @return
     * @throws Exception 
     */
    @GetMapping("/metadata")
    @ResponseBody
    public R queryMetadata(Integer endpointId) {
        ValidateUtils.is(endpointId).notNull(RCode.ENDPOINT_ID_ISNULL);
        List<MetadataMetric> metadata;
		try {
			metadata = metricService.metadata(endpointId);
		} catch (IOException e) {
			throw new NZException(RCode.ERROR);
		}
        Map data = new HashMap();
        data.put("list", metadata);
        return R.ok(data);
    }


}
