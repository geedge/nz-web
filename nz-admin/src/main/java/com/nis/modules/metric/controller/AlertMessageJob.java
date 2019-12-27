package com.nis.modules.metric.controller;

import com.google.gson.Gson;
import com.nis.common.utils.Constant;
import com.nis.modules.alert.dao.AlertMessageDao;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.service.SysDictService;
import org.apache.commons.collections.CollectionUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.*;

/**
 * alert message
 * 根据redis中的alertMessage 数据endtime和当前时间与alertrule表中持续时间比对
 * 失效则修改alert_message state为2 并删除redis中对应内容
 */
public class AlertMessageJob extends QuartzJobBean {

    private static Logger logger = LoggerFactory.getLogger(AlertMessageJob.class);

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private AlertRuleService alertRuleService;

    @Autowired
    private AlertMessageDao alertMessageDao;

    @Autowired
    private SysDictService sysDictService;

    private static Gson gson = new Gson();

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        // 1. 从 redis 中获取缓存信息
        HashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
        Map<String, String> cacheAlertMap = opsForHash.entries(Constant.REDIS_KEY_ALERTMESSAGE);
        Set<Map.Entry<String, String>> cacheAlertEntries = cacheAlertMap.entrySet();

        // 2. 获取所有 alert_rule 表数据
        Map<Integer, Integer> alertRuleIdAndLast = new HashMap<>();
        List<AlertRuleEntity> alertRules = alertRuleService.list();
        for (AlertRuleEntity ruleEntity : alertRules) {
            alertRuleIdAndLast.put(ruleEntity.getId(), ruleEntity.getLast());
        }

        Map<String, Double> cacheAlert = new HashMap<>();
        Integer cacheAlertId = null;
        Long cacheEndTime = null;

        // 当前时间
        long currentTimeMillis = 0;
        // 缓存hashmap 中的 key value
        String hashKey, value = null;

        // 默认 alertInterval 为15s
        Integer alertInterval = 15;
        Map<String, Object> map = new HashMap<>(2);
        map.put("type", Constant.DICT_ALERTINTERVAL_TYPE);

        List<SysDictEntity> sysDictEntities = sysDictService.queryList(map);
        if (CollectionUtils.isNotEmpty(sysDictEntities)) {
            alertInterval = Integer.valueOf(sysDictEntities.get(0).getValue());
        }

        // 循环比对
        List<String> updateCacheKeys = new ArrayList<>();

        for (Map.Entry<String, String> entry : cacheAlertEntries) {
            hashKey = entry.getKey();
            value = entry.getValue();

            // 缓存中的实体
            cacheAlert = gson.fromJson(value, Map.class);
            cacheAlertId = cacheAlert.get("id").intValue();
            cacheEndTime = cacheAlert.get("time").longValue();

            // 数据库存储持续时间为 秒
            Integer last = alertRuleIdAndLast.get(cacheAlertId);
            // 缓存中的 id 和数据库alert rule表中有数据对应
            if (last != null) {
                currentTimeMillis = System.currentTimeMillis();

                // 当前时间 - 结束时间 大于等于 持续时间 证明已经失效
                if (currentTimeMillis > cacheEndTime && (currentTimeMillis - cacheEndTime) / 1000 - last - alertInterval > 0) {
                    updateCacheKeys.add(hashKey);
                }
            } else {
                continue;
            }
        }

        // 3. 处理变化  1. 修改数据库alert_message 中的state 2. 更新缓存
        if (CollectionUtils.isNotEmpty(updateCacheKeys)) {
            alertMessageDao.updateBatch(updateCacheKeys);
        }

        for (String key : updateCacheKeys) {
            cacheAlertMap.remove(key);
        }

        redisTemplate.delete(Constant.REDIS_KEY_ALERTMESSAGE);
        opsForHash.putAll(Constant.REDIS_KEY_ALERTMESSAGE, cacheAlertMap);
    }
}
