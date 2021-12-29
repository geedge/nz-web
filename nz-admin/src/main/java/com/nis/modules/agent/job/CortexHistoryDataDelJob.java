package com.nis.modules.agent.job;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import io.minio.BucketExistsArgs;
import io.minio.GetBucketLifecycleArgs;
import io.minio.MinioClient;
import io.minio.SetBucketLifecycleArgs;
import io.minio.messages.*;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;

import javax.net.ssl.*;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@DisallowConcurrentExecution
public class CortexHistoryDataDelJob extends QuartzJobBean {

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

        String minioEndpoint = String.format("%s://%s/", connectionScheme, sysConfigMap.get(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_ENDPOINT));
        String accessKey = sysConfigMap.get(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_ACCESS_KEY);
        String secretKey = sysConfigMap.get(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_SECRET_ACCESS_KEY);
        String bucketName = sysConfigMap.get(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_BUCKET);
        String regionStr = sysConfigMap.get(Constant.SYSCONFIG_KEY_METRICS_STORAGE_S3_REGION);

        try {
            UrlBuilder urlBuilder = UrlBuilder.ofHttp(minioEndpoint, Charset.forName("UTF-8"));
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(urlBuilder.getHost(), urlBuilder.getPort(), StrUtil.equalsIgnoreCase(connectionScheme, "https") ? true : false)
                    .credentials(accessKey, secretKey)
                    // region 不能为 "" ， 为 null 表示不设置
                    .region(StrUtil.isEmpty(regionStr) ? null : regionStr)
                    // https 取消 SSL 验证
                    .httpClient(getUnsafeOkHttpClent())
                    .build();

            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (bucketExists) {
                log.info(String.format("bucket %s exists", bucketName));
                // 查询过期数据，设置存储桶生命周期
                String metricsStorageRetentionDay = sysConfigMap.get(Constant.SYSCONFIG_KEY_METRICS_STORAGE_RETENTION);

                // cortex 生命周期策略 /fake 目录下 文件保存 metricsStorageRetentionDay d
                List<LifecycleRule> rules = new LinkedList<>();
                rules.add(
                        new LifecycleRule(
                                Status.ENABLED,
                                null,
                                new Expiration((ZonedDateTime) null, Integer.valueOf(metricsStorageRetentionDay), null),
                                new RuleFilter("fake/"),
                                "cortexExpirationRule",
                                null,
                                null,
                                null));
                LifecycleConfiguration config = new LifecycleConfiguration(rules);
                minioClient.setBucketLifecycle(SetBucketLifecycleArgs.builder().bucket(bucketName).config(config).build());

                // 获取配置
                LifecycleConfiguration lifecycleConfiguration = minioClient.getBucketLifecycle(GetBucketLifecycleArgs.builder().bucket(bucketName).build());
                List<LifecycleRule> rules1 = lifecycleConfiguration.rules();
                for (LifecycleRule l : rules1) {
                    log.info(String.format("Lifecycle status is %s , Lifecycle prefix is %s , Lifecycle expiration days is %s", l.status(), l.filter().prefix(), l.expiration().days()));
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

    /**
     * 不校验 minio server 证书
     *
     * @return
     * @throws KeyManagementException
     */
    public OkHttpClient getUnsafeOkHttpClent() throws KeyManagementException {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });
            return builder.build();
        } catch (NoSuchAlgorithmException e) {
            log.error(e);
        }
        return null;
    }
}
