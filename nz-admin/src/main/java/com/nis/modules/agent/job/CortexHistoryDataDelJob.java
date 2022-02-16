package com.nis.modules.agent.job;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.lifecycle.LifecycleFilter;
import com.amazonaws.services.s3.model.lifecycle.LifecyclePrefixPredicate;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@DisallowConcurrentExecution
public class CortexHistoryDataDelJob extends QuartzJobBean {

    static {
        System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, "true");
    }

    private static Log log = Log.get();

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SysConfigService sysConfigService;

    static private boolean execForFirstTimeFlag = true;

    // agent util cortex 改变时，向此 map put data ,用以判断 cortex event log 是否改变
    static public Map<String, Object> CORTEX_EVALLOG_CHANGE_MAP = Tool.MapUtil.newConcurrentHashMap();

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

        // 第一次定时任务执行时间执行生命周期设置，之后根据 cortex 是否改动进行执行
        if (!Tool.CollectionUtil.isNotEmpty(CORTEX_EVALLOG_CHANGE_MAP) && !execForFirstTimeFlag) return;

        log.info("Cortex data bucket setup lifecycle");
        // 2. 设置存储桶生命周期
        String connectionScheme = sysConfigMap.get(Constant.CORTEX_MONITOR_SCHEME);
        connectionScheme = StrUtil.isEmpty(connectionScheme) ? "http" : connectionScheme;

        String minioEndpoint = sysConfigMap.get(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_ENDPOINT);
        String accessKey = sysConfigMap.get(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_ACCESS_KEY);
        String secretKey = sysConfigMap.get(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_SECRET_ACCESS_KEY);
        String bucketName = sysConfigMap.get(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_BUCKET);
        String regionStr = sysConfigMap.get(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_REGION);

        try {
            AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
            ClientConfiguration clientConfig = new ClientConfiguration();
            clientConfig.setProtocol(Protocol.valueOf(connectionScheme.toUpperCase()));

            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .withClientConfiguration(clientConfig)
                    .withEndpointConfiguration(
                            new AwsClientBuilder.EndpointConfiguration(minioEndpoint, StrUtil.isEmpty(regionStr) ? null : regionStr)
                    )
                    .build();

            boolean bucketExists = s3Client.doesBucketExistV2(bucketName);
            if (bucketExists) {
                log.info(String.format("bucket %s exists", bucketName));
                // 查询过期数据，设置存储桶生命周期
                String metricsStorageRetentionDay = sysConfigMap.get(Constant.SYSCONFIG_KEY_METRICS_STORAGE_RETENTION);

                // Create a rule to archive objects with the "glacierobjects/" prefix to Glacier immediately.
                BucketLifecycleConfiguration.Rule rule1 = new BucketLifecycleConfiguration.Rule()
                        .withId("cortexExpirationRule")
                        .withFilter(new LifecycleFilter(new LifecyclePrefixPredicate("fake/")))
                        .withExpirationInDays(Integer.valueOf(metricsStorageRetentionDay))
                        .withStatus(BucketLifecycleConfiguration.ENABLED);

                // Add the rules to a new BucketLifecycleConfiguration.
                BucketLifecycleConfiguration configuration = new BucketLifecycleConfiguration().withRules(Arrays.asList(rule1));
                // Save the configuration.
                s3Client.setBucketLifecycleConfiguration(bucketName, configuration);

                // 获取配置
                // Retrieve the configuration.
                // Verify that the configuration now has one rules. prefix: fake/ expirationInDays: sysconfig.metricsStorageRetentionDay
                BucketLifecycleConfiguration bucketLifecycleConfiguration = s3Client.getBucketLifecycleConfiguration(bucketName);
                List<BucketLifecycleConfiguration.Rule> rules1 = bucketLifecycleConfiguration.getRules();
                for (BucketLifecycleConfiguration.Rule l : rules1) {
                    LifecyclePrefixPredicate predicate = (LifecyclePrefixPredicate) l.getFilter().getPredicate();
                    String prefix = predicate.getPrefix();
                    log.info(String.format("Lifecycle status is %s , Lifecycle prefix is %s , Lifecycle expiration days is %s", l.getStatus(), prefix, l.getExpirationInDays()));
                }

                // 标识重置
                execForFirstTimeFlag = false;
                CORTEX_EVALLOG_CHANGE_MAP.clear();
            } else {
                log.info(String.format("bucket %s does not exist", bucketName));
                log.info("CortexHistoryDataDelJob Done");
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

}
