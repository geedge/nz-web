package com.nis.modules.agent.job;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONPath;
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
import com.nis.common.utils.JSONUtil;
import com.nis.common.utils.Tool;
import com.nis.modules.election.dao.LeaderElectionDao;
import com.nis.modules.sys.service.SysConfService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.Arrays;
import java.util.List;

@DisallowConcurrentExecution
public class CortexHistoryDataDelJob extends QuartzJobBean {

    static {
        System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, "true");
    }

    private static Log log = Log.get();

    @Autowired
    private SysConfService sysConfService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Thread.currentThread().setName("CortexHistoryDataDelJob");

        // 检查 HA_LOCK
        LeaderElectionDao dao = LeaderElectionDao.getInstance();
        boolean isLeader = dao.isLeader(Constant.SYS_HA_LOCK, Constant.SERVER_ID);
        log.info("[CortexHistoryDataDelJob] [Get HA_LOCK] [isLeader: {}]", isLeader);
        if (BooleanUtil.negate(isLeader)) return;

        log.info("cortex history data delete job start...");

        StopWatch sw = StopWatch.create("cortex history data delete job");
        sw.start();
        try {
            // 定时清理 minio cortex 历史数据
            this.cortexHistoryDataDel();
        } catch (Exception e) {
            log.error(e, "cortex history data delete job error");
        } finally {
            sw.stop();
        }
        log.info("cortex history data delete job Run Time: {}", sw.prettyPrint());
    }

    /**
     * buidl buildAmazonS3Client
     *
     * @return
     */
    private AmazonS3 buildAmazonS3Client() {
        log.info("build Amazon S3 Client begin...");
        String connectionScheme = sysConfService.getValue(Constant.CORTEX_MONITOR_SCHEME);
        connectionScheme = StrUtil.isEmpty(connectionScheme) ? "http" : connectionScheme;

        String minioEndpoint = sysConfService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_ENDPOINT);
        String accessKey = sysConfService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_ACCESS_KEY);
        String secretKey = sysConfService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_SECRET_ACCESS_KEY);
        String regionStr = sysConfService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_REGION);

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
        log.info("Cortex S3 Endpoint info: {}", JSONPath.read(JSONUtil.toJsonStr(s3Client), "endpoint"));
        log.info("build Amazon S3 Client finshed.");
        return s3Client;
    }

    /**
     * 通过对 minio bucket 设置生命周期实现清理过期数据
     */
    private void cortexHistoryDataDel() {
        // object storage 标识 1: 本地文件存储，2：S3对象存储
        String objStroageEnable = sysConfService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_TYPE);
        objStroageEnable = StrUtil.emptyToDefault(objStroageEnable, "1");
        if (StrUtil.equals(objStroageEnable, "1")) {
            log.warn("cortex history data delete job is not executed, current system metrics_storage_type config: {}", objStroageEnable);
            return;
        }

        try {
            log.info("Cortex data bucket config lifecycle begin...");
            AmazonS3 s3Client = this.buildAmazonS3Client();

            String bucketName = sysConfService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_BUCKET);
            if (s3Client.doesBucketExistV2(bucketName)) {
                log.info(String.format("bucket %s exists", bucketName));

                // 指标存储天数，默认 30 天，单位天
                String retentionDayStr = sysConfService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_RETENTION);
                Integer retentionDay = Integer.valueOf(StrUtil.emptyToDefault(retentionDayStr, "30"));

                String ruleId = "cortexExpirationRule";
                String lifecyclePrefix = "fake/";
                // 获取配置
                // Retrieve the configuration.
                // Verify that the configuration now has one rules. prefix: fake/ expirationInDays: sysconfig.metricsStorageRetentionDay
                BucketLifecycleConfiguration bucketLifecycleConfiguration = s3Client.getBucketLifecycleConfiguration(bucketName);
                if (ObjectUtil.isNull(bucketLifecycleConfiguration)) {
                    log.info("bucket has empty lifecycle configuration, name: {}", bucketName);
                    this.setBucketLifecycleConfiguration(s3Client, bucketName, ruleId, lifecyclePrefix, retentionDay);
                } else {
                    List<BucketLifecycleConfiguration.Rule> bucketLifecycleConfigurationRules = bucketLifecycleConfiguration.getRules();
                    bucketLifecycleConfigurationRules = Tool.CollUtil.defaultIfEmpty(bucketLifecycleConfigurationRules, Tool.ListUtil.empty());
                    BucketLifecycleConfiguration.Rule cortexExpirationRule = bucketLifecycleConfigurationRules.stream().filter(rule -> StrUtil.equals(rule.getId(), ruleId)).findFirst().orElse(null);
                    if (ObjectUtil.isNull(cortexExpirationRule)) {
                        log.info("bucket has empty lifecycle configuration, name: {}", bucketName);
                        this.setBucketLifecycleConfiguration(s3Client, bucketName, ruleId, lifecyclePrefix, retentionDay);
                    } else {
                        // 当前存在过期策略，判断规则是否发生变化
                        LifecyclePrefixPredicate predicate = (LifecyclePrefixPredicate) cortexExpirationRule.getFilter().getPredicate();
                        boolean isUpdate = BooleanUtil.or(
                                ObjectUtil.notEqual(BucketLifecycleConfiguration.ENABLED, cortexExpirationRule.getStatus()),
                                ObjectUtil.notEqual(lifecyclePrefix, predicate.getPrefix()),
                                ObjectUtil.notEqual(retentionDay, cortexExpirationRule.getExpirationInDays())
                        );
                        if (isUpdate) {
                            log.info("bucket lifecycle configuration is update, name: {}", bucketName);
                            this.setBucketLifecycleConfiguration(s3Client, bucketName, ruleId, lifecyclePrefix, retentionDay);
                        } else {
                            log.info("cortex history data delete job is not executed，bucket lifecycle configuration is not update, name: {}", bucketName);
                        }
                    }
                }

                // query the existing lifecycle configuration
                bucketLifecycleConfiguration = s3Client.getBucketLifecycleConfiguration(bucketName);
                List<BucketLifecycleConfiguration.Rule> rules1 = bucketLifecycleConfiguration.getRules();
                for (BucketLifecycleConfiguration.Rule l : rules1) {
                    LifecyclePrefixPredicate predicate = (LifecyclePrefixPredicate) l.getFilter().getPredicate();
                    log.info(String.format("Bucket name: %s, Current config: Lifecycle status is %s , Lifecycle prefix is %s , Lifecycle expiration days is %s", bucketName, l.getStatus(), predicate.getPrefix(), l.getExpirationInDays()));
                }
            } else {
                log.warn(String.format("bucket %s does not exist", bucketName));
            }
            log.info("Cortex data bucket config finshed.");
        } catch (Exception e) {
            log.error(e);
        }
    }

    /**
     * set Bucket Lifecycle Configuration
     *
     * @param s3Client
     * @param bucketName
     * @param ruleId
     * @param lifecyclePrefix
     * @param retentionDay
     */
    private void setBucketLifecycleConfiguration(AmazonS3 s3Client, String bucketName, String ruleId, String lifecyclePrefix, Integer retentionDay) {
        log.info("set bucket lifecycle configuration, bucket name: {}, ruleId: {}, lifecyclePrefix: {}, retentionDay: {}", bucketName, ruleId, lifecyclePrefix, retentionDay);
        // Create a rule to archive objects with the "glacierobjects/" prefix to Glacier immediately.
        BucketLifecycleConfiguration.Rule rule = new BucketLifecycleConfiguration.Rule()
                .withId(ruleId)
                .withFilter(new LifecycleFilter(new LifecyclePrefixPredicate(lifecyclePrefix)))
                .withExpirationInDays(retentionDay)
                .withStatus(BucketLifecycleConfiguration.ENABLED);

        // Add the rules to a new BucketLifecycleConfiguration.
        BucketLifecycleConfiguration configuration = new BucketLifecycleConfiguration().withRules(Arrays.asList(rule));
        // Save the configuration.
        s3Client.setBucketLifecycleConfiguration(bucketName, configuration);
    }

}
