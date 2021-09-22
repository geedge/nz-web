package com.nis.modules.asset.job;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.Constant;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
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

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private PromserverService promService;
    
    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    	// 检查 haMode
    	String serverId = (String) redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
        if(!StrUtil.equals(serverId, Constant.SERVER_ID)) return;

        StopWatch sw = new StopWatch();
        sw.start();
        try {
            // 获取 ping 数据
            this.execAssetPing();
        } catch (RuntimeException e) {
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

        // 查询 confagent_path asset_ping_interval
        List<SysConfigEntity> configList = sysConfigService.list(new LambdaQueryWrapper<SysConfigEntity>().in(SysConfigEntity::getParamKey, Constant.CONFAGENT_PATH, Constant.SYSCONFIG_KEY_ASSET_PING_INTERVAL));
        Map<String, String> configMap = configList.stream().collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));

        String confagentPath = configMap.get(Constant.CONFAGENT_PATH);
        long currentTimeMillis = System.currentTimeMillis();
        // 单位 s
        String assetPingIntervalStr = configMap.get(Constant.SYSCONFIG_KEY_ASSET_PING_INTERVAL);
        // 减去 半个 asset ping interval 周期，保证数据的完整性
        long time = (currentTimeMillis - Long.valueOf(assetPingIntervalStr) * 1000 / 2) / 1000;
        String promPathPrefix = promService.getPromPathPrefix();
        String pingStateUrlPath = promPathPrefix + "/api/v1/query?query=probe_success&time=" + time;
        String rttUrlPath = promPathPrefix + "/api/v1/query?query=probe_icmp_duration_seconds{phase='rtt'}&time=" + time;

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
                upAssetPings.add(new AssetPing(assetId, host, 1, d, new Date()));
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
        // 如果本次请求缺失 asset ， 保存上次内容，不更新 时间
        for (AssetPing upAssetPing : upAssetPings) {
            Iterator<AssetPing> iterator = list.iterator();
            while (iterator.hasNext()) {
                AssetPing next = iterator.next();
                if (upAssetPing.getAssetId().equals(next.getAssetId())) {
                    iterator.remove();
                }
            }
        }
        if (Tool.CollectionUtil.isNotEmpty(list)) upAssetPings.addAll(list);

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
        JSONObject jsonObject = promserverService.requestAgent(promserver, urlPath);
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
}
