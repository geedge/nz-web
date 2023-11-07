package com.nis.common.config;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.log.Log;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RedisLock;
import com.nis.common.utils.Tool;
import com.nis.modules.sys.service.SysBackupService;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.time.StopWatch;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.RedisTemplate;

import javax.sql.DataSource;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Configuration
@DependsOn(value = {"dataSourceConfig", "redisConfig"})
public class FlywayConfig implements ApplicationContextAware {

    private static final Log log = Log.get();

    private static final String SYS_RESTORE_LOCK_KEY = "sys_restore_lock";

    private List<Callback> migrateCallbackList;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private FlywayProperties flywayProperties;

    @Autowired
    private RedisLock redisLock;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SysBackupService sysBackupService;

    @PostConstruct
    public void migrate() {
        log.info("[FlywayConfig] [migrate] [enable: {}]", flywayProperties.getEnabled());
        if (!flywayProperties.getEnabled()) {
            log.warn("[FlywayConfig] [migrate] [not enabled, do not execute flyway migrate]");
            return;
        }

        // build Fluent Configuration
        FluentConfiguration configure = this.buildFluentConfiguration();

        log.info("[FlywayConfig] [flyway migrate] [begin]");
        StopWatch sw = new StopWatch();
        sw.start();
        Flyway flyway = configure.load();
        try {
            // restore system data
            this.restoreSystemData();

            // migrate
            flyway.migrate();
        } catch (Exception e) {
            log.error(e, "[FlywayConfig] [migrate error.]");

            // repair
            flyway.repair();

            // 停止服务,避免 systemd 服务重启机制,覆盖 flyway
            Tool.RuntimeUtil.exec("systemctl stop nz-web.service");
        } finally {
            // stop
            sw.stop();
        }
        log.info("[FlywayConfig] [flyway migrate] [finshed] [Run time: {}]", sw.toString());
    }

    /**
     * restore system data
     */
    private void restoreSystemData() {
        Boolean hasSysRestoreFilename = redisTemplate.hasKey("sys_restore_filename");
        log.info("[restoreSystemData] [sys_restore_filename key in redis] [exist: {}]", hasSysRestoreFilename);
        if (hasSysRestoreFilename) {
            // get restore filename
            String restoreFilenameInRedis = (String) redisTemplate.opsForValue().get("sys_restore_filename");

            // aes decrypt
            String restoreFilename = Tool.AesUtil.decrypt(restoreFilenameInRedis, Constant.AES_SECRET_KEY);
            if (restoreFilename == null) {
                log.warn("[restoreSystemData] [decrypt error] [restoreFilenameInRedis: {}]", restoreFilenameInRedis);
                return;
            }
            log.info("[restoreSystemData] [restore filename: {}]", restoreFilename);

            // check restore file exists
            Boolean fileExists = sysBackupService.checkRestoreFileExists(restoreFilename);
            if (BooleanUtil.negate(fileExists)) {
                log.warn("[restoreSystemData] [no restore file in this server] [sleep 5 minutes]");
                Tool.ThreadUtil.sleep(5, TimeUnit.MINUTES);
                while (true) {
                    Boolean hasKey = redisTemplate.hasKey(SYS_RESTORE_LOCK_KEY);
                    log.warn("[restoreSystemData] [sys_restore_lock] [release: {}]", !hasKey);
                    if (hasKey) {
                        Tool.ThreadUtil.sleep(10, TimeUnit.SECONDS);
                    } else {
                        log.info("[restoreSystemData] [lock release and start web service]");
                        break;
                    }
                }
            } else {
                // get lock
                String uuid = Tool.StrUtil.uuid();
                try {
                    if (redisLock.lock(SYS_RESTORE_LOCK_KEY, uuid, -1L)) {
                        log.info("[restoreSystemData] [begin] [get lock]");

                        // restore
                        sysBackupService.restore(restoreFilename);

                        // unlock
                        redisLock.unlock(SYS_RESTORE_LOCK_KEY, uuid);
                        // del sys_restore_filename key
                        redisTemplate.delete("sys_restore_filename");

                        log.info("[restoreSystemData] [finshed]");
                    } else {
                        log.warn("[restoreSystemData] [unable to get the lock] [waiting for lock release...]");
                        while (true) {
                            Boolean hasKey = redisTemplate.hasKey(SYS_RESTORE_LOCK_KEY);
                            log.warn("[restoreSystemData] [sys_restore_lock] [release: {}]", !hasKey);
                            if (hasKey) {
                                Tool.ThreadUtil.sleep(10, TimeUnit.SECONDS);
                            } else {
                                log.info("[restoreSystemData] [lock release and start web service]");
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error(e, "[restoreSystemData] [error]");
                    // 还原失败，停止服务
                    Tool.RuntimeUtil.exec("systemctl stop nz-web.service");
                }
            }
        } else {
            log.info("[restoreSystemData] [no need to restore]");
        }
    }

    /**
     * build Fluent Configuration
     *
     * @return A FluentConfiguration Object
     */
    private FluentConfiguration buildFluentConfiguration() {
        log.info("[buildFluentConfiguration] [begin]");

        Map<String, String> configMap = Tool.MapUtil.newHashMap();
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(flywayProperties.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor property : propertyDescriptors) {
                String key = property.getName();
                if (!"class".equals(key) && !"enabled".equals(key)) {
                    Object value = ReflectUtil.invoke(flywayProperties, property.getReadMethod(), key);
                    configMap.put("flyway." + key, value.toString());
                }
            }
        } catch (IntrospectionException e) {
            log.error(e, "[buildFluentConfiguration] [error]");
        }

        FluentConfiguration configure = Flyway.configure();
        configure.dataSource(dataSource);
        configure.configuration(configMap);

        // callbacks
        log.info("[buildFluentConfiguration] [migrate callbacks List] [size: {}]", migrateCallbackList.size());
        if (Tool.CollUtil.isNotEmpty(migrateCallbackList)) {
            log.info("[buildFluentConfiguration] [configure migrate callbacks]");
            configure.callbacks(migrateCallbackList.stream().toArray(Callback[]::new));
        }

        log.info("[buildFluentConfiguration] [finshed]");
        return configure;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // org.flywaydb.core.api.callback;
        Map<String, Callback> callbackMap = applicationContext.getBeansOfType(Callback.class);
        callbackMap = Tool.MapUtil.defaultIfEmpty(callbackMap, Tool.MapUtil.newHashMap());

        List<Callback> callbackList = callbackMap.values().stream().collect(Collectors.toList());
        migrateCallbackList = Tool.CollUtil.defaultIfEmpty(callbackList, Tool.ListUtil.list(true));
        if (log.isDebugEnabled()) {
            log.debug("[FlywayConfig] [Migrate Callback List] [size: {}]", migrateCallbackList.size());
        }
    }
}
