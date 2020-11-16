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
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
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

    @Autowired
    private PromserverService promserverService;

    NezhaMetrics() {
        map = new HashMap<>();
    }

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        if (map == null) {
            map = new HashMap<>();
        }

        /**
         *   alert_nums指标
         */
        List<MetricsDto> alertNums = alertMessageService.queryAlertMessageStatisticsInfoByRule();
        Map<Integer, Double> ruleIdAndMessageTotalMap = alertNums.stream().collect(Collectors.toMap(MetricsDto::getId, MetricsDto::getTotal));

        // 先清空指标 再更新数据防止旧数据存在
        this.cleanMeterInRegistryByName(meterRegistry, "nz_alert_nums");
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

        /**
         * asset_nums指标
         */
        this.cleanMeterInRegistryByName(meterRegistry, "nz_asset_nums");
        List<MetricsDto> assetNums = assetService.queryAssetStatisticsByModel();
        if (ToolUtil.isNotEmpty(assetNums)) {
            MetricsDto metricsDto;
            for (int i = 0; i < assetNums.size(); i++) {
                metricsDto = assetNums.get(i);
                // 这里将统计值扔进map操作是避免弱引用  统计结果出现NaN
                map.put("assetNums" + i, metricsDto.getTotal());
                String key = "assetNums" + i;
                Gauge.builder("nz_asset_nums", map, x -> x.get(key))
                        .tags("model", metricsDto.getName(), "type", metricsDto.getAssetType(), "vendor", metricsDto.getVendor(), "state", metricsDto.getState())
                        .description("Asset statistics info")
                        .register(meterRegistry);
            }
        } else {
            map.put("assetNums", 0.0);
            String key = "assetNums";
            Gauge.builder("nz_asset_nums", map, x -> x.get(key))
                    .tags("model", "null", "type", "null", "vendor", "null")
                    .description("Asset statistics info")
                    .register(meterRegistry);
        }


        /**
         * endpoint_nums指标
         */
        this.cleanMeterInRegistryByName(meterRegistry, "nz_endpoint_nums");
        List<MetricsDto> endpointNums = endpointService.queryEndpointStatisticsInfoByModule();
        if (ToolUtil.isNotEmpty(endpointNums)) {
            for (int i = 0; i < endpointNums.size(); i++) {
                // 这里将统计值扔进map操作是避免弱引用  统计结果出现NaN
                map.put("endpointNums" + i, endpointNums.get(i).getTotal());
                String key = "endpointNums" + i;
                Gauge.builder("nz_endpoint_nums", map, x -> x.get(key))
                        .tags("module", endpointNums.get(i).getName())
                        .description("Endpoint statistics info")
                        .register(meterRegistry);
            }
        } else {
            map.put("endpointNums", 0.0);
            String key = "endpointNums";
            Gauge.builder("nz_endpoint_nums", map, x -> x.get(key))
                    .tags("module", "null")
                    .description("Endpoint statistics info")
                    .register(meterRegistry);
        }


        /**
         * asset ping 相关指标
         */
        this.cleanMeterInRegistryByName(meterRegistry, "nz_asset_ping");
        this.cleanMeterInRegistryByName(meterRegistry, "nz_asset_offline");
        this.cleanMeterInRegistryByName(meterRegistry, "nz_asset_online_nums");

        SysConfigEntity pingSwitchConfig = sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_ASSET_PING_SWITCH));
        // ping 开关为开启状态统计 nz_asset_ping 指标
        if (pingSwitchConfig != null && Constant.ASSET_PING_OPEN_SWITCH.equals(pingSwitchConfig.getParamValue())) {
            List<AssetPing> assetPings = assetPingDao.selectList(null);
            List<Asset> assetList = assetService.list();
            Map<String, Asset> assetHostAndAssetMap = new HashMap<>();
            for (Asset asset : assetList) {
                assetHostAndAssetMap.put(asset.getHost(), asset);
            }

            /**
             * asset_offline指标
             */
            List<AssetPing> offLineAssetPingList = assetPings.stream().filter(assetPing -> assetPing.getStatus().equals(0)).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(offLineAssetPingList)) {
                String host;
                for (int i = 0; i < offLineAssetPingList.size(); i++) {
                    host = offLineAssetPingList.get(i).getHost();
                    map.put("nz_asset_offline" + i, 1d);
                    String key = "nz_asset_offline" + i;
                    Gauge.builder("nz_asset_offline", map, x -> x.get(key))
                            .tags("host", host, "state", assetHostAndAssetMap.get(host).getState() + "")
                            .description("Asset statistics info")
                            .register(meterRegistry);
                }
            } else {
                map.put("assetOffline", 0.0);
                Gauge.builder("nz_asset_offline", map, x -> x.get("assetOffline"))
                        .description("Asset offline statistics info")
                        .register(meterRegistry);
            }

            /**
             * asset_online_nums指标
             */
            List<MetricsDto> assetOnlineNums = assetService.queryAssetStatisticsByModelOnLine();
            if (ToolUtil.isNotEmpty(assetOnlineNums)) {
                MetricsDto metricsDto;
                for (int i = 0; i < assetOnlineNums.size(); i++) {
                    metricsDto = assetOnlineNums.get(i);
                    map.put("assetOnlineNums" + i, metricsDto.getTotal());
                    String key = "assetOnlineNums" + i;
                    Gauge.builder("nz_asset_online_nums", map, x -> x.get(key))
                            .tags("model", metricsDto.getName(), "type", metricsDto.getAssetType(), "vendor", metricsDto.getVendor())
                            .description("Asset online statistics info")
                            .register(meterRegistry);
                }
            } else {
                map.put("assetOnlineNums", 0.0);
                String key = "assetOnlineNums";
                Gauge.builder("nz_asset_online_nums", map, x -> x.get(key))
                        .tags("model", "null", "type", "null", "vendor", "null")
                        .description("Asset online statistics info")
                        .register(meterRegistry);
            }

            /**
             * nz_asset_ping 指标
             */
            List<AssetPing> onLineAssetPingList = assetPings.stream().filter(assetPing -> assetPing.getStatus().equals(1)).collect(Collectors.toList());
            Map<String, Double> assetHostAndRttMap = onLineAssetPingList.stream().collect(Collectors.toMap(AssetPing::getHost, AssetPing::getRtt));
            if (ToolUtil.isNotEmpty(assetHostAndRttMap)) {

                List<Idc> idcList = idcDao.selectList(null);
                Map<Integer, String> idcIdAndNameMap = idcList.stream().collect(Collectors.toMap(Idc::getId, Idc::getName));
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


        /**
         * nz_prometheus_status 指标
         */
        this.cleanMeterInRegistryByName(meterRegistry, "nz_prometheus_status");
        List<Promserver> promserverList = promserverService.list();
        if (CollectionUtils.isNotEmpty(promserverList)) {
            List<Idc> idcList = idcDao.selectList(null);
            Map<Integer, String> idcIdAndNameMap = idcList.stream().collect(Collectors.toMap(Idc::getId, Idc::getName));

            Promserver promserver;
            for (int i = 0; i < promserverList.size(); i++) {
                promserver = promserverList.get(i);
                String key = "nz_prometheus_status" + i;
                map.put(key, promserver.getStatus().doubleValue());
                Gauge.builder("nz_prometheus_status", map, x -> x.get(key))
                        .tags("datacenter", idcIdAndNameMap.get(promserver.getIdcId()), "type", Constant.PromserverType.getPromserverType(promserver.getType()).getName(), "host", promserver.getHost(), "port", promserver.getPort() + "")
                        .description("Prometheus status")
                        .register(meterRegistry);
            }
        }
    }

    /**
     * 按照指标名称清除之前数据
     *
     * @param registry
     * @param meterName
     */
    private void cleanMeterInRegistryByName(MeterRegistry registry, String meterName) {
        while (true) {
            Search find = registry.find(meterName);
            Meter meter = find.meter();
            if (ToolUtil.isNotEmpty(meter)) {
                registry.remove(meter);
            } else {
                break;
            }
        }
    }
}
