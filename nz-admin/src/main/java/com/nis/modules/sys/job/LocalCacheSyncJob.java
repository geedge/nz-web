package com.nis.modules.sys.job;

import cn.hutool.cache.impl.CacheObj;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.JSONUtil;
import com.nis.common.utils.LocalCacheManager;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.backend.AlertContext;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.entity.AlertRuleEvalLog;
import com.nis.modules.alert.entity.RecordRuleEntity;
import com.nis.modules.alert.entity.RecordRuleEvalLog;
import com.nis.modules.alert.service.AlertRuleEvalLogService;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.alert.service.RecordRuleEvalLogService;
import com.nis.modules.alert.service.RecordRuleService;
import com.nis.modules.election.dao.LeaderElectionDao;
import com.nis.modules.sys.entity.SysComponent;
import com.nis.modules.sys.service.SysComponentService;
import com.nis.modules.sys.service.SysService;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.Iterator;
import java.util.List;
import java.util.Map;


@DisallowConcurrentExecution
public class LocalCacheSyncJob extends QuartzJobBean {

    private static final Log log = Log.get();

    @Autowired
    private SysService sysService;

    @Autowired
    private AlertRuleService alertRuleService;

    @Autowired
    private RecordRuleService recordRuleService;

    @Autowired
    private SysComponentService sysComponentService;

    @Autowired
    private AlertRuleEvalLogService alertRuleEvalLogService;

    @Autowired
    private RecordRuleEvalLogService recordRuleEvalLogService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Thread.currentThread().setName("LocalCacheSyncJob");

        LeaderElectionDao leaderElectionDao = LeaderElectionDao.getInstance();
        boolean isLeader = leaderElectionDao.isLeader(Constant.SYS_HA_LOCK, Constant.SERVER_ID);
        log.info("[LocalCacheSyncJob] [Get HA_LOCK] [isLeader: {}]", isLeader);
        if (Tool.BooleanUtil.negate(isLeader)) return;

        // master serverId
        String masterServerId = leaderElectionDao.getLeader(Constant.SYS_HA_LOCK);

        log.info("[LocalCacheSyncJob] [begin]");
        StopWatch sw = new StopWatch();
        sw.start();
        try {
            this.syncLocalCacheToSlave(masterServerId);
        } catch (Exception e) {
            log.error(e, "[LocalCacheSyncJob] [error]");
        } finally {
            sw.stop();
        }

        log.info("[LocalCacheSyncJob] [finshed] [Run Time: {}]", sw.toString());
    }

    /**
     * syncLocalCacheToSlave
     *
     * @param masterServerId
     */
    private void syncLocalCacheToSlave(String masterServerId) {
        List<SysComponent> slaveWebComponentList = sysComponentService.list(new LambdaQueryWrapper<SysComponent>()
                .eq(SysComponent::getComponent, Constant.NZ_WEB_COMPONENT_NAME)
                .ne(SysComponent::getServerid, masterServerId));
        log.info("[syncLocalCacheToSlave] [slaveWebComponentList] [size: {}]", Tool.CollUtil.size(slaveWebComponentList));

        for (SysComponent sysComponent : slaveWebComponentList) {
            // sync_cache_alert_rule_eval_log
            Thread.ofVirtual().start(() -> {
                log.info("[syncLocalCacheToSlave] [sync_cache_alert_rule_eval_log] [begin]");
                List<AlertRuleEntity> allRuleList = alertRuleService.list(new LambdaQueryWrapper<AlertRuleEntity>().in(AlertRuleEntity::getType, Tool.ListUtil.of(1, 2)));

                Map<String, List> notifyDataMap = Tool.MapUtil.newHashMap();
                for (AlertRuleEntity rule : allRuleList) {
                    List<AlertRuleEvalLog> alertRuleEvalLogs = alertRuleEvalLogService.queryListByRuleId(rule.getId());
                    if (Tool.CollUtil.isNotEmpty(alertRuleEvalLogs)) {
                        notifyDataMap.put(rule.getId().toString(), alertRuleEvalLogs);
                    }
                }
                sysService.notifySysMessageByComponent(sysComponent, Constant.SysTopicNameEnum.SYNC_CACHE_ALERT_RULE_EVAL_LOG.getName(), JSONUtil.toJsonStr(notifyDataMap));
            });

            // sync_cache_record_rule_eval_log
            Thread.ofVirtual().start(() -> {
                log.info("[syncLocalCacheToSlave] [sync_cache_record_rule_eval_log] [begin]");
                List<RecordRuleEntity> allRuleList = recordRuleService.list(new LambdaQueryWrapper<RecordRuleEntity>().eq(RecordRuleEntity::getState, 1));

                Map<String, List> notifyDataMap = Tool.MapUtil.newHashMap();
                for (RecordRuleEntity rule : allRuleList) {
                    List<RecordRuleEvalLog> recordRuleEvalLogs = recordRuleEvalLogService.queryListByRuleId(rule.getId());
                    if (Tool.CollUtil.isNotEmpty(recordRuleEvalLogs)) {
                        notifyDataMap.put(rule.getId().toString(), recordRuleEvalLogs);
                    }
                }
                sysService.notifySysMessageByComponent(sysComponent, Constant.SysTopicNameEnum.SYNC_CACHE_RECORD_RULE_EVAL_LOG.getName(), JSONUtil.toJsonStr(notifyDataMap));
            });

            // sync_cache_alert_for_state
            Thread.ofVirtual().start(() -> {
                log.info("[syncLocalCacheToSlave] [sync_cache_alert_for_state] [begin]");
                List<AlertRuleEntity> allRuleList = alertRuleService.list(new LambdaQueryWrapper<AlertRuleEntity>().eq(AlertRuleEntity::getState, 1).in(AlertRuleEntity::getType, Tool.ListUtil.of(1, 2)));

                Map<String, Map<String, String>> notifyDataMap = Tool.MapUtil.newHashMap();
                for (AlertRuleEntity rule : allRuleList) {
                    String ruleCacheKey = AlertContext.getCacheKey(rule);
                    TimedCache<String, Object> cache = LocalCacheManager.getTimedCache(Constant.ALERT_CACHE_PREFIX, ruleCacheKey);
                    Iterator<CacheObj<String, Object>> iterator = cache.cacheObjIterator();

                    Map<String, String> ruleStateMap = Tool.MapUtil.newHashMap();
                    while (iterator.hasNext()) {
                        CacheObj<String, Object> next = iterator.next();
                        String metricStr = next.getKey();
                        String timestampStr = Tool.StrUtil.toString(next.getValue());
                        ruleStateMap.put(metricStr, timestampStr);
                    }

                    if (Tool.MapUtil.isNotEmpty(ruleStateMap)) {
                        notifyDataMap.put(rule.getId().toString(), ruleStateMap);
                    }
                }
                sysService.notifySysMessageByComponent(sysComponent, Constant.SysTopicNameEnum.SYNC_CACHE_ALERT_FOR_STATE.getName(), JSONUtil.toJsonStr(notifyDataMap));
            });

            // sync_cache_asset_ping
            Thread.ofVirtual().start(() -> {
                log.info("[syncLocalCacheToSlave] [sync_cache_asset_ping] [begin]");

                Map<String, Map<String, String>> notifyDataMap = Tool.MapUtil.newHashMap();

                Map<String, String> mgtPingData = Tool.MapUtil.newHashMap();
                TimedCache<String, Object> mgt = LocalCacheManager.getTimedCache(Constant.SYS_STATE_CACHE_PREFIX, Constant.SYSCONFIG_KEY_ASSET_MGT_PING_STATE);
                Iterator<CacheObj<String, Object>> mgtIterator = mgt.cacheObjIterator();
                while (mgtIterator.hasNext()) {
                    CacheObj<String, Object> next = mgtIterator.next();
                    String assetId = next.getKey();
                    String assetPingJsonStr = Tool.StrUtil.toString(next.getValue());
                    mgtPingData.put(assetId, assetPingJsonStr);
                }

                Map<String, String> ipmiPingData = Tool.MapUtil.newHashMap();
                TimedCache<String, Object> ipmi = LocalCacheManager.getTimedCache(Constant.SYS_STATE_CACHE_PREFIX, Constant.SYSCONFIG_KEY_ASSET_IPMI_PING_STATE);
                Iterator<CacheObj<String, Object>> ipmiIterator = ipmi.cacheObjIterator();
                while (ipmiIterator.hasNext()) {
                    CacheObj<String, Object> next = ipmiIterator.next();
                    String assetId = next.getKey();
                    String assetPingJsonStr = Tool.StrUtil.toString(next.getValue());
                    ipmiPingData.put(assetId, assetPingJsonStr);
                }

                notifyDataMap.put(Constant.AssetPingType.MGT.getType(), mgtPingData);
                notifyDataMap.put(Constant.AssetPingType.IPMI.getType(), ipmiPingData);
                sysService.notifySysMessageByComponent(sysComponent, Constant.SysTopicNameEnum.SYNC_CACHE_ASSET_PING.getName(), JSONUtil.toJsonStr(notifyDataMap));
            });

            // sync_cache_agent_state
            Thread.ofVirtual().start(() -> {
                log.info("[syncLocalCacheToSlave] [sync_cache_agent_state] [begin]");
                TimedCache<String, Object> cache = LocalCacheManager.getTimedCache(Constant.SYS_STATE_CACHE_PREFIX, Constant.AGENT_STATE_CACHE_KEY);

                Map<String, String> notifyDataMap = Tool.MapUtil.newHashMap();
                Iterator<CacheObj<String, Object>> iterator = cache.cacheObjIterator();
                while (iterator.hasNext()) {
                    CacheObj<String, Object> next = iterator.next();
                    String agentId = next.getKey();
                    String agentStateInfoJsonStr = Tool.StrUtil.toString(next.getValue());
                    notifyDataMap.put(agentId, agentStateInfoJsonStr);
                }
                sysService.notifySysMessageByComponent(sysComponent, Constant.SysTopicNameEnum.SYNC_CACHE_AGENT_STATE.getName(), JSONUtil.toJsonStr(notifyDataMap));
            });

            // sync_cache_system_login
            Thread.ofVirtual().start(() -> {
                log.info("[syncLocalCacheToSlave] [sync_cache_system_login] [begin]");
                TimedCache<String, Object> cache = LocalCacheManager.getTimedCache(Constant.SYS_CACHE_PREFIX, Constant.SYS_LOGIN_LOCKOUT_CACHE_KEY);

                Map<String, Map<String, String>> notifyDataMap = Tool.MapUtil.newHashMap();
                Iterator<CacheObj<String, Object>> iterator = cache.cacheObjIterator();
                while (iterator.hasNext()) {
                    CacheObj<String, Object> next = iterator.next();
                    String key = next.getKey();
                    Map<String, String> map = Tool.MapUtil.newHashMap();
                    map.put("retry", Tool.StrUtil.toString(next.getValue()));
                    map.put("ttl", Tool.StrUtil.toString(next.getTtl()));
                    map.put("lastAccess", Tool.StrUtil.toString(next.getLastAccess()));
                    notifyDataMap.put(key, map);
                }
                sysService.notifySysMessageByComponent(sysComponent, Constant.SysTopicNameEnum.SYNC_CACHE_SYSTEM_LOGIN.getName(), JSONUtil.toJsonStr(notifyDataMap));
            });
        }
    }
}
