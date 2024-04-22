package com.nis.modules.asset.job;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.LocalCacheManager;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.agent.service.PromApiService;
import com.nis.modules.asset.dao.AssetPingDao;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetPing;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetPingService;
import com.nis.modules.election.dao.LeaderElectionDao;
import com.nis.modules.sys.service.SysConfService;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@DisallowConcurrentExecution
public class AssetPingStatusJob extends QuartzJobBean {

    private static final Log log = Log.get();

    @Autowired
    private AssetPingDao assetPingDao;

    @Autowired
    private AssetPingService assetPingService;

    @Autowired
    private SysConfService sysConfService;

    @Autowired
    private AssetAssetService assetService;

    @Autowired
    private PromApiService promApiService;

    // 计数器，每 forceSync 个周期 执行一次强制执行
    private volatile static Integer counter = 1;

    @Value("${asset.ping.check.force.sync:10}")
    private Integer forceSync;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    	Thread.currentThread().setName("AssetPingStatusJob");
        // 检查 HA_LOCK
        LeaderElectionDao dao = LeaderElectionDao.getInstance();
        boolean isLeader = dao.isLeader(Constant.SYS_HA_LOCK, Constant.SERVER_ID);
        log.info("[AssetPingStatusJob] [Get HA_LOCK] [isLeader: {}]", isLeader);
        if (BooleanUtil.negate(isLeader)) return;

        log.info("[AssetPingStatusJob] [begin]");

        StopWatch sw = new StopWatch();
        sw.start();
        try {
            // update asset ping status
            this.updateAssetPingStatus(Constant.AssetPingType.MGT.getType());
            this.updateAssetPingStatus(Constant.AssetPingType.IPMI.getType());

            // counter
            if (counter % forceSync == 0) {
                counter = 1;
            } else {
                counter++;
            }
        } catch (Exception e) {
            log.error(e, "[AssetPingStatusJob] [error]");
        } finally {
            sw.stop();
        }

        log.info("[AssetPingStatusJob] [finshed] [Run Time: {}]", sw.toString());
    }

    /**
     * update asset ping status
     *
     * @throws RuntimeException
     */
    @SuppressWarnings("unchecked")
    @Transactional(rollbackFor = Exception.class)
    public void updateAssetPingStatus(String type) {
        String assetPingIntervalStr = sysConfService.getValue(Constant.SYSCONFIG_KEY_ASSET_PING_INTERVAL);
        // 减去 半个 asset ping interval 周期，保证数据的完整性
        long time = (System.currentTimeMillis() - Long.valueOf(assetPingIntervalStr) * 1000 / 2) / 1000;

        Map<String, String> assetHostAndStatusMap = this.getPingMetricsData(String.format("probe_success{module='ping',source_ip!='',type='%s'}", type), time);
        Map<String, String> assetHostAndRttMap = this.getPingMetricsData(String.format("probe_icmp_duration_seconds{module='ping',source_ip!='',phase='rtt',type='%s'}", type), time);

        // 如果当前未查询到数据，更新状态为 down
        if (ToolUtil.isEmpty(assetHostAndStatusMap) || ToolUtil.isEmpty(assetHostAndRttMap)) {
            log.warn("[updateAssetPingStatus] [no ping metrics data found, all asset ping status are down] [type: {}]", type);
            this.setAllPingStatusDown(type);
            return;
        }

        Map<String, Integer> assetHostAndIdMap = Tool.MapUtil.newHashMap();
        if (StrUtil.equals(Constant.AssetPingType.MGT.getType(), type)) {
            List<AssetAsset> assetList = assetService.list();
            assetHostAndIdMap = assetList.stream().collect(Collectors.toMap(AssetAsset::getManageIp, AssetAsset::getId));
        } else if (StrUtil.equals(Constant.AssetPingType.IPMI.getType(), type)) {
            List<AssetAsset> assetList = assetService.list(new LambdaQueryWrapper<AssetAsset>().ne(AssetAsset::getIpmiIp, ""));
            assetHostAndIdMap = assetList.stream().collect(Collectors.toMap(AssetAsset::getIpmiIp, AssetAsset::getId));
        }

        List<AssetPing> upPingList = new ArrayList<>();
        List<AssetPing> downPingList = new ArrayList<>();
        for (Map.Entry<String, String> entry : assetHostAndStatusMap.entrySet()) {
            String host = entry.getKey();
            Integer assetId = assetHostAndIdMap.get(host);
            if (assetId == null) continue;

            String state = entry.getValue();
            if ("1".equals(state)) {
                // 秒转毫秒 ， 四舍五入 保留两位小数
                BigDecimal bd = BigDecimal.valueOf(Double.valueOf(assetHostAndRttMap.get(host)));
                bd = bd.multiply(BigDecimal.valueOf(1000));
                Double d = bd.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                upPingList.add(new AssetPing(null, assetId, host, 1, type, d, new Date()));
            } else {
                downPingList.add(new AssetPing(null, assetId, host, 0, type, null, null));
            }
        }

        // update ping status list
        List<AssetPing> updatePingStatusList = Stream.of(upPingList, downPingList).flatMap(List::stream).collect(Collectors.toList());
        List<Integer> updateAssetIdList = updatePingStatusList.stream().map(AssetPing::getAssetId).distinct().collect(Collectors.toList());

        // current db ping status list
        List<AssetPing> currentPingStatusInDb = assetPingService.list(new LambdaQueryWrapper<AssetPing>().eq(AssetPing::getType, type));

        // filter & distinct
        Map<Integer, AssetPing> tempDistinctPingStatusInDbMap = currentPingStatusInDb.stream()
                // 获取本次请求指标未得到的 ping，设置为 down
                .filter(assetPing -> !Tool.CollUtil.contains(updateAssetIdList, assetPing.getAssetId()))
                .collect(Collectors.toMap(AssetPing::getAssetId, entity -> entity,
                        (entity1, entity2) -> entity1));// 重复就返回第一个
        List<AssetPing> tempAddDownPingStatusList = tempDistinctPingStatusInDbMap.values().stream().collect(Collectors.toList());
        for (AssetPing ping : tempAddDownPingStatusList) {
            ping.setStatus(0);
            updatePingStatusList.add(ping);
        }

        // 是否强制同步标识
        Boolean isForceSync = counter % forceSync == 0;
        log.info("[updateAssetPingStatus] [schedule for force sync db] [counter: {}] [forceSync: {}] [isForceSync: {}] [type: {}]", counter, forceSync, isForceSync, type);

        // 强制更新 db
        if (isForceSync) {
            log.info("[updateAssetPingStatus] [force sync of db asset_ping status] [size: {}] [type: {}]", Tool.CollUtil.size(updatePingStatusList), type);
            this.forceSyncDbPingStatus(updatePingStatusList, type);
        }
        // 未更新前 asset_ping 历史数据
        Map<Integer, Integer> befUpdHistoryPingMap = currentPingStatusInDb.stream()
                .collect(Collectors.toMap(AssetPing::getAssetId, AssetPing::getStatus,
                        (entity1, entity2) -> entity1)); // 重复就返回第一个

        TimedCache<String, Object> cache = null;
        if (StrUtil.equals(Constant.AssetPingType.MGT.getType(), type)) {
            cache = LocalCacheManager.getTimedCache(Constant.SYS_STATE_CACHE_PREFIX, Constant.SYSCONFIG_KEY_ASSET_MGT_PING_STATE);
        } else if (StrUtil.equals(Constant.AssetPingType.IPMI.getType(), type)) {
            cache = LocalCacheManager.getTimedCache(Constant.SYS_STATE_CACHE_PREFIX, Constant.SYSCONFIG_KEY_ASSET_IPMI_PING_STATE);
        }
        // 更新缓存
        if (cache != null && BooleanUtil.negate(cache.isEmpty())) {
            for (int i = updatePingStatusList.size() - 1; i >= 0; i--) {
                AssetPing assetPing = updatePingStatusList.get(i);
                if (cache.containsKey(assetPing.getAssetId().toString())) {
                    AssetPing cachePing = JSON.parseObject((String) cache.get(assetPing.getAssetId().toString()), AssetPing.class);
                    cache.put(assetPing.getAssetId().toString(), JSON.toJSONString(assetPing));
                    // 如果状态相同则只更新缓存中的数据信息
                    if (cachePing.getStatus().equals(assetPing.getStatus())) {
                        // 增加数据库状态对比 避免数据库历史数据导致状态不一致没有更新的情况
                        Integer befUpdHistoryAssetPingStatus = befUpdHistoryPingMap.get(cachePing.getAssetId());
                        if (befUpdHistoryAssetPingStatus != null && befUpdHistoryAssetPingStatus.equals(cachePing.getStatus())) {
                            updatePingStatusList.remove(i);
                        }
                    }
                } else {
                    // 数据库存在 缓存中不存在
                    cache.put(assetPing.getAssetId().toString(), JSON.toJSONString(assetPing));
                }
            }
        } else {
            for (AssetPing assetPing : updatePingStatusList) {
                cache.put(assetPing.getAssetId().toString(), JSON.toJSONString(assetPing));
            }
        }
        // 非强制同步数据库周期
        if (!isForceSync) {
            log.info("[updateAssetPingStatus] [not is schedule for force sync db] [update status change ping in db] [size: {}] [type: {}]", Tool.CollUtil.size(updatePingStatusList), type);
            // 存在状态变化的 ping 就更新 db
            this.updateStatusChangePingInDb(updatePingStatusList, type);
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
     * set all ping status down
     */
    private void setAllPingStatusDown(String type) {
        // delete
        assetPingDao.delete(new LambdaQueryWrapper<AssetPing>().eq(AssetPing::getType, type));

        List<AssetAsset> openPingAssets = assetService.getOpenPingAssets();

        List<AssetPing> assetPingList = new ArrayList<>();
        if (StrUtil.equals(Constant.AssetPingType.MGT.getType(), type)) {
            for (AssetAsset asset : openPingAssets) {
                assetPingList.add(new AssetPing(null, asset.getId(), asset.getManageIp(), 0, type, null, null));
            }
        } else if (StrUtil.equals(Constant.AssetPingType.IPMI.getType(), type)) {
            openPingAssets = openPingAssets.stream().filter(asset -> StrUtil.isNotEmpty(asset.getIpmiIp())).collect(Collectors.toList());
            for (AssetAsset asset : openPingAssets) {
                assetPingList.add(new AssetPing(null, asset.getId(), asset.getIpmiIp(), 0, type, null, null));
            }
        }

        // insert
        if (Tool.CollectionUtil.isNotEmpty(assetPingList)) {
            assetPingService.saveBatch(assetPingList);
        }
    }

    /**
     * force sync db ping status
     *
     * @param list
     * @param type
     */
    private void forceSyncDbPingStatus(List<AssetPing> list, String type) {
        // delete
        assetPingDao.delete(new LambdaQueryWrapper<AssetPing>().eq(AssetPing::getType, type));

        // insert
        if (Tool.CollUtil.isNotEmpty(list)) {
            assetPingService.saveBatch(list);
        }
    }

    /**
     * update status change ping in db
     *
     * @param list
     * @param type
     */
    private void updateStatusChangePingInDb(List<AssetPing> list, String type) {
        if (ToolUtil.isNotEmpty(list)) {
            // delete
            List<Integer> updatePingAssetIdList = list.stream().map(AssetPing::getAssetId).distinct().collect(Collectors.toList());
            assetPingService.remove(new LambdaQueryWrapper<AssetPing>().in(AssetPing::getAssetId, updatePingAssetIdList).eq(AssetPing::getType, type));
            // insert
            assetPingService.saveBatch(list);
        }
    }
}
