package com.nis.modules.asset.job;

import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.DateUtil;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.asset.dao.AssetAssetDao;
import com.nis.modules.asset.dao.AssetPingDao;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetPing;
import com.nis.modules.asset.service.AssetPingService;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@DisallowConcurrentExecution
public class AssetPingStatusJob extends QuartzJobBean {

    private static Log log = Log.get();

    @Autowired
    private AssetPingDao assetPingDao;

    @Autowired
    private AssetPingService assetPingService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private PromserverService promserverService;

    @Autowired
    private AssetAssetDao assetDao;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 检查 haMode
        boolean haMode = sysConfigService.checkHaMode();
        if (!haMode) return;

        StopWatch sw = new StopWatch();
        sw.start();
        try {
            // 获取 ping 数据
            this.execAssetPing();
        } catch (Exception e) {
            log.error("asset ping 采集异常", e);
        } finally {
            sw.stop();
        }
        log.info("asset ping Run Time: {}", sw.toString());
    }

    @Transactional(rollbackFor = Exception.class)
    public void execAssetPing() throws RuntimeException {
        Promserver promserver = promserverService.getOneAvaPromServer(null, null, Constant.PromserverType.GLOBAL.getValue());
        if (promserver == null) {
            log.info("asset ping 状态定时任务不执行,当前暂无可用 promserver");
            return;
        }

        SysConfigEntity configEntity = sysConfigService.getOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, Constant.CONFAGENT_PATH));
        String confagentPath = configEntity.getParamValue();

        String pingStateUrlPath = confagentPath + "/prometheus/proxy/api/v1/query?query=probe_success";
        String rttUrlPath = confagentPath + "/prometheus/proxy/api/v1/query?query=probe_icmp_duration_seconds{phase='rtt'}";

        Map<String, String> assetHostAndStateMap = this.getMetricsDatabByPath(promserver, pingStateUrlPath);
        Map<String, String> assetHostAndRttMap = this.getMetricsDatabByPath(promserver, rttUrlPath);

        // 如果当前未查询到数据，放弃本次更新
        if (ToolUtil.isEmpty(assetHostAndStateMap) || ToolUtil.isEmpty(assetHostAndRttMap)) return;

        List<AssetAsset> assetList = assetDao.selectList(null);
        Map<String, Integer> assetHostAndIdMap = assetList.stream().collect(Collectors.toMap(AssetAsset::getManageIp, AssetAsset::getId));

        // 可达 ping asset集合
        List<AssetPing> upAssetPings = new ArrayList<>();
        // 不可达 ping asset 集合
        List<AssetPing> downAssetPings = new ArrayList<>();
        for (Map.Entry<String, String> entry : assetHostAndStateMap.entrySet()) {
            String host = entry.getKey();
            Integer assetId = assetHostAndIdMap.get(host);
            if (assetId == null) continue;

            String state = entry.getValue();
            // up
            if ("1".equals(state)) {
                // 秒转毫秒 ， 四舍五入 保留两位小数
                BigDecimal bd = BigDecimal.valueOf(Double.valueOf(assetHostAndRttMap.get(host)));
                bd = bd.multiply(BigDecimal.valueOf(1000));
                Double d = bd.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                upAssetPings.add(new AssetPing(assetId, host, 1, d, DateUtil.getUTCTimeByConfigTimeZone()));
            } else {
                // down
                downAssetPings.add(new AssetPing(assetId, host, 0, null, null));
            }
        }
        // 更新数据库
        List<AssetPing> list = assetPingService.list();
        Map<String, AssetPing> hostAndAssetPing = list.stream().collect(Collectors.toMap(AssetPing::getHost, Function.identity()));

        // 处理不可达资产的最后可达时间
        for (AssetPing assetPing : downAssetPings) {
            AssetPing ping = hostAndAssetPing.get(assetPing.getHost());
            if (ping != null) assetPing.setLastUpdate(ping.getLastUpdate());
        }

        upAssetPings.addAll(downAssetPings);

        assetPingDao.deleteAll();
        assetPingService.saveBatch(upAssetPings);
    }


    /**
     * 根据 url 请求带理接口并返回 asset host / state ，rtt 数据
     *
     * @param promserver
     * @param urlPath
     * @return
     */
    private Map<String, String> getMetricsDatabByPath(Promserver promserver, String urlPath) {
        JSONObject jsonObject = requestAgent(promserver, urlPath);
        Map dataMap = (Map) jsonObject.get("data");
        List<Map> resultList = JSONObject.parseArray(JSONObject.toJSONString(dataMap.get("result")), Map.class);

        Map<String, String> map = new HashMap<>();
        for (Map pojo : resultList) {
            Map metricMap = (Map) pojo.get("metric");
            Object assethost = metricMap.get("host");
            if (assethost == null) continue;
            List<String> timeAndData = (List<String>) pojo.get("value");
            map.put(assethost.toString(), timeAndData.get(1));
        }
        return map;
    }


    /**
     * 请求并返回结果
     *
     * @param promServer
     * @param urlPath
     * @return
     */
    private JSONObject requestAgent(Promserver promServer, String urlPath) {
        URL url = null;
        try {
            url = new URL("http", promServer.getHost(), promServer.getPort(), urlPath);
        } catch (MalformedURLException e) {
            log.error(e);
        }
        String result = Tool.HttpUtil.createGet(url.toString()).auth(promServer.getToken()).execute().body();
        if (log.isDebugEnabled()) log.debug(String.format("请求 %s 接口，相应内容为 %s", url.toString(), result));
        return JSONObject.parseObject(result);
    }
}
