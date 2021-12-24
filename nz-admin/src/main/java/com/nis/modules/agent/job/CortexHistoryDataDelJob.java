package com.nis.modules.agent.job;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.Constant;
import com.nis.common.utils.DateUtil;
import com.nis.common.utils.Tool;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@DisallowConcurrentExecution
public class CortexHistoryDataDelJob extends QuartzJobBean {

    private static Log log = Log.get();

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SysConfigService sysConfigService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Thread.currentThread().setName("CortexHistoryDataDelJob");
        // 检查 haMode
        String serverId = (String) redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
        if (!StrUtil.equals(serverId, Constant.SERVER_ID)) return;

        log.info("CortexHistoryDataDelJob 开始执行");
        StopWatch sw = new StopWatch();
        sw.start();
        try {
            // 定时清理 minio cortex 历史数据
            this.cortexHistoryDataDel();
        } catch (RuntimeException e) {
            log.error("CortexHistoryDataDelJob 更新异常", e);
        } finally {
            sw.stop();
        }
        log.info("CortexHistoryDataDelJob 执行结束，Run Time: {}", sw.toString());
    }

    private void cortexHistoryDataDel() {
        List<SysConfigEntity> sysConfigList = sysConfigService.list();
        Map<String, String> sysConfigMap = sysConfigList.stream().collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));

        // 1. 对象存储方式 s3 时执行
        // object storage 标识 1: 本地文件存储，2：S3对象存储
        String objStroageEnable = sysConfigMap.get(Constant.SYSCONFIG_KEY_METRICS_STORAGE_TYPE);
        objStroageEnable = StrUtil.emptyToDefault(objStroageEnable, "1");
        if (StrUtil.equals(objStroageEnable, "1")) return;

        // 2. 删除超时数据
        String minioEndpoint = String.format("http://%s/", sysConfigMap.get(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_ENDPOINT));
        String accessKey = sysConfigMap.get(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_ACCESS_KEY);
        String secretKey = sysConfigMap.get(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_SECRET_ACCESS_KEY);
        String bucketName = sysConfigMap.get(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_BUCKET);
        try {
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(minioEndpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (bucketExists) {
                log.info(String.format("bucket %s exists", bucketName));
                // 查询过期数据，然后删除
                String metricsStorageRetentionDay = sysConfigMap.get(Constant.SYSCONFIG_KEY_METRICS_STORAGE_RETENTION);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(new Date());
                calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - Integer.valueOf(metricsStorageRetentionDay));
                Date keepDate = calendar.getTime();
                log.info("Metrics data retention deadline: " + DateUtil.formatDate(keepDate, "yyyy-MM-dd HH:mm:ss"));

                // 查询 存储对象
                Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).recursive(true).build());
                List<Item> itemList = new ArrayList<>();
                for (Result<Item> result : results) {
                    itemList.add(result.get());
                }
                log.info("Metrics objects in bucket size: " + itemList.size());

                // 过滤
                itemList = itemList.stream().filter(item -> {
                    ZonedDateTime zonedDateTime = item.lastModified();
                    Date from = Date.from(zonedDateTime.toInstant());
                    return Tool.DateUtil.compare(keepDate, from) > 0;
                }).sorted(Comparator.comparing(Item::lastModified)).collect(Collectors.toList());

                log.info("Metrics objects to clean size:  " + itemList.size());

                // 删除
                List<String> removeObjNameList = itemList.stream().map(Item::objectName).collect(Collectors.toList());
                List<DeleteObject> removeObjList = new LinkedList<>();
                for (String name : removeObjNameList) {
                    removeObjList.add(new DeleteObject(name));
                }
                Iterable<Result<DeleteError>> errorResults = minioClient.removeObjects(RemoveObjectsArgs.builder().bucket(bucketName).objects(removeObjList).build());
                List<String> deleteErrorResults = new ArrayList<>();
                for (Result<DeleteError> result : errorResults) {
                    DeleteError deleteError = result.get();
                    deleteErrorResults.add(deleteError.toString());
                }
                log.info("Metrics objects clean error size: " + deleteErrorResults.size());
                if (Tool.CollectionUtil.isNotEmpty(deleteErrorResults)) {
                    log.error("Metrics objects clean error info: {}", JSONObject.toJSONString(deleteErrorResults));
                }
            } else {
                log.info(String.format("bucket %s does not exist", bucketName));
                log.info("CortexHistoryDataDelJob Done");
            }
        } catch (Exception e) {
            log.error(e);
        }
    }
}
