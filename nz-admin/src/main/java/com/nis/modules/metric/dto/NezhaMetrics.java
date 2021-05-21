package com.nis.modules.metric.dto;

import com.nis.common.utils.Constant;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.alert.dao.AlertRuleDao;
import com.nis.modules.asset.dao.AssetModelDao;
import com.nis.modules.asset.dao.DcDao;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.module.dao.MonitorModuleDao;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.search.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class NezhaMetrics implements MeterBinder {

    public Map<String, Double> map;

    NezhaMetrics() {
        map = new HashMap<>();
    }

    @Autowired
    private DcDao dcDao;

    @Autowired
    private AlertRuleDao alertRuleDao;

    @Autowired
    private MonitorModuleDao monitorModuleDao;

    @Autowired
    private PromserverService promserverService;

    @Autowired
    private AssetModelDao modelDao;

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        if (map == null) {
            map = new HashMap<>(16);
        }

        // 清空指标数据
        this.cleanMeterInRegistryByName(meterRegistry, "nz_alert_nums", "nz_asset_nums", "nz_endpoint_nums", "nz_prometheus_status");

        /**
         * alert_nums指标 / 根据告警规则 id 统计激活状态的告警消息数量
         */
        List<MetricsDto> nzAlertMumsMetrics = alertRuleDao.nzAlertMumsMetric();
        for (int i = 0; i < nzAlertMumsMetrics.size(); i++) {
            MetricsDto dto = nzAlertMumsMetrics.get(i);
            String key = "alertNums" + i;
            map.put(key, dto.getTotal().doubleValue());
            Gauge.builder("nz_alert_nums", map, x -> x.get(key))
                    .tags("id", dto.getId() + "", "name", dto.getName(), "severity", dto.getSeverity())
                    .description("Alert statistics info")
                    .register(meterRegistry);
        }

        /**
         * nz_asset_nums
         */
        List<MetricsDto> nzAssetNumsMetric = modelDao.nzAssetNumsMetric();
        for (int i = 0; i < nzAssetNumsMetric.size(); i++) {
            MetricsDto dto = nzAssetNumsMetric.get(i);
            // 这里将统计值扔进 map 操作是避免弱引用  统计结果出现NaN
            String key = "assetNums" + i;
            map.put(key, dto.getTotal().doubleValue());
            Gauge.builder("nz_asset_nums", map, x -> x.get(key))
                    .tags("model", dto.getModel(), "type", dto.getType() != null ? dto.getType() : "", "brand", dto.getBrand() != null ? dto.getBrand() : "", "state", dto.getState() != null ? dto.getState() : "")
                    .description("Asset statistics info")
                    .register(meterRegistry);
        }

        /**
         * nz_endpoint_nums
         */
        List<MetricsDto> nzEndpointNumsMetric = monitorModuleDao.nzEndpointNumsMetric();
        for (int i = 0; i < nzEndpointNumsMetric.size(); i++) {
            MetricsDto dto = nzEndpointNumsMetric.get(i);
            String key = "endpointNums" + i;
            map.put(key, dto.getTotal().doubleValue());
            Gauge.builder("nz_endpoint_nums", map, x -> x.get(key))
                    .tags("module", dto.getName())
                    .description("Endpoint statistics info")
                    .register(meterRegistry);
        }

        /**
         * nz_prometheus_status
         */
        List<Promserver> promserverList = promserverService.list();
        List<Dc> dcList = dcDao.selectList(null);
        Map<Integer, String> idcIdAndNameMap = dcList.stream().collect(Collectors.toMap(Dc::getId, Dc::getName));

        for (int i = 0; i < promserverList.size(); i++) {
            Promserver promserver = promserverList.get(i);
            String key = "status" + i;
            map.put(key, promserver.getStatus().doubleValue());
            Gauge.builder("nz_prometheus_status", map, x -> x.get(key))
                    .tags("datacenter", idcIdAndNameMap.get(promserver.getDcId()) == null ? "null" : idcIdAndNameMap.get(promserver.getDcId()), "type", Constant.PromserverType.getPromserverType(promserver.getType()).getName(), "host", promserver.getHost(), "port", promserver.getPort() + "")
                    .description("Prometheus statistics info")
                    .register(meterRegistry);
        }
    }

    /**
     * 按照指标名称清除之前数据
     *
     * @param registry
     * @param meterNames
     */
    private void cleanMeterInRegistryByName(MeterRegistry registry, String... meterNames) {
        for (String meterName : meterNames) {
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
}
