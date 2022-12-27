package com.nis.modules.terminal.job;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.sys.service.SysConfService;
import com.nis.modules.terminal.service.TerminalRecordService;
import com.nis.modules.terminal.service.TerminalSessionService;
import com.nis.modules.terminal.utils.TerminalConstant;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@DisallowConcurrentExecution
public class CleanExpiredTerminalRecordDataJob extends QuartzJobBean {

    private static final Log log = Log.get();

    static {
        System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, "true");
    }

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SysConfService sysConfService;

    @Autowired
    private TerminalSessionService terminalSessionService;

    @Autowired
    private TerminalRecordService terminalRecordService;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Thread.currentThread().setName("CleanExpiredTerminalRecordDataJob");

        // 检查 haMode
        String serverId = (String) redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
        log.info("[CleanExpiredTerminalRecordDataJob] [Get ha lock from redis] [server id: {}]", serverId);
        if (!StrUtil.equals(serverId, Constant.SERVER_ID)) {
            log.warn("[CleanExpiredTerminalRecordDataJob] [did not get the ha lock, not run job] [redis server id: {}] [system server id: {}]", serverId, Constant.SERVER_ID);
            return;
        }

        log.info("[CleanExpiredTerminalRecordDataJob] [start cleaning expired data]");

        StopWatch sw = StopWatch.create("CleanExpiredTerminalRecordDataJob");
        sw.start();

        try {
            // clean Expired Terminal Record Data
            this.cleanExpiredTerminalRecordData();
        } catch (Exception e) {
            log.error(e, "[CleanExpiredTerminalRecordDataJob] [failed to clean data]");
        } finally {
            sw.stop();
        }

        log.info("[CleanExpiredTerminalRecordDataJob] [clean data complete] [Run Time: {}]", sw.prettyPrint());
    }

    @Transactional(rollbackFor = Exception.class)
    public void cleanExpiredTerminalRecordData() {
        // 存储模式，1: 本地存储，2：S3对象存储
        String terminalStorageType = sysConfService.getValue(TerminalConstant.TERMINAL_STORAGE_TYPE);
        terminalStorageType = StrUtil.emptyToDefault(terminalStorageType, "1");
        log.info("[cleanExpiredTerminalRecordData] [record storage type] [value: {}]", terminalStorageType);

        DateTime now = Tool.DateUtil.date();

        // 数据存储天数，默认 365 天
        String terminalRecordLocalRetention = sysConfService.getValue(TerminalConstant.TERMINAL_RECORD_LOCAL_RETENTION);
        terminalRecordLocalRetention = Tool.StrUtil.emptyToDefault(terminalRecordLocalRetention, "365");

        // 过期时间
        DateTime expireDate = Tool.DateUtil.offsetDay(now, -Integer.valueOf(terminalRecordLocalRetention));
        log.info("[cleanExpiredTerminalRecordData] [clear expired record date in db] [now: {}] [retention days: {}] [expire date: {}]", now, terminalRecordLocalRetention, expireDate);
        terminalSessionService.cleanExpiredDataByTime(expireDate);
        log.info("[cleanExpiredTerminalRecordData] [clear expired record date in db finshed]");

        // 如果是 S3 存储内容信息，则配置存储桶过期策略
        if (StrUtil.equals("2", terminalStorageType)) {
            AmazonS3 s3Client = terminalRecordService.buildTerminalStorageS3Client();
            try {
                String bucketName = sysConfService.getValue(TerminalConstant.TERMINAL_STORAGE_S3_BUCKET);
                log.info("[cleanExpiredTerminalRecordData] [config terminal record bucket lifecycle] [bucketName: {}] [retention days: {}]", bucketName, terminalRecordLocalRetention);

                if (s3Client.doesBucketExistV2(bucketName)) {
                    log.info("[cleanExpiredTerminalRecordData] [bucket exist] [bucketName: {}]", bucketName);

                    Integer retentionDay = Integer.valueOf(terminalRecordLocalRetention);
                    String ruleId = "terminalRecordExpirationRule";

                    // Retrieve the configuration.
                    // Verify that the configuration now has one rules. prefix: fake/ expirationInDays: sysconfig.metricsStorageRetentionDay
                    BucketLifecycleConfiguration bucketLifecycleConfiguration = s3Client.getBucketLifecycleConfiguration(bucketName);
                    if (ObjectUtil.isNull(bucketLifecycleConfiguration)) {
                        log.info("[cleanExpiredTerminalRecordData] [bucket has empty lifecycle configuration] [bucketName: {}]", bucketName);
                        this.setBucketLifecycleConfiguration(s3Client, bucketName, ruleId, retentionDay);
                    } else {
                        List<BucketLifecycleConfiguration.Rule> bucketLifecycleConfigurationRules = bucketLifecycleConfiguration.getRules();
                        bucketLifecycleConfigurationRules = Tool.CollUtil.defaultIfEmpty(bucketLifecycleConfigurationRules, Tool.ListUtil.empty());

                        BucketLifecycleConfiguration.Rule cortexExpirationRule = bucketLifecycleConfigurationRules.stream()
                                .filter(rule -> StrUtil.equals(rule.getId(), ruleId))
                                .findFirst()
                                .orElse(null);

                        if (ObjectUtil.isNull(cortexExpirationRule)) {
                            log.info("[cleanExpiredTerminalRecordData] [bucket has empty lifecycle configuration] [bucketName: {}]", bucketName);
                            this.setBucketLifecycleConfiguration(s3Client, bucketName, ruleId, retentionDay);
                        } else {
                            // 当前存在过期策略，判断规则是否发生变化
                            boolean isUpdate = BooleanUtil.or(
                                    ObjectUtil.notEqual(BucketLifecycleConfiguration.ENABLED, cortexExpirationRule.getStatus()),
                                    ObjectUtil.notEqual(retentionDay, cortexExpirationRule.getExpirationInDays())
                            );
                            log.info("[cleanExpiredTerminalRecordData] [bucket lifecycle configuration] [bucketName: {}] [updated: {}]", bucketName, isUpdate);
                            if (isUpdate) {
                                this.setBucketLifecycleConfiguration(s3Client, bucketName, ruleId, retentionDay);
                            }
                        }
                    }

                    // query the existing lifecycle configuration
                    bucketLifecycleConfiguration = s3Client.getBucketLifecycleConfiguration(bucketName);
                    List<BucketLifecycleConfiguration.Rule> rules1 = bucketLifecycleConfiguration.getRules();
                    for (BucketLifecycleConfiguration.Rule l : rules1) {
                        log.info("[cleanExpiredTerminalRecordData] [current lifecycle config info] [bucketName: {}] [status: {}] [expiration days: {}]", bucketName, l.getStatus(), l.getExpirationInDays());
                    }
                } else {
                    log.warn("[cleanExpiredTerminalRecordData] [bucket does not exist] [bucketName: {}]", bucketName);
                }
            } catch (Exception e) {
                log.error(e, "[cleanExpiredTerminalRecordData] [error]");
            } finally {
                s3Client.shutdown();
            }
        }
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
        log.info("[setBucketLifecycleConfiguration] [bucketName: {}] [ruleId: {}] [retentionDay: {}]", bucketName, ruleId, retentionDay);

        BucketLifecycleConfiguration.Rule rule = new BucketLifecycleConfiguration.Rule()
                .withId(ruleId)
                .withExpirationInDays(retentionDay)
                .withStatus(BucketLifecycleConfiguration.ENABLED);

        BucketLifecycleConfiguration configuration = new BucketLifecycleConfiguration().withRules(Arrays.asList(rule));
        // Save the configuration.
        s3Client.setBucketLifecycleConfiguration(bucketName, configuration);
    }

}
