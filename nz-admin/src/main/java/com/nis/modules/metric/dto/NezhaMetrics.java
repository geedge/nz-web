package com.nis.modules.metric.dto;

import com.nis.common.utils.ToolUtil;
import com.nis.modules.alert.dao.AlertRuleDao;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.endpoint.service.EndpointService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.search.Search;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class NezhaMetrics implements MeterBinder {
    public Map<String, Double> map;
    @Autowired
    private AlertMessageService alertMessageService;
    @Autowired
    private AssetService assetService;
    @Autowired
    private EndpointService endpointService;

    @Autowired
    private AlertRuleDao alertRuleDao;
    
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
        List<MetricsDto> alertNums = alertMessageService.queryAlertMessageStatisticsInfoByRule();
        Map<Integer, Double> ruleIdAndMessageTotalMap = alertNums.stream().collect(Collectors.toMap(MetricsDto::getId, MetricsDto::getTotal));

        // 先清空指标 再更新数据防止旧数据存在
        while (true) {
            Search find = meterRegistry.find("nz_alert_nums");
            Meter meter = find.meter();
            if (ToolUtil.isNotEmpty(meter)) {
                meterRegistry.remove(meter);
            } else {
                break;
            }
        }

        List<AlertRuleEntity> rules = alertRuleDao.selectList(null);

        if (CollectionUtils.isNotEmpty(rules)) {
            AlertRuleEntity ruleEntity;
            Double total;
            for (int i = 0; i < rules.size(); i++) {
                ruleEntity = rules.get(i);
                total = ruleIdAndMessageTotalMap.get(ruleEntity.getId());
                map.put("alertNums" + i, ToolUtil.isEmpty(total) ? 0 : total);

                String key = "alertNums" + i;
                Gauge.builder("nz_alert_nums", map, x -> x.get(key))
                        .tags("id", ruleEntity.getId() + "", "name", ruleEntity.getAlertName(), "severity", ruleEntity.getSeverity())
                        .description("Alert message statistics info")
                        .register(meterRegistry);
            }
        }
        /*List<MetricsDto> alertNums = alertMessageService.queryAlertMessageStatisticsInfo();
        // 先清空指标 再更新数据防止旧数据存在
        Search find = meterRegistry.find("nz_alert_nums");
        Meter meter = find.meter();
        if(meter!=null) {
        	meterRegistry.remove(meter);
        }
        if(alertNums!=null&&alertNums.size()>0) {
        	for(int i=0;i<alertNums.size();i++) {
        		// 这里将统计值扔进map操作是避免弱引用  统计结果出现NaN
        		map.put("alertNums"+i, alertNums.get(i).getTotal());
        		String key="alertNums"+i;
        		*/
        /*Gauge.builder("nz_alert_nums", map, x -> x.get(key))
                .tags("type",alertNums.get(i).getType().toString(),"severity",alertNums.get(i).getSeverity(),"name","alert_message")
                .description("Alert message statistics info")
                .register(meterRegistry);*//*
                Gauge.builder("nz_alert_nums", map, x -> x.get(key))
                        .tags("severity",alertNums.get(i).getSeverity(),"name","alert_message")
                        .description("Alert message statistics info")
                        .register(meterRegistry);
        	}
        }else {
        	map.put("alertNums", 0.0);
    		String key="alertNums";
    		*//*Gauge.builder("nz_alert_nums", map, x -> x.get(key))
            .tags("type","null","severity","null","name","alert_message")
            .description("Alert message statistics info")
            .register(meterRegistry);*//*
            Gauge.builder("nz_alert_nums", map, x -> x.get(key))
                    .tags("severity","null","name","alert_message")
                    .description("Alert message statistics info")
                    .register(meterRegistry);
        }*/
        /**
         * asset_nums指标
         */
        List<MetricsDto> assetNums = assetService.queryAssetStatisticsByModel();
        if(assetNums!=null && assetNums.size()>0) {
        	for(int i=0;i<assetNums.size();i++) {
        		// 这里将统计值扔进map操作是避免弱引用  统计结果出现NaN
        		map.put("assetNums"+i, assetNums.get(i).getTotal());
        		String key="assetNums"+i;
        		Gauge.builder("nz_asset_nums", map, x -> x.get(key))
                .tags("model",assetNums.get(i).getName().toString())
                .description("Asset statistics info")
                .register(meterRegistry);
        	}
        }else {
        	map.put("assetNums", 0.0);
    		String key="assetNums";
    		Gauge.builder("nz_asset_nums", map, x -> x.get(key))
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
        	Gauge.builder("nz_asset_offline_num", map, x -> x.get("assetOffline"))
            .description("Asset offline statistics info")
            .register(meterRegistry);
        }else {
        	map.put("assetOffline", 0.0);
        	Gauge.builder("nz_asset_offline_num", map, x -> x.get("assetOffline"))
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
        		Gauge.builder("nz_asset_online_nums", map, x -> x.get(key))
                .tags("model",assetOnlineNums.get(i).getName().toString())
                .description("Asset online statistics info")
                .register(meterRegistry);
        	}
        }else {
        	map.put("assetOnlineNums", 0.0);
    		String key="assetOnlineNums";
    		Gauge.builder("nz_asset_online_nums", map, x -> x.get(key))
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
        		Gauge.builder("nz_endpoint_nums", map, x -> x.get(key))
                .tags("model",endpointNums.get(i).getName().toString())
                .description("Endpoint statistics info")
                .register(meterRegistry);
        	}
        }else {
        	map.put("endpointNums", 0.0);
    		String key="endpointNums";
    		Gauge.builder("nz_endpoint_nums", map, x -> x.get(key))
            .tags("model","null")
            .description("Endpoint statistics info")
            .register(meterRegistry);
        }
    }
}
