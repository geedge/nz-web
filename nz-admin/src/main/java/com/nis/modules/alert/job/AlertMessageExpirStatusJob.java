package com.nis.modules.alert.job;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.DateUtil;
import com.nis.modules.alert.dao.AlertMessageDao;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.*;
import java.util.stream.Collectors;

/**
 * alert message
 * 根据redis中的alertMessage 数据endtime和当前时间与alertrule表中持续时间比对
 * 失效则修改alert_message state为2 并删除redis中对应内容*/


@Slf4j
@DisallowConcurrentExecution
public class AlertMessageExpirStatusJob extends QuartzJobBean {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private AlertRuleService alertRuleService;

    @Autowired
    private AlertMessageDao alertMessageDao;

    @Autowired
    private SysConfigService sysConfigService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.debug("告警消息过期状态任务开始");
        long start, end;
        start = System.currentTimeMillis();

        this.handleAlertStatus();

        end = System.currentTimeMillis();
        log.debug("告警消息过期状态任务 start time:" + start + "; end time:" + end + "; Run Time:" + (end - start) + "(ms)");
        log.debug("告警消息过期状态任务结束");
    }

    private void handleAlertStatus(){
        // 1. 从 redis 中获取缓存信息
        HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
        Map<String, String> cacheAlertMap = opsForHash.entries(Constant.REDIS_KEY_ALERTMESSAGE);
        Set<Map.Entry<String, String>> cacheAlertEntries = cacheAlertMap.entrySet();

        // 2. 获取所有 alert_rule 表数据
        List<AlertRuleEntity> alertRules = alertRuleService.list();
        Map<Integer, Integer> alertRuleIdAndLast = alertRules.stream().collect(Collectors.toMap(AlertRuleEntity::getId, AlertRuleEntity::getLast));

        // 3. 默认 alertInterval 为15s
        String[] keys = {Constant.SYSCONFIG_KEY_ALERTINTERVAL, Constant.SYSCONFIG_KEY_SCRAPEINTERVAL};
        List<SysConfigEntity> configs = sysConfigService.list(new QueryWrapper<SysConfigEntity>().lambda().in(SysConfigEntity::getParamKey, keys));

        Integer alertInterval = Integer.valueOf(configs.get(0).getParamValue());
        Integer scrapeInterval = Integer.valueOf(configs.get(1).getParamValue());

        Map<String, Object> cacheAlert = new HashMap<>();
        Integer cacheAlertId = null;
        Long cacheEndTime = null;

        // 缓存hashmap 中的 key value
        String hashKey, value = null;

        // 循环比对
        List<String> updateCacheKeys = new ArrayList<>();

        // rule 删除后遗留下来的 key,需要删除
        List<String> removeKeys = new ArrayList<>();

       // 当前时间
        long currentTimeMillis = 0;
        for (Map.Entry<String, String> entry : cacheAlertEntries) {
            hashKey = entry.getKey();
            value = entry.getValue();

            // 缓存中的实体
            cacheAlert = JSONObject.parseObject(value, Map.class);

            cacheAlertId = Integer.parseInt(String.valueOf(cacheAlert.get("id")));
            cacheEndTime = Long.valueOf(String.valueOf(cacheAlert.get("time")));

            // 数据库存储持续时间为 秒
            Integer last = alertRuleIdAndLast.get(cacheAlertId);
            // 缓存中的 id 和数据库alert rule表中有数据对应
            if (last != null) {
                currentTimeMillis = System.currentTimeMillis();

                // 当前时间 - 结束时间 大于等于 持续时间 证明已经失效
                if ((currentTimeMillis - cacheEndTime) / 1000 - (last > scrapeInterval ? last : scrapeInterval) - alertInterval > 0) {
                    updateCacheKeys.add(hashKey);
                }
            } else {
                // 通过 ruleId 获取为 null 整明该 rule 已经删除
                removeKeys.add(hashKey);
            }
        }

        // 3. 处理变化  1. 修改数据库alert_message 中的state 2. 更新缓存
        if (CollectionUtils.isNotEmpty(updateCacheKeys)) {
            log.debug("状态变化的hashkeys" + updateCacheKeys.toString());
            alertMessageDao.updateBatch(updateCacheKeys, DateUtil.getUTCTimeByConfigTimeZone());

            removeKeys.addAll(updateCacheKeys);
            String[] delKeys = new String[removeKeys.size()];
            removeKeys.toArray(delKeys);
            // 删除失效的key
            opsForHash.delete(Constant.REDIS_KEY_ALERTMESSAGE, delKeys);
        }
    }
}
