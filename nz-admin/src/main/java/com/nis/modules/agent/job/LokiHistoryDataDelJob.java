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
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@DisallowConcurrentExecution
public class LokiHistoryDataDelJob extends QuartzJobBean {

    static {
        System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, "true");
    }

    private static final Log log = Log.get();

    @Autowired
    private SysConfigService sysConfigService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Thread.currentThread().setName("LokiHistoryDataDelJob");

        // 检查 HA_LOCK
        LeaderElectionDao dao = LeaderElectionDao.getInstance();
        boolean isLeader = dao.isLeader(Constant.SYS_HA_LOCK, Constant.SERVER_ID);
        log.info("[LokiHistoryDataDelJob] [Get HA_LOCK] [isLeader: {}]", isLeader);
        if (BooleanUtil.negate(isLeader)) return;

        log.info("[LokiHistoryDataDelJob] [begin]");

        StopWatch sw = StopWatch.create("LokiHistoryDataDelJob");
        sw.start();
        try {
            // Loki Storage Retention
            this.lokiStorageRetention();
        } catch (Exception e) {
            log.error(e, "[LokiHistoryDataDelJob] [error]");
        } finally {
            sw.stop();
        }

        log.info("[LokiHistoryDataDelJob] [finshed] [Run Time: {}]", sw.prettyPrint());
    }


    /**
     * buidl buildAmazonS3Client
     *
     * @param sysConfigMap
     * @return
     */
    private AmazonS3 buildAmazonS3Client(Map<String, String> sysConfigMap) {
        log.info("[buildAmazonS3Client] [build Amazon S3 Client] [begin]");
        String connectionScheme = sysConfigMap.get(Constant.LOKI_MONITOR_SCHEME);
        connectionScheme = StrUtil.isEmpty(connectionScheme) ? "http" : connectionScheme;

        String minioEndpoint = sysConfigMap.get(Constant.SYSCONFIG_KEY_LOGS_STORAGE_S3_ENDPOINT);
        String accessKey = sysConfigMap.get(Constant.SYSCONFIG_KEY_LOGS_STORAGE_S3_ACCESS_KEY);
        String secretKey = sysConfigMap.get(Constant.SYSCONFIG_KEY_LOGS_STORAGE_S3_SECRET_ACCESS_KEY);
        String regionStr = sysConfigMap.get(Constant.SYSCONFIG_KEY_LOGS_STORAGE_S3_REGION);

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
        log.info("[buildAmazonS3Client] [S3 Endpoint] [url: {}]", JSONPath.read(JSONUtil.toJsonStr(s3Client), "endpoint"));
        log.info("[buildAmazonS3Client] [finshed]");
        return s3Client;
    }

    /**
     * loki Storage Retention
     * 通过对 minio bucket 设置生命周期实现清理过期数据
     */
    public void lokiStorageRetention() {
        List<SysConfigEntity> sysConfigList = sysConfigService.list();
        Map<String, String> sysConfigMap = sysConfigList.stream().collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));

        // logs storage 标识 1: 本地文件存储，2：S3对象存储
        String logsStorageType = sysConfigMap.get(Constant.SYSCONFIG_KEY_LOGS_STORAGE_TYPE);
        logsStorageType = StrUtil.emptyToDefault(logsStorageType, "1");

        log.info("[lokiStorageRetention] [begin] [logs_storage_type: {}]", logsStorageType);
        if (StrUtil.equals("1", logsStorageType)) {
            log.warn("[lokiStorageRetention] [not run job] [logs_storage_type: {}]", logsStorageType);
            return;
        }

        // build Amazon S3 Client
        AmazonS3 s3Client = this.buildAmazonS3Client(sysConfigMap);

        String bucketName = sysConfigMap.get(Constant.SYSCONFIG_KEY_LOGS_STORAGE_S3_BUCKET);
        boolean doesBucketExistV2 = s3Client.doesBucketExistV2(bucketName);
        log.info("[lokiStorageRetention] [bucket: {}] [exist: {}]", bucketName, doesBucketExistV2);

        if (doesBucketExistV2) {
            // 日志存储天数，默认 7 天
            String retentionDayStr = sysConfigMap.get(Constant.SYSCONFIG_KEY_LOGS_STORAGE_RETENTION);
            Integer retentionDay = Integer.valueOf(StrUtil.emptyToDefault(retentionDayStr, "7"));

            // Retrieve the configuration.
            // Verify that the configuration now has one rules. prefix: fake/ expirationInDays: sysconfig.logsStorageRetentionDay
            BucketLifecycleConfiguration bucketLifecycleConfiguration = s3Client.getBucketLifecycleConfiguration(bucketName);
            log.info("[lokiStorageRetention] [currunt lifecycle] [exist: {}]", ObjectUtil.isNotNull(bucketLifecycleConfiguration));

            String ruleId = "LogsExpirationRule";
            if (ObjectUtil.isNull(bucketLifecycleConfiguration)) {
                log.info("[lokiStorageRetention] [add lifecycle configuration]");
                this.setBucketLifecycleConfiguration(s3Client, bucketName, ruleId, retentionDay);
            } else {
                List<BucketLifecycleConfiguration.Rule> bucketLifecycleConfigurationRules = bucketLifecycleConfiguration.getRules();
                bucketLifecycleConfigurationRules = Tool.CollUtil.defaultIfEmpty(bucketLifecycleConfigurationRules, Tool.ListUtil.empty());
                BucketLifecycleConfiguration.Rule logsExpirationRule = bucketLifecycleConfigurationRules.stream().filter(rule -> StrUtil.equals(rule.getId(), ruleId)).findFirst().orElse(null);
                log.info("[lokiStorageRetention] [filter rules by id] [bucket: {}] [ruleId: {}] [exist: {}]", bucketName, ruleId, ObjectUtil.isNotNull(logsExpirationRule));

                if (ObjectUtil.isNull(logsExpirationRule)) {
                    log.info("[lokiStorageRetention] [add lifecycle configuration]");
                    this.setBucketLifecycleConfiguration(s3Client, bucketName, ruleId, retentionDay);
                } else {
                    // 当前存在过期策略，判断规则是否发生变化
                    boolean isUpdate = BooleanUtil.or(
                            ObjectUtil.notEqual(BucketLifecycleConfiguration.ENABLED, logsExpirationRule.getStatus()),
                            ObjectUtil.notEqual(retentionDay, logsExpirationRule.getExpirationInDays())
                    );

                    log.info("[lokiStorageRetention] [lifecycle configuration] [isUpdate: {}]", isUpdate);
                    if (isUpdate) {
                        log.info("[lokiStorageRetention] [update lifecycle configuration]");
                        this.setBucketLifecycleConfiguration(s3Client, bucketName, ruleId, retentionDay);
                    } else {
                        log.info("[lokiStorageRetention] [lifecycle configuration is not update] [bucket: {}]", bucketName);
                    }
                }
            }

            // query the existing lifecycle configuration
            bucketLifecycleConfiguration = s3Client.getBucketLifecycleConfiguration(bucketName);
            List<BucketLifecycleConfiguration.Rule> rules1 = bucketLifecycleConfiguration.getRules();
            for (BucketLifecycleConfiguration.Rule l : rules1) {
                LifecyclePrefixPredicate predicate = (LifecyclePrefixPredicate) l.getFilter().getPredicate();
                log.info("[lokiStorageRetention] [current lifecycle configuration] [bucket: {}] [status: {}] [prefix: {}] [expiration days: {}]", bucketName, l.getStatus(), predicate != null ? predicate.getPrefix() : "", l.getExpirationInDays());
            }
        } else {
            log.warn("[lokiStorageRetention] [bucket does not exist] [name: {}]", bucketName);
        }

        log.info("[lokiStorageRetention] [finshed]");
    }

    /**
     * set Bucket Lifecycle Configuration
     *
     * @param s3Client
     * @param bucketName
     * @param ruleId
     * @param retentionDay
     */
    private void setBucketLifecycleConfiguration(AmazonS3 s3Client, String bucketName, String ruleId, Integer retentionDay) {
        log.info("[setBucketLifecycleConfiguration] [bucket: {}] [ruleId: {}] [retentionDay: {}]", bucketName, ruleId, retentionDay);
        // Create a rule to archive objects with the "glacierobjects/" prefix to Glacier immediately.
        BucketLifecycleConfiguration.Rule rule = new BucketLifecycleConfiguration.Rule()
                .withId(ruleId)
                .withFilter(new LifecycleFilter())
                .withExpirationInDays(retentionDay)
                .withStatus(BucketLifecycleConfiguration.ENABLED);

        // Add the rules to a new BucketLifecycleConfiguration.
        BucketLifecycleConfiguration configuration = new BucketLifecycleConfiguration().withRules(Arrays.asList(rule));
        // Save the configuration.
        s3Client.setBucketLifecycleConfiguration(bucketName, configuration);
    }

}
