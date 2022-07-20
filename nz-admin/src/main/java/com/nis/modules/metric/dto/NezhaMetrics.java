package com.nis.modules.metric.dto;

import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.alert.dao.AlertHandlerDao;
import com.nis.modules.alert.dao.AlertRuleDao;
import com.nis.modules.asset.dao.AssetModelDao;
import com.nis.modules.asset.dao.DcDao;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.module.dao.MonitorModuleDao;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.AgentService;
import com.nis.modules.sys.service.SysConfService;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.search.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class NezhaMetrics implements MeterBinder {

    @Autowired
    private DcDao dcDao;

    @Autowired
    private AlertRuleDao alertRuleDao;

    @Autowired
    private MonitorModuleDao monitorModuleDao;

    @Autowired
    private AgentService agentService;

    @Autowired
    private AssetModelDao modelDao;

    @Autowired
    private SysConfService sysConfService;

    @Autowired
    private AlertHandlerDao alertHandlerDao;

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        // 清空指标数据
        this.cleanMeterInRegistryByName(meterRegistry, "nz_alert_nums", "nz_asset_nums", "nz_endpoint_nums", "nz_agent_status", "nz_alerts");

        /**
         * alert_nums指标 / 根据告警规则 id 统计激活状态的告警消息数量
         */
        List<MetricsDto> nzAlertMumsMetrics = alertRuleDao.nzAlertMumsMetric();
        for (int i = 0; i < nzAlertMumsMetrics.size(); i++) {
            MetricsDto dto = nzAlertMumsMetrics.get(i);
            Gauge.builder("nz_alert_nums", dto, value -> dto.getTotal())
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
            Gauge.builder("nz_asset_nums", dto, value -> dto.getTotal())
                    .tags("model", dto.getModel(), "type", dto.getType(), "brand", dto.getBrand(), "state", dto.getState())
                    .description("Asset statistics info")
                    .register(meterRegistry);
        }

        /**
         * nz_endpoint_nums
         */
        List<MetricsDto> nzEndpointNumsMetric = monitorModuleDao.nzEndpointNumsMetric();
        for (int i = 0; i < nzEndpointNumsMetric.size(); i++) {
            MetricsDto dto = nzEndpointNumsMetric.get(i);
            Gauge.builder("nz_endpoint_nums", dto, value -> dto.getTotal())
                    .tags("module", dto.getName())
                    .description("Endpoint statistics info")
                    .register(meterRegistry);
        }

        /**
         * nz_agent_status
         */
        List<Agent> agentList = agentService.list();
        List<Dc> dcList = dcDao.selectList(null);
        Map<Integer, String> idcIdAndNameMap = dcList.stream().collect(Collectors.toMap(Dc::getId, Dc::getName));

        for (int i = 0; i < agentList.size(); i++) {
            Agent agent = agentList.get(i);
            Gauge.builder("nz_agent_status", agent,value -> agent.getStatus())
                    .tags("datacenter", idcIdAndNameMap.get(agent.getDcId()) == null ? "null" : idcIdAndNameMap.get(agent.getDcId()), "type", Constant.AgentType.getAgentType(agent.getType()).getName(), "host", agent.getHost(), "port", agent.getPort() + "")
                    .description("Prometheus statistics info")
                    .register(meterRegistry);
        }

        /**
         * nz_alerts
         */
        String nzAlertsMetricsEnabled = sysConfService.getValue("nz_alerts_metrics_enabled");
        nzAlertsMetricsEnabled = Tool.StrUtil.emptyToDefault(nzAlertsMetricsEnabled, "on");
        if (Tool.StrUtil.equals("on", nzAlertsMetricsEnabled)) {
            List<String> activeMessageLabelsList = alertHandlerDao.selectAlertMessageLabelsList();
            activeMessageLabelsList = activeMessageLabelsList.stream().filter(s -> Tool.StrUtil.isNotEmpty(s)).distinct().collect(Collectors.toList());

            for (String labelsJSONStr : activeMessageLabelsList) {
                // labels
                List<Tag> tags = new ArrayList<>();
                Map<String, Object> labelsMap = JSONObject.parseObject(labelsJSONStr, Map.class);
                for (Map.Entry<String, Object> entry : labelsMap.entrySet()) {
                    String key = entry.getKey();
                    if (Tool.StrUtil.equals("__name__", key)) continue;
                    tags.add(new ImmutableTag(key, entry.getValue().toString()));
                }

                Gauge.builder("nz_alerts", labelsMap, value -> 0d)
                        .tags(tags)
                        .description("Alert active message statistics info")
                        .register(meterRegistry);
            }
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
