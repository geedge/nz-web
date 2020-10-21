package com.nis.modules.metric.dto;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.alert.dao.AlertRuleDao;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.asset.dao.AssetPingDao;
import com.nis.modules.asset.dao.IdcDao;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.AssetPing;
import com.nis.modules.asset.entity.Idc;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.endpoint.service.EndpointService;
import com.nis.modules.sys.dao.SysConfigDao;
import com.nis.modules.sys.entity.SysConfigEntity;
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

    @Autowired
    private IdcDao idcDao;

    @Autowired
    private AssetPingDao assetPingDao;

    @Autowired
    private SysConfigDao sysConfigDao;
    
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

        // 先清空指标 再更新数据防止旧数据存在
        while (true) {
            Search find = meterRegistry.find("nz_asset_nums");
            Meter meter = find.meter();
            if (ToolUtil.isNotEmpty(meter)) {
                meterRegistry.remove(meter);
            } else {
                break;
            }
        }
       
        /**
         * asset_nums指标
         */
        List<MetricsDto> assetNums = assetService.queryAssetStatisticsByModel();
        if(ToolUtil.isNotEmpty(assetNums)) {
            MetricsDto metricsDto;
        	for(int i=0;i<assetNums.size();i++) {
                metricsDto = assetNums.get(i);
                // 这里将统计值扔进map操作是避免弱引用  统计结果出现NaN
        		map.put("assetNums"+i, metricsDto.getTotal());
        		String key="assetNums"+i;
        		Gauge.builder("nz_asset_nums", map, x -> x.get(key))
                .tags("model", metricsDto.getName(), "type", metricsDto.getAssetType(), "vendor", metricsDto.getVendor())
                .description("Asset statistics info")
                .register(meterRegistry);
        	}
        }else {
        	map.put("assetNums", 0.0);
    		String key="assetNums";
    		Gauge.builder("nz_asset_nums", map, x -> x.get(key))
            .tags("model","null","type","null","vendor","null")
            .description("Asset statistics info")
            .register(meterRegistry);
        }


        // 先清空指标 再更新数据防止旧数据存在
        while (true) {
            Search find = meterRegistry.find("nz_asset_offline");
            Meter meter = find.meter();
            if (ToolUtil.isNotEmpty(meter)) {
                meterRegistry.remove(meter);
            } else {
                break;
            }
        }
        /**
         * asset_offline指标
         */
        MetricsDto assetOffline = assetService.queryAssetStatisticsByModelOffLine();
        if(assetOffline!=null) {
        	map.put("assetOffline", assetOffline.getTotal());
        	Gauge.builder("nz_asset_offline", map, x -> x.get("assetOffline"))
            .description("Asset offline statistics info")
            .register(meterRegistry);
        }else {
        	map.put("assetOffline", 0.0);
        	Gauge.builder("nz_asset_offline", map, x -> x.get("assetOffline"))
            .description("Asset offline statistics info")
            .register(meterRegistry);
        }


        // 先清空指标 再更新数据防止旧数据存在
        while (true) {
            Search find = meterRegistry.find("nz_asset_online_nums");
            Meter meter = find.meter();
            if (ToolUtil.isNotEmpty(meter)) {
                meterRegistry.remove(meter);
            } else {
                break;
            }
        }
        /**
         * asset_online_nums指标
         */
        List<MetricsDto> assetOnlineNums = assetService.queryAssetStatisticsByModelOnLine();
        if(ToolUtil.isNotEmpty(assetOnlineNums)) {
            MetricsDto metricsDto;
        	for(int i=0;i<assetOnlineNums.size();i++) {
                metricsDto = assetOnlineNums.get(i);
                // 这里将统计值扔进map操作是避免弱引用  统计结果出现NaN
        		map.put("assetOnlineNums"+i, metricsDto.getTotal());
        		String key="assetOnlineNums"+i;
        		Gauge.builder("nz_asset_online_nums", map, x -> x.get(key))
                .tags("model", metricsDto.getName(), "type", metricsDto.getAssetType(), "vendor", metricsDto.getVendor())
                .description("Asset online statistics info")
                .register(meterRegistry);
        	}
        }else {
        	map.put("assetOnlineNums", 0.0);
    		String key="assetOnlineNums";
    		Gauge.builder("nz_asset_online_nums", map, x -> x.get(key))
            .tags("model","null","type","null","vendor","null")
            .description("Asset online statistics info")
            .register(meterRegistry);
        }



        // 先清空指标 再更新数据防止旧数据存在
        while (true) {
            Search find = meterRegistry.find("nz_endpoint_nums");
            Meter meter = find.meter();
            if (ToolUtil.isNotEmpty(meter)) {
                meterRegistry.remove(meter);
            } else {
                break;
            }
        }
        /**
         * endpoint_nums指标
         */
        List<MetricsDto> endpointNums = endpointService.queryEndpointStatisticsInfoByModule();
        if(ToolUtil.isNotEmpty(endpointNums)) {
        	for(int i=0;i<endpointNums.size();i++) {
        		// 这里将统计值扔进map操作是避免弱引用  统计结果出现NaN
        		map.put("endpointNums"+i, endpointNums.get(i).getTotal());
        		String key="endpointNums"+i;
        		Gauge.builder("nz_endpoint_nums", map, x -> x.get(key))
                .tags("module",endpointNums.get(i).getName().toString())
                .description("Endpoint statistics info")
                .register(meterRegistry);
        	}
        }else {
        	map.put("endpointNums", 0.0);
    		String key="endpointNums";
    		Gauge.builder("nz_endpoint_nums", map, x -> x.get(key))
            .tags("module","null")
            .description("Endpoint statistics info")
            .register(meterRegistry);
        }


        /**
         * nz_asset_ping 指标
         */
        // 先清空指标 再更新数据防止旧数据存在
        while (true) {
            Search find = meterRegistry.find("nz_asset_ping");
            Meter meter = find.meter();
            if (ToolUtil.isNotEmpty(meter)) {
                meterRegistry.remove(meter);
            } else {
                break;
            }
        }

        SysConfigEntity pingSwitchConfig = sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_ASSET_PING_SWITCH));
        // ping 开关为开启状态统计 nz_asset_ping 指标
        if (pingSwitchConfig != null && Constant.ASSET_PING_OPEN_SWITCH.equals(pingSwitchConfig.getParamValue())) {
            List<AssetPing> assetPings = assetPingDao.selectList(new QueryWrapper<AssetPing>().lambda().eq(AssetPing::getStatus, 1));
            Map<String, Double> assetHostAndRttMap = assetPings.stream().collect(Collectors.toMap(AssetPing::getHost, AssetPing::getRtt));
            if (ToolUtil.isNotEmpty(assetHostAndRttMap)) {

                List<Idc> idcList = idcDao.selectList(null);
                Map<Integer, String> idcIdAndNameMap = idcList.stream().collect(Collectors.toMap(Idc::getId, Idc::getName));
                List<Asset> assetList = assetService.list();
                Map<String, Asset> assetHostAndAssetMap = new HashMap<>();
                for (Asset asset : assetList) {
                    assetHostAndAssetMap.put(asset.getHost(), asset);
                }

                String assetHost;
                Asset asset;
                int i = 0;
                for (Map.Entry<String, Double> entry : assetHostAndRttMap.entrySet()) {
                    assetHost = entry.getKey();
                    asset = assetHostAndAssetMap.get(assetHost);
                    if (ToolUtil.isEmpty(asset)) continue;
                    String key = "nz_asset_ping" + i++;

                    map.put(key, entry.getValue());
                    Gauge.builder("nz_asset_ping", map, x -> x.get(key))
                            .tags("sn", asset.getSn(), "host", assetHost, "datacenter", idcIdAndNameMap.get(asset.getIdcId()))
                            .description("Asset ping statistics info")
                            .register(meterRegistry);
                }
            }
        }
    }
}
