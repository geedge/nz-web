package com.nis.modules.sys.util;

import cn.hutool.log.Log;
import com.nis.common.smartvalidate.ann.NotNull;
import com.nis.common.utils.Tool;
import lombok.Data;
import org.apache.commons.lang3.time.StopWatch;
import redis.clients.jedis.Connection;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.search.IndexDefinition;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Schema;
import redis.clients.jedis.util.SafeEncoder;

import java.util.Collections;
import java.util.List;


/**
 * RedisSearchUtil
 * 创建 RedisSearch Index
 * 用法 : RedisSearchUtil.RedisSearchUtilBuilder.builder().jedisPooled(jedisClient).build().createRedisSearchIndex();
 *
 * @author shizhendong
 * @date 2023-02-03
 */
@Data
public class RedisSearchUtil {

    private final static Log log = Log.get();

    @NotNull
    private JedisPooled jedisClient;

    /**
     * query Existing Index List
     * exec FT._LIST command by sendCommand() function
     *
     * @return
     */
    private List<String> queryExistingIndexList() {
        Connection resource = this.jedisClient.getPool().getResource();
        try {
            ProtocolCommand protocolCommand = new ProtocolCommand() {
                @Override
                public byte[] getRaw() {
                    return SafeEncoder.encode("FT._LIST");
                }
            };
            resource.sendCommand(protocolCommand);

            List<String> respList = Tool.ListUtil.list(true);
            Object response = resource.getOne();
            if (response instanceof List) {
                for (Object itemResp : ((List) response)) {
                    respList.add(new String((byte[]) itemResp));
                }
            } else {
                respList = Collections.singletonList(new String((byte[]) response));
            }
            if (log.isDebugEnabled()) {
                log.debug("[queryExistingIndexList] [finshed] [exist index size: {}] [content: {}]", respList.size(), Tool.JSONUtil.toJsonStr(respList));
            }
            return respList;
        } finally {
            resource.close();
        }
    }

    /**
     * create Redis Search Index
     */
    public void createRedisSearchIndex() {
        log.info("[createRedisSearchIndex] [begin]");
        StopWatch sw = new StopWatch();
        sw.start();

        // dc
        this.createDcIndex();

        // asset_asset
        this.createAssetIndex();

        // asset_model
        this.createModelIndex();

        // monitor_project
        this.createProjectIndex();

        // monitor_module
        this.createModuleIndex();

        // monitor_endpoint
        this.createEndpointIndex();

        // alert_rule
        this.createAlertRuleIndex();

        // alert_message_active
        this.createAlertMsgActiveIndex();

        // alert_message_history
        this.createAlertMsgHistoryIndex();

        // alert_message_silence
        this.createAlertMsgSilenceIndex();

        sw.stop();
        log.info("[createRedisSearchIndex] [finshed] [Run Time: {}]", sw.toString());
    }

    /**
     * create Dc Index
     */
    private void createDcIndex() {
        log.info("[createDcIndex] [begin]");

        String indexName = "dcIndex";

        // query Existing Index List
        List<String> existingIndexList = this.queryExistingIndexList();

        boolean indexExist = existingIndexList.contains(indexName);
        log.info("[createDcIndex] [index is exist: {}]", indexExist);
        if (indexExist) this.jedisClient.ftDropIndex(indexName);

        IndexOptions indexOptions = IndexOptions.defaultOptions().setDefinition(
                new IndexDefinition(IndexDefinition.Type.JSON).setPrefixes(new String[]{"dc:"})
        );

        Schema schema = new Schema()
                .addSortableNumericField("$.id").as("id")
                .addSortableTextField("$.name", 1).as("name")
                .addSortableTextField("$.state", 1).as("state")
                .addTextField("$.location", 1);
        // create index
        this.jedisClient.ftCreate(indexName, indexOptions, schema);

        log.info("[createDcIndex] [finshed]");
    }

    /**
     * create Asset Index
     */
    private void createAssetIndex() {
        log.info("[createAssetIndex] [begin]");

        String indexName = "assetIndex";

        // query Existing Index List
        List<String> existingIndexList = this.queryExistingIndexList();

        boolean indexExist = existingIndexList.contains(indexName);
        log.info("[createDcIndex] [index is exist: {}]", indexExist);
        if (indexExist) this.jedisClient.ftDropIndex(indexName);

        IndexOptions indexOptions = IndexOptions.defaultOptions().setDefinition(
                new IndexDefinition(IndexDefinition.Type.JSON).setPrefixes(new String[]{"asset_asset:"})
        );

        Schema schema = new Schema()
                .addSortableNumericField("$.id").as("id")
                .addSortableTextField("$.name", 1).as("name")
                .addSortableTextField("$.sn", 1).as("sn")
                .addSortableTextField("$.manageIp", 1).as("manageIp")
                .addSortableNumericField("$.stateId").as("stateId")
                .addSortableNumericField("$.modelId").as("modelId")
                .addSortableNumericField("$.typeId").as("typeId");
        // create index
        this.jedisClient.ftCreate(indexName, indexOptions, schema);

        log.info("[createAssetIndex] [finshed]");
    }

    /**
     * create Model Index
     */
    private void createModelIndex() {
        log.info("[createModelIndex] [begin]");

        String indexName = "modelIndex";

        // query Existing Index List
        List<String> existingIndexList = this.queryExistingIndexList();

        boolean indexExist = existingIndexList.contains(indexName);
        log.info("[createModelIndex] [index is exist: {}]", indexExist);
        if (indexExist) this.jedisClient.ftDropIndex(indexName);

        IndexOptions indexOptions = IndexOptions.defaultOptions().setDefinition(
                new IndexDefinition(IndexDefinition.Type.JSON).setPrefixes(new String[]{"asset_model:"})
        );

        Schema schema = new Schema()
                .addSortableNumericField("$.id").as("id")
                .addSortableNumericField("$.brandId").as("brandId")
                .addSortableTextField("$.name", 1).as("name");
        // create index
        this.jedisClient.ftCreate(indexName, indexOptions, schema);

        log.info("[createModelIndex] [finshed]");
    }

    /**
     * create Project Index
     */
    private void createProjectIndex() {
        log.info("[createProjectIndex] [begin]");

        String indexName = "projectIndex";

        // query Existing Index List
        List<String> existingIndexList = this.queryExistingIndexList();

        boolean indexExist = existingIndexList.contains(indexName);
        log.info("[createDcIndex] [index is exist: {}]", indexExist);
        if (indexExist) this.jedisClient.ftDropIndex(indexName);

        IndexOptions indexOptions = IndexOptions.defaultOptions().setDefinition(
                new IndexDefinition(IndexDefinition.Type.JSON).setPrefixes(new String[]{"monitor_project:"})
        );
        Schema schema = new Schema()
                .addSortableNumericField("$.id").as("id")
                .addSortableTextField("$.name", 1).as("name");
        // create index
        this.jedisClient.ftCreate(indexName, indexOptions, schema);

        log.info("[createProjectIndex] [finshed]");
    }

    /**
     * create Module Index
     */
    private void createModuleIndex() {
        log.info("[createModuleIndex] [begin]");

        String indexName = "moduleIndex";

        // query Existing Index List
        List<String> existingIndexList = this.queryExistingIndexList();

        boolean indexExist = existingIndexList.contains(indexName);
        log.info("[createDcIndex] [index is exist: {}]", indexExist);
        if (indexExist) this.jedisClient.ftDropIndex(indexName);

        IndexOptions indexOptions = IndexOptions.defaultOptions().setDefinition(
                new IndexDefinition(IndexDefinition.Type.JSON).setPrefixes(new String[]{"monitor_module:"})
        );
        Schema schema = new Schema()
                .addSortableNumericField("$.id").as("id")
                .addSortableTextField("$.name", 1).as("name");
        // create index
        this.jedisClient.ftCreate(indexName, indexOptions, schema);

        log.info("[createModuleIndex] [finshed]");
    }

    /**
     * create Endpoint Index
     */
    private void createEndpointIndex() {
        log.info("[createEndpointIndex] [begin]");

        String indexName = "endpointIndex";

        // query Existing Index List
        List<String> existingIndexList = this.queryExistingIndexList();

        boolean indexExist = existingIndexList.contains(indexName);
        log.info("[createDcIndex] [index is exist: {}]", indexExist);
        if (indexExist) this.jedisClient.ftDropIndex(indexName);

        IndexOptions indexOptions = IndexOptions.defaultOptions().setDefinition(
                new IndexDefinition(IndexDefinition.Type.JSON).setPrefixes(new String[]{"monitor_endpoint:"})
        );

        Schema schema = new Schema()
                .addSortableNumericField("$.id").as("id")
                .addSortableTextField("$.name", 1).as("name")
                .addSortableNumericField("$.assetId").as("assetId")
                .addSortableNumericField("$.moduleId").as("moduleId");
        // create index
        this.jedisClient.ftCreate(indexName, indexOptions, schema);

        log.info("[createEndpointIndex] [finshed]");
    }

    /**
     * create AlertRule Index
     */
    private void createAlertRuleIndex() {
        log.info("[createAlertRuleIndex] [begin]");

        String indexName = "ruleIndex";

        // query Existing Index List
        List<String> existingIndexList = this.queryExistingIndexList();

        boolean indexExist = existingIndexList.contains(indexName);
        log.info("[createDcIndex] [index is exist: {}]", indexExist);
        if (indexExist) this.jedisClient.ftDropIndex(indexName);

        IndexOptions indexOptions = IndexOptions.defaultOptions().setDefinition(
                new IndexDefinition(IndexDefinition.Type.JSON).setPrefixes(new String[]{"alert_rule:"})
        );

        Schema schema = new Schema()
                .addSortableNumericField("$.id").as("id")
                .addSortableTextField("$.name", 1).as("name")
                .addSortableNumericField("$.severityId").as("severityId");
        // create index
        this.jedisClient.ftCreate(indexName, indexOptions, schema);

        log.info("[createAlertRuleIndex] [finshed]");
    }


    /**
     * create Alert Msg Active Index
     */
    private void createAlertMsgActiveIndex() {
        log.info("[createAlertMsgActiveIndex] [begin]");

        String indexName = "activeAlertMsgIndex";

        // query Existing Index List
        List<String> existingIndexList = this.queryExistingIndexList();

        boolean indexExist = existingIndexList.contains(indexName);
        log.info("[createDcIndex] [index is exist: {}]", indexExist);
        if (indexExist) this.jedisClient.ftDropIndex(indexName);

        IndexOptions indexOptions = IndexOptions.defaultOptions().setDefinition(
                new IndexDefinition(IndexDefinition.Type.JSON).setPrefixes(new String[]{"alert_message_active:"})
        );

        Schema schema = new Schema()
                .addSortableNumericField("$.id").as("id")
                .addSortableNumericField("$.ruleId").as("ruleId")
                .addSortableNumericField("$.assetId").as("assetId")
                .addSortableNumericField("$.endpointId").as("endpointId")
                .addTextField("$.summary", 1).as("summary")
                .addTextField("$.labels", 1).as("labels");
        // create index
        this.jedisClient.ftCreate(indexName, indexOptions, schema);

        log.info("[createAlertMsgActiveIndex] [finshed]");
    }

    /**
     * create Alert Msg History Index
     */
    private void createAlertMsgHistoryIndex() {
        log.info("[createAlertMsgHistoryIndex] [begin]");

        String indexName = "historyAlertMsgIndex";

        // query Existing Index List
        List<String> existingIndexList = this.queryExistingIndexList();

        boolean indexExist = existingIndexList.contains(indexName);
        log.info("[createDcIndex] [index is exist: {}]", indexExist);
        if (indexExist) this.jedisClient.ftDropIndex(indexName);

        IndexOptions indexOptions = IndexOptions.defaultOptions().setDefinition(
                new IndexDefinition(IndexDefinition.Type.JSON).setPrefixes(new String[]{"alert_message_history:"})
        );

        Schema schema = new Schema()
                .addSortableNumericField("$.id").as("id")
                .addSortableNumericField("$.ruleId").as("ruleId")
                .addSortableNumericField("$.assetId").as("assetId")
                .addSortableNumericField("$.endpointId").as("endpointId")
                .addTextField("$.summary", 1).as("summary")
                .addTextField("$.labels", 1).as("labels");
        // create index
        this.jedisClient.ftCreate(indexName, indexOptions, schema);

        log.info("[createAlertMsgHistoryIndex] [finshed]");
    }

    /**
     * create Alert Msg Silence Index
     */
    private void createAlertMsgSilenceIndex() {
        log.info("[createAlertMsgHistoryIndex] [begin]");

        String indexName = "silenceAlertMsgIndex";

        // query Existing Index List
        List<String> existingIndexList = this.queryExistingIndexList();

        boolean indexExist = existingIndexList.contains(indexName);
        log.info("[createDcIndex] [index is exist: {}]", indexExist);
        if (indexExist) this.jedisClient.ftDropIndex(indexName);

        IndexOptions indexOptions = IndexOptions.defaultOptions().setDefinition(
                new IndexDefinition(IndexDefinition.Type.JSON).setPrefixes(new String[]{"alert_message_silence:"})
        );

        Schema schema = new Schema()
                .addSortableNumericField("$.id").as("id")
                .addSortableNumericField("$.ruleId").as("ruleId")
                .addSortableNumericField("$.assetId").as("assetId")
                .addSortableNumericField("$.endpointId").as("endpointId")
                .addTextField("$.summary", 1).as("summary")
                .addTextField("$.labels", 1).as("labels");
        // create index
        this.jedisClient.ftCreate(indexName, indexOptions, schema);

        log.info("[createAlertMsgHistoryIndex] [finshed]");
    }

    public static class RedisSearchUtilBuilder {

        private RedisSearchUtil redisSearchUtil = new RedisSearchUtil();

        public static RedisSearchUtilBuilder builder() {
            return new RedisSearchUtilBuilder();
        }

        public RedisSearchUtilBuilder jedisPooled(JedisPooled jedisClient) {
            this.redisSearchUtil.setJedisClient(jedisClient);
            return this;
        }

        public RedisSearchUtil build() {
            return this.redisSearchUtil;
        }
    }

}
