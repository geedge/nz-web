package com.nis.modules.sys.backend;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.toolkit.SqlRunner;
import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.*;
import com.google.gson.Gson;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RedisLock;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetModel;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetModelService;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.module.service.MonitorModuleService;
import com.nis.modules.project.entity.MonitorProject;
import com.nis.modules.project.service.MonitorProjectService;
import com.nis.modules.sys.util.RedisSearchUtil;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPooled;

import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DbSyncRedisThread
 * 通过监听 binlog，将 mysql 数据同步到 redis
 *
 * @author shizhendong
 * @date 2023-02-03
 */
@Component
public class DbSyncRedisThread implements Runnable {

    private static final Log log = Log.get();

    @Value("${database.host}")
    private String databaseHost;
    @Value("${database.port:3306}")
    private int databasePort;
    @Value("${database.user}")
    private String databaseUser;
    @Value("${database.pin}")
    private String databasePin;

    @Value("${redis.host}")
    private String redisHost;
    @Value("${redis.port:6379}")
    private int redisPort;
    @Value("${redis.database:0}")
    private int redisDatabase;
    @Value("${redis.pin}")
    private String redisPin;
    @Value("${spring.redis.jedis.pool.max-idle:10}")
    private int maxIdle;
    @Value("${spring.redis.jedis.pool.min-idle:3}")
    private int minIdle;
    @Value("${spring.redis.jedis.pool.max-wait:100000}")
    private int maxWait;
    @Value("${spring.redis.jedis.pool.max-active:100}")
    private int maxActive;
    @Value("${spring.redis.timeout:100000}")
    private int redisTimeout;

    /**
     * table_columns_map
     * key: table_name
     * value: table columns info. include ordinal_position and column_name.
     */
    private static Map<String, Object> TABLE_COLUMNS_MAP;

    /**
     * redis key
     * sync binlog position
     */
    private static final String REDIS_KEY_BINLOG_POSITION = "sync_binlog_position";

    /**
     * sync_table_names
     * 数据同步的表名称
     */
    private static List<String> SYNC_TABLE_NAMES = Tool.ListUtil.of(
            "dc", "asset_asset", "asset_model",
            "monitor_project", "monitor_module", "monitor_endpoint",
            "alert_rule", "alert_message_active", "alert_message_history", "alert_message_silence"
    );

    private JedisPooled jedisClient;

    private static final Gson GSON = new Gson();

    @Autowired
    private RedisLock redisLock;

    @Autowired
    private DcService dcService;

    @Autowired
    private AssetAssetService assetService;

    @Autowired
    private AssetModelService assetModelService;

    @Autowired
    private MonitorProjectService projectService;

    @Autowired
    private MonitorModuleService moduleService;

    @Autowired
    private MonitorEndpointService endpointService;

    @Autowired
    private AlertRuleService alertRuleService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * init Columns Mapping
     */
    private void initTableCloumnsMapping() {
        log.info("[initTableCloumnsMapping] [init table columns info begin]");

        TABLE_COLUMNS_MAP = Tool.MapUtil.newHashMap();
        String querySqlStr = StrUtil.concat(true, "SELECT ",
                "TABLE_NAME, COLUMN_NAME, ORDINAL_POSITION ",
                "FROM ",
                "INFORMATION_SCHEMA.COLUMNS ",
                "WHERE  TABLE_SCHEMA = DATABASE();");
        List<Map<String, Object>> selectList = SqlRunner.db().selectList(querySqlStr);
        Map<String, List<Map<String, Object>>> tMap = selectList.stream()
                .filter(map -> SYNC_TABLE_NAMES.contains(StrUtil.toString(map.get("TABLE_NAME"))))
                .collect(Collectors.groupingBy(map -> StrUtil.toString(map.get("TABLE_NAME"))));

        for (Map.Entry<String, List<Map<String, Object>>> entry : tMap.entrySet()) {
            String tableName = entry.getKey();
            List<Map<String, Object>> value = entry.getValue();

            Map<Integer, String> positionWithNameMapping = value.stream()
                    .collect(Collectors.toMap(
                            map -> {
                                String ordinalPosition = map.get("ORDINAL_POSITION").toString();
                                return Integer.valueOf(ordinalPosition);
                            },
                            map -> {
                                String columnName = map.get("COLUMN_NAME").toString();
                                return StringUtils.underlineToCamel(columnName);
                            }));
            TABLE_COLUMNS_MAP.put(tableName, positionWithNameMapping);
        }

        log.info("[initTableCloumnsMapping] [table columns map size: {}]", TABLE_COLUMNS_MAP.size());
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Db Sync Redis Thread");
        log.info("DbSyncRedisThread start...");

        // init Table Cloumns Mapping
        this.initTableCloumnsMapping();

        // jedis client
        this.jedisClient = this.buildJedisPool(redisHost, redisPort, redisTimeout, redisPin, redisDatabase, maxIdle, minIdle, maxWait, maxActive);

        BinaryLogClient binaryLogClient = null;
        try {
            binaryLogClient = this.buildBinaryLogClient(this.databaseHost, this.databasePort, this.databaseUser, this.databasePin);

            // binaryLogClient.setEventDeserializer();

            // Lifecycle Listener
            binaryLogClient.registerLifecycleListener(this.buildLifecycleListener());

            // Event Listener
            binaryLogClient.registerEventListener(this.buildEventListener());

            try {
                // connect
                binaryLogClient.connect();
            } catch (IOException e) {
                log.error(e, "[DbSyncRedisThread] [BinaryLogClient connect error]");
            }
        } finally {
            // disconnect
            try {
                if (binaryLogClient != null) {
                    binaryLogClient.disconnect();
                }
            } catch (IOException e) {
                log.error(e, "[DbSyncRedisThread] [disconnect BinaryLogClient error]");
            }

            // close
            Tool.IoUtil.close(this.jedisClient);
        }
    }


    /**
     * build Jedis Pool
     *
     * @param redisHost
     * @param redisPort
     * @param redisTimeout
     * @param redisPin
     * @param redisDatabase
     * @param maxIdle
     * @param minIdle
     * @param maxWait
     * @param maxActive
     * @return
     */
    private JedisPooled buildJedisPool(String redisHost, int redisPort, int redisTimeout, String redisPin, int redisDatabase, int maxIdle, int minIdle, int maxWait, int maxActive) {
        log.info("[buildJedisPool] [begin]");
        log.info("[buildJedisPool] [redisHost: {}] [redisPort: {}] [redisDatabase: {}]", redisHost, redisPort, redisDatabase);

        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMaxWait(Duration.ofMillis(maxWait));

        JedisPooled jedisPooled = new JedisPooled(poolConfig, redisHost, redisPort, redisTimeout, StrUtil.emptyToNull(redisPin), redisDatabase);
        log.info("[buildJedisPool] [finshed]");
        return jedisPooled;
    }

    /**
     * build Binary Log Client
     *
     * @param databaseHost
     * @param databasePort
     * @param databaseUser
     * @param databasePin
     * @return
     */
    private BinaryLogClient buildBinaryLogClient(String databaseHost, int databasePort, String databaseUser, String databasePin) {
        log.info("[buildBinaryLogClient] [begin]");
        log.info("[buildBinaryLogClient] [databaseHost: {}] [databasePort: {}] [databaseUser: {}]", databaseHost, databasePort, databaseUser);

        // 数据库密码解密
        databasePin = Tool.AesUtil.decrypt(databasePin, Constant.AES_SECRET_KEY);

        // Binary Log Client
        BinaryLogClient client = new BinaryLogClient(databaseHost, databasePort, databaseUser, databasePin);

        log.info("[buildBinaryLogClient] [finshed]");
        return client;
    }

    /**
     * build Lifecycle Listener
     * 1. create Redis Search Index
     * 2. update full data
     *
     * @return
     */
    private BinaryLogClient.AbstractLifecycleListener buildLifecycleListener() {
        log.info("[buildLifecycleListener] [begin]");
        BinaryLogClient.AbstractLifecycleListener lifecycleListener = new BinaryLogClient.AbstractLifecycleListener() {
            @Override
            public void onConnect(BinaryLogClient client) {
                super.onConnect(client);

                // HA
                try {
                    boolean lock = redisLock.lock("lock_db_sync_redis", Constant.SERVER_ID, 5L);
                    log.info("[onConnect] [Get sync lock from redis] [result: {}]", lock);
                    if (lock) {
                        // create Redis Search Index
                        // TODO https://github.com/RediSearch/RediSearch/issues/367
                        RedisSearchUtil redisSearchUtil = RedisSearchUtil.RedisSearchUtilBuilder.builder()
                                .jedisPooled(jedisClient)
                                .build();
                        // create Redis Search Index
                        redisSearchUtil.createRedisSearchIndex();

                        // db
                        long currentBinlogPosition = client.getBinlogPosition();
                        // redis
                        String binlogPositionInRedis = redisTemplate.opsForValue().get(REDIS_KEY_BINLOG_POSITION);
                        log.info("[onConnect] [binlog position] [db: {}] [redis: {}]", currentBinlogPosition, binlogPositionInRedis);

                        if (!StrUtil.equals(StrUtil.toString(currentBinlogPosition), binlogPositionInRedis)) {
                            log.info("[onConnect] [binlog position is updated. full update cache data]");
                            // 全量更新-同步
                            updateAll();
                            // update position
                            redisTemplate.opsForValue().set(REDIS_KEY_BINLOG_POSITION, StrUtil.toString(currentBinlogPosition));
                        } else {
                            log.info("[onConnect] [binlog position not updated. do not update cached data]");
                        }
                    } else {
                        log.warn("[DbSyncRedisThread] [did not get the sync lock, do not update cached data]");
                    }
                } finally {
                    redisLock.unlock("lock_db_sync_redis", Constant.SERVER_ID);
                }
            }
        };
        log.info("[buildLifecycleListener] [finshed]");
        return lifecycleListener;
    }

    /**
     * build Event Listener
     *
     * @return
     */
    private BinaryLogClient.EventListener buildEventListener() {
        log.info("[buildEventListener] [begin]");
        BinaryLogClient.EventListener eventListener = new BinaryLogClient.EventListener() {
            @Override
            public void onEvent(Event event) {
                // 检查 HA mode
                String serverId = redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
                if (!Tool.StrUtil.equals(serverId, Constant.SERVER_ID)) {
                    return;
                }

                EventHeader eventHeader = event.getHeader();
                EventType eventType = eventHeader.getEventType();
                EventData eventData = event.getData();

                // table
                if (Tool.ObjectUtil.equal(0, EventType.TABLE_MAP.compareTo(eventType))) {
                    TableMapEventData tableMapEventData = (TableMapEventData) eventData;
                    String tableName = tableMapEventData.getTable();
                    if (SYNC_TABLE_NAMES.contains(tableName)) {
                        long tableId = tableMapEventData.getTableId();
                        if (Tool.ObjectUtil.isNotNull(TABLE_COLUMNS_MAP.get(StrUtil.toString(tableId)))) {
                            return;
                        }
                        // TABLE_MAP 可监听到内容包含 table_id & table_name
                        // 其他 EVENT 只能获取到 table_id，table_id 只在监听时可获得，于是在 TABLE_MAP 事件做对应
                        // TABLE_COLUMNS_MAP.put(StrUtil.toString(tableId), TABLE_COLUMNS_MAP.get(tableName));
                        TABLE_COLUMNS_MAP.put(StrUtil.concat(true, StrUtil.toString(tableId), "_table_name"), tableName);
                    }
                }

                // Insert
                if (Tool.ObjectUtil.equal(0, EventType.WRITE_ROWS.compareTo(eventType))) {
                    WriteRowsEventData writeRowsEventData = (WriteRowsEventData) eventData;
                    String tableName = (String) TABLE_COLUMNS_MAP.get(StrUtil.toString(writeRowsEventData.getTableId()) + "_table_name");
                    if (Tool.ObjectUtil.isNull(tableName)) return;

                    Map<Integer, String> columnsInfo = (Map) TABLE_COLUMNS_MAP.get(tableName);
                    for (Serializable[] row : writeRowsEventData.getRows()) {
                        Map<Object, Object> insertEntityMap = Tool.MapUtil.builder().map();
                        for (int i = 0; i < row.length; i++) {
                            insertEntityMap.put(columnsInfo.get(i + 1), row[i]);
                        }
                        // set
                        jedisClient.jsonSet(getKey(tableName, insertEntityMap.get("id")), GSON.toJson(insertEntityMap));
                    }
                }

                // Update
                if (Tool.ObjectUtil.equal(0, EventType.UPDATE_ROWS.compareTo(eventType))) {
                    UpdateRowsEventData updateRowsEventData = (UpdateRowsEventData) eventData;
                    String tableName = (String) TABLE_COLUMNS_MAP.get(StrUtil.toString(updateRowsEventData.getTableId()) + "_table_name");
                    if (Tool.ObjectUtil.isNull(tableName)) return;

                    Map<Integer, String> columnsInfo = (Map) TABLE_COLUMNS_MAP.get(tableName);
                    for (Map.Entry<Serializable[], Serializable[]> entry : updateRowsEventData.getRows()) {
                        Serializable[] after = entry.getValue();
                        Map<Object, Object> updateEntityMap = Tool.MapUtil.builder().map();
                        for (int i = 0; i < after.length; i++) {
                            updateEntityMap.put(columnsInfo.get(i + 1), after[i]);
                        }
                        // set
                        jedisClient.jsonSet(getKey(tableName, updateEntityMap.get("id")), GSON.toJson(updateEntityMap));
                    }
                }

                // Delete
                if (Tool.ObjectUtil.equal(0, EventType.DELETE_ROWS.compareTo(eventType))) {
                    DeleteRowsEventData deleteRowsEventData = (DeleteRowsEventData) eventData;
                    String tableName = (String) TABLE_COLUMNS_MAP.get(StrUtil.toString(deleteRowsEventData.getTableId()) + "_table_name");
                    if (Tool.ObjectUtil.isNull(tableName)) return;

                    Map<Integer, String> columnsInfo = (Map) TABLE_COLUMNS_MAP.get(tableName);
                    for (Serializable[] row : deleteRowsEventData.getRows()) {
                        for (int i = 0; i < row.length; i++) {
                            String fieldName = columnsInfo.get(i + 1);
                            if (StrUtil.equals("id", fieldName)) {
                                // del
                                jedisClient.del(getKey(tableName, row[i]));
                            }
                        }
                    }
                }

                // XID
                if (Tool.ObjectUtil.equal(0, EventType.XID.compareTo(eventType))) {
                    EventHeaderV4 header = (EventHeaderV4) eventHeader;
                    redisTemplate.opsForValue().set(REDIS_KEY_BINLOG_POSITION, StrUtil.toString(header.getNextPosition()));
                }
            }
        };
        log.info("[buildEventListener] [finshed]");
        return eventListener;
    }

    /**
     * 全量更新 asset & endpoint 内容
     * 先删除后增
     */
    private void updateAll() {
        log.info("[updateAll] [begin]");

        StopWatch sw = new StopWatch();
        sw.start();
        try {
            // remove
            this.removeTablesCache();

            // push
            this.pushTablesCache();
        } catch (Exception e) {
            log.error(e, "[updateAll] [error]");
            // 同步失败,停止服务
            Tool.RuntimeUtil.exec("systemctl stop nz-web.service");
        } finally {
            sw.stop();
        }
        log.info("[updateAll] [finshed] [Run Time: {}]", sw.toString());
    }

    /**
     * remove Tables Cache
     */
    private void removeTablesCache() {
        log.info("[removeTableCache] [begin]");
        StopWatch sw = new StopWatch();
        sw.start();

        Set<String> dcRemoveKeys = redisTemplate.keys("dc:*");
        if (Tool.CollUtil.isNotEmpty(dcRemoveKeys)) redisTemplate.delete(dcRemoveKeys);

        Set<String> assetRemoveKeys = redisTemplate.keys("asset_asset:*");
        if (Tool.CollUtil.isNotEmpty(assetRemoveKeys)) redisTemplate.delete(assetRemoveKeys);

        Set<String> assetModelRemoveKeys = redisTemplate.keys("asset_model:*");
        if (Tool.CollUtil.isNotEmpty(assetModelRemoveKeys)) redisTemplate.delete(assetModelRemoveKeys);

        Set<String> projectRemoveKeys = redisTemplate.keys("monitor_project:*");
        if (Tool.CollUtil.isNotEmpty(projectRemoveKeys)) redisTemplate.delete(projectRemoveKeys);

        Set<String> moduleRemoveKeys = redisTemplate.keys("monitor_module:*");
        if (Tool.CollUtil.isNotEmpty(moduleRemoveKeys)) redisTemplate.delete(moduleRemoveKeys);

        Set<String> endpointRemoveKeys = redisTemplate.keys("monitor_endpoint:*");
        if (Tool.CollUtil.isNotEmpty(endpointRemoveKeys)) redisTemplate.delete(endpointRemoveKeys);

        Set<String> ruleRemoveKeys = redisTemplate.keys("alert_rule:*");
        if (Tool.CollUtil.isNotEmpty(ruleRemoveKeys)) redisTemplate.delete(ruleRemoveKeys);

        Set<String> alertMsgActiveRemoveKeys = redisTemplate.keys("alert_message_active:*");
        if (Tool.CollUtil.isNotEmpty(alertMsgActiveRemoveKeys)) redisTemplate.delete(alertMsgActiveRemoveKeys);

        Set<String> alertMsgHistoryRemoveKeys = redisTemplate.keys("alert_message_history:*");
        if (Tool.CollUtil.isNotEmpty(alertMsgHistoryRemoveKeys)) redisTemplate.delete(alertMsgHistoryRemoveKeys);

        Set<String> alertMsgSilenceruleRemoveKeys = redisTemplate.keys("alert_message_silence:*");
        if (Tool.CollUtil.isNotEmpty(alertMsgSilenceruleRemoveKeys))
            redisTemplate.delete(alertMsgSilenceruleRemoveKeys);

        sw.stop();
        log.info("[removeTableCache] [finshed] [Run Time: {}]", sw.toString());
    }

    /**
     * push Tables Cache
     */
    private void pushTablesCache() {
        log.info("[pushTablesCache] [begin]");
        StopWatch sw = new StopWatch();
        sw.start();

        List<Dc> dcList = dcService.list();
        for (Dc dc : dcList) {
            this.jedisClient.jsonSet(this.getKey("dc", dc.getId()), GSON.toJson(dc));
        }

        List<AssetAsset> assetList = assetService.list();
        for (AssetAsset asset : assetList) {
            this.jedisClient.jsonSet(this.getKey("asset_asset", asset.getId()), GSON.toJson(asset));
        }

        List<AssetModel> modelList = assetModelService.list();
        for (AssetModel model : modelList) {
            this.jedisClient.jsonSet(this.getKey("asset_model", model.getId()), GSON.toJson(model));
        }

        List<MonitorProject> projectList = projectService.list();
        for (MonitorProject project : projectList) {
            this.jedisClient.jsonSet(this.getKey("monitor_project", project.getId()), GSON.toJson(project));
        }

        List<MonitorModule> moduleList = moduleService.list();
        for (MonitorModule module : moduleList) {
            this.jedisClient.jsonSet(this.getKey("monitor_module", module.getId()), GSON.toJson(module));
        }

        List<MonitorEndpoint> endpointList = endpointService.list();
        for (MonitorEndpoint endpoint : endpointList) {
            this.jedisClient.jsonSet(this.getKey("monitor_endpoint", endpoint.getId()), GSON.toJson(endpoint));
        }

        List<AlertRuleEntity> alertRuleList = alertRuleService.list();
        for (AlertRuleEntity alertRule : alertRuleList) {
            this.jedisClient.jsonSet(this.getKey("alert_rule", alertRule.getId()), GSON.toJson(alertRule));
        }

        List<Map<String, Object>> alertMsgActiveList = SqlRunner.db().selectList("SELECT * FROM alert_message_active;");
        for (Map<String, Object> msgEntity : alertMsgActiveList) {
            Map<String, Object> tMap = msgEntity.entrySet().stream().collect(HashMap::new, (k, v) -> k.put(StringUtils.underlineToCamel(v.getKey()), v.getValue()), HashMap::putAll);
            this.jedisClient.jsonSet(this.getKey("alert_message_active", Tool.MapUtil.getStr(msgEntity, "id")), GSON.toJson(tMap));
        }

        List<Map<String, Object>> alertMsgHistoryList = SqlRunner.db().selectList("SELECT * FROM alert_message_history;");
        for (Map<String, Object> msgEntity : alertMsgHistoryList) {
            Map<String, Object> tMap = msgEntity.entrySet().stream().collect(HashMap::new, (k, v) -> k.put(StringUtils.underlineToCamel(v.getKey()), v.getValue()), HashMap::putAll);
            this.jedisClient.jsonSet(this.getKey("alert_message_history", Tool.MapUtil.getStr(msgEntity, "id")), GSON.toJson(tMap));
        }

        List<Map<String, Object>> alertMsgSilenceList = SqlRunner.db().selectList("SELECT * FROM alert_message_silence;");
        for (Map<String, Object> msgEntity : alertMsgSilenceList) {
            Map<String, Object> tMap = msgEntity.entrySet().stream().collect(HashMap::new, (k, v) -> k.put(StringUtils.underlineToCamel(v.getKey()), v.getValue()), HashMap::putAll);
            this.jedisClient.jsonSet(this.getKey("alert_message_silence", Tool.MapUtil.getStr(msgEntity, "id")), GSON.toJson(tMap));
        }
        sw.stop();
        log.info("[pushTablesCache] [finshed] [Run Time: {}]", sw.toString());
    }

    /**
     * get Key
     *
     * @param prefix table name
     * @param id
     * @return
     */
    private String getKey(String prefix, Object id) {
        return StrUtil.concat(true, prefix, ":", StrUtil.toString(id));
    }
}
