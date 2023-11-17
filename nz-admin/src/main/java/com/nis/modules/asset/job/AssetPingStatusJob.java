package com.nis.modules.asset.job;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.TimedCacheManager;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.PromApiService;
import com.nis.modules.asset.dao.AssetAssetDao;
import com.nis.modules.asset.dao.AssetPingDao;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetPing;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetPingService;
import com.nis.modules.election.dao.LeaderElectionDao;
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
import java.util.*;
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
    private AssetAssetDao assetDao;

    @Autowired
    private AssetAssetService assetAssetService;

    @Autowired
    private PromApiService promApiService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    	Thread.currentThread().setName("AssetPingStatusJob");
        // 检查 HA_LOCK
        LeaderElectionDao dao = LeaderElectionDao.getInstance();
        boolean isLeader = dao.isLeader(Constant.SYS_HA_LOCK, Constant.SERVER_ID);
        log.info("[AssetPingStatusJob] [Get HA_LOCK] [isLeader: {}]", isLeader);
        if (BooleanUtil.negate(isLeader)) return;

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

    @SuppressWarnings("unchecked")
	@Transactional(rollbackFor = Exception.class)
    public void execAssetPing() throws RuntimeException {
        Agent agent = promApiService.getOneAvaliableGlobalAgent();
        if (agent == null) {
            log.info("asset ping 状态更新为 DOWN,当前暂无可用 agent");
            this.allAssetPingStateDown();
            return;
        }

        // 查询 confagent_path asset_ping_interval
        List<SysConfigEntity> configList = sysConfigService.list(new LambdaQueryWrapper<SysConfigEntity>().in(SysConfigEntity::getParamKey, Constant.CONFAGENT_PATH, Constant.SYSCONFIG_KEY_ASSET_PING_INTERVAL));
        Map<String, String> configMap = configList.stream().collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));

        long currentTimeMillis = System.currentTimeMillis();
        // 单位 s
        String assetPingIntervalStr = configMap.get(Constant.SYSCONFIG_KEY_ASSET_PING_INTERVAL);
        // 减去 半个 asset ping interval 周期，保证数据的完整性
        long time = (currentTimeMillis - Long.valueOf(assetPingIntervalStr) * 1000 / 2) / 1000;

        Map<String, String> assetHostAndStateMap = this.getPingMetricsData("probe_success", time);
        Map<String, String> assetHostAndRttMap = this.getPingMetricsData("probe_icmp_duration_seconds{phase='rtt'}",time);

        // 如果当前未查询到数据，更新状态为 down
        if (ToolUtil.isEmpty(assetHostAndStateMap) || ToolUtil.isEmpty(assetHostAndRttMap)) {
            log.info("asset ping 状态更新为 DOWN,未查询到 ping 指标数据");
            this.allAssetPingStateDown();
            return;
        }

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
                upAssetPings.add(new AssetPing(null,assetId, host, 1,d,new Date()));
            } else {
                // down
                downAssetPings.add(new AssetPing(null,assetId, host, 0,null,null));
            }
        }
        
        List<AssetPing> list = assetPingService.list();
        upAssetPings.addAll(downAssetPings);
        // 如果本次请求缺失 asset ， 更新状态为 down
        for (AssetPing upAssetPing : upAssetPings) {
            Iterator<AssetPing> iterator = list.iterator();
            while (iterator.hasNext()) {
                AssetPing next = iterator.next();
                if (upAssetPing.getAssetId().equals(next.getAssetId())) {
                	upAssetPing.setId(next.getId());
                    iterator.remove();
                }
            }
        }
        if (Tool.CollectionUtil.isNotEmpty(list)) {
            for (AssetPing assetPing : list) {
                assetPing.setStatus(0);
            }
            upAssetPings.addAll(list);
        }

        // 未更新前 asset_ping 历史数据
        List<AssetPing> befUpdHistoryAssetPingDataList = assetPingService.list();
        Map<Integer, Integer> befUpdHistoryAssetPingAssetIdAndStatusMap = befUpdHistoryAssetPingDataList.stream().collect(Collectors.toMap(AssetPing::getAssetId, AssetPing::getStatus));

        //判断查询数据与缓存中变化的信息
        TimedCache<String, Object> cache = TimedCacheManager.getCache(Constant.SYS_STATE_CACHE_PREFIX, Constant.SYSCONFIG_KEY_ASSET_PING);
        if (BooleanUtil.negate(cache.isEmpty())) {
            for (int i = upAssetPings.size() - 1; i >= 0; i--) {
                AssetPing assetPing = upAssetPings.get(i);
                if (cache.containsKey(assetPing.getAssetId().toString())) {
                    AssetPing cachePing = JSON.parseObject((String) cache.get(assetPing.getAssetId().toString()), AssetPing.class);
                    cache.put(assetPing.getAssetId().toString(), JSON.toJSONString(assetPing));
                    // 如果状态相同则只更新缓存中的数据信息
                    if (cachePing.getStatus().equals(assetPing.getStatus())) {
                        // 增加数据库状态对比 避免数据库历史数据导致状态不一致没有更新的情况
                        Integer befUpdHistoryAssetPingStatus = befUpdHistoryAssetPingAssetIdAndStatusMap.get(cachePing.getAssetId());
                        if (befUpdHistoryAssetPingStatus != null && befUpdHistoryAssetPingStatus.equals(cachePing.getStatus())) {
                            upAssetPings.remove(i);
                        }
                    }
                } else {
                    // 数据库存在 缓存中不存在
                    cache.put(assetPing.getAssetId().toString(), JSON.toJSONString(assetPing));
                }
            }
        } else {
            for (AssetPing assetPing : upAssetPings) {
                cache.put(assetPing.getAssetId().toString(), JSON.toJSONString(assetPing));
            }
        }
        if(ToolUtil.isNotEmpty(upAssetPings)) {
        	assetPingService.saveOrUpdateBatch(upAssetPings);
        }
    }


    /**
     * 根据 url 请求带理接口并返回 asset host / state ，rtt 数据
     *
     * @param query
     * @param time
     * @return
     */
    private Map<String, String> getPingMetricsData(String query, long time) {
        JSONObject jsonObject = promApiService.query(query, String.valueOf(time), null);
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
     * 开启ping 的 asset 状态更新为 down
     */
    private void allAssetPingStateDown() {
        assetPingDao.deleteAll();

        // 开启 Ping Assets
        List<AssetAsset> openPingAssets = assetAssetService.getOpenPingAssets();

        List<AssetPing> assetPingList = new ArrayList<>();
        for (AssetAsset asset : openPingAssets) {
            assetPingList.add(new AssetPing(null,asset.getId(), asset.getManageIp(), 0,null,null));
        }

        if (Tool.CollectionUtil.isNotEmpty(assetPingList)) assetPingService.saveBatch(assetPingList);
    }
}
