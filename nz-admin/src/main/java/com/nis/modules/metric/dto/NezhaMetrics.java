package com.nis.modules.metric.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.endpoint.service.EndpointService;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

@Component
public class NezhaMetrics implements MeterBinder {
    public Map<String, Double> map;
    @Autowired
    private AlertMessageService alertMessageService;
    @Autowired
    private AssetService assetService;
    @Autowired
    private EndpointService endpointService;
    
    NezhaMetrics() {
        map = new HashMap<>();
    }

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        if(map==null) {
        	map=new HashMap<>();
        }
        
        /**
         *   alert_nums指标
         */
        List<MetricsDto> alertNums = alertMessageService.queryAlertMessageStatisticsInfo();
        
        if(alertNums!=null&&alertNums.size()>0) {
        	for(int i=0;i<alertNums.size();i++) {
        		// 这里将统计值扔进map操作是避免弱引用  统计结果出现NaN
        		map.put("alertNums"+i, alertNums.get(i).getTotal());
        		String key="alertNums"+i;
        		Gauge.builder("alert_nums", map, x -> x.get(key))
                .tags("type",alertNums.get(i).getType().toString(),"severity",alertNums.get(i).getSeverity(),"name","alert_message")
                .description("Alert message statistics info")
                .register(meterRegistry);
        	}
        }else {
        	map.put("alertNums", 0.0);
    		String key="alertNums";
    		Gauge.builder("alert_nums", map, x -> x.get(key))
            .tags("type","null","severity","null","name","alert_message")
            .description("Alert message statistics info")
            .register(meterRegistry);
        }
        /**
         * asset_nums指标
         */
        List<MetricsDto> assetNums = assetService.queryAssetStatisticsByModel();
        if(assetNums!=null && assetNums.size()>0) {
        	for(int i=0;i<assetNums.size();i++) {
        		// 这里将统计值扔进map操作是避免弱引用  统计结果出现NaN
        		map.put("assetNums"+i, assetNums.get(i).getTotal());
        		String key="assetNums"+i;
        		Gauge.builder("asset_nums", map, x -> x.get(key))
                .tags("model",assetNums.get(i).getName().toString())
                .description("Asset statistics info")
                .register(meterRegistry);
        	}
        }else {
        	map.put("assetNums", 0.0);
    		String key="assetNums";
    		Gauge.builder("asset_nums", map, x -> x.get(key))
            .tags("model","null")
            .description("Asset statistics info")
            .register(meterRegistry);
        }
        /**
         * asset_offline指标
         */
        MetricsDto assetOffline = assetService.queryAssetStatisticsByModelOffLine();
        if(assetOffline!=null) {
        	map.put("assetOffline", assetOffline.getTotal());
        	Gauge.builder("asset_offline_num", map, x -> x.get("assetOffline"))
            .description("Asset offline statistics info")
            .register(meterRegistry);
        }else {
        	map.put("assetOffline", 0.0);
        	Gauge.builder("asset_offline_num", map, x -> x.get("assetOffline"))
            .description("Asset offline statistics info")
            .register(meterRegistry);
        }
        
        /**
         * asset_online_nums指标
         */
        List<MetricsDto> assetOnlineNums = assetService.queryAssetStatisticsByModelOnLine();
        if(assetOnlineNums!=null && assetOnlineNums.size()>0) {
        	for(int i=0;i<assetOnlineNums.size();i++) {
        		// 这里将统计值扔进map操作是避免弱引用  统计结果出现NaN
        		map.put("assetOnlineNums"+i, assetOnlineNums.get(i).getTotal());
        		String key="assetOnlineNums"+i;
        		Gauge.builder("asset_online_nums", map, x -> x.get(key))
                .tags("model",assetOnlineNums.get(i).getName().toString())
                .description("Asset online statistics info")
                .register(meterRegistry);
        	}
        }else {
        	map.put("assetOnlineNums", 0.0);
    		String key="assetOnlineNums";
    		Gauge.builder("asset_online_nums", map, x -> x.get(key))
            .tags("model","null")
            .description("Asset online statistics info")
            .register(meterRegistry);
        }
        /**
         * endpoint_nums指标
         */
        List<MetricsDto> endpointNums = endpointService.queryEndpointStatisticsInfoByModel();
        if(endpointNums!=null&&endpointNums.size()>0) {
        	for(int i=0;i<endpointNums.size();i++) {
        		// 这里将统计值扔进map操作是避免弱引用  统计结果出现NaN
        		map.put("endpointNums"+i, endpointNums.get(i).getTotal());
        		String key="endpointNums"+i;
        		Gauge.builder("endpoint_nums", map, x -> x.get(key))
                .tags("model",endpointNums.get(i).getName().toString())
                .description("Endpoint statistics info")
                .register(meterRegistry);
        	}
        }else {
        	map.put("endpointNums", 0.0);
    		String key="endpointNums";
    		Gauge.builder("endpoint_nums", map, x -> x.get(key))
            .tags("model","null")
            .description("Endpoint statistics info")
            .register(meterRegistry);
        }
    }
}
