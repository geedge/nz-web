/**
 *
 *    优化endpoint列表查询 缺失索引表添加索引
 *    update flyway_schema_history set version = '2021.12.07.1',script = 'V2021.12.07.1__endpoint add index.sql' where version ='2022.02.15'
 *
 **/
 
ALTER TABLE `monitor_endpoint_config` 
ADD INDEX `idx_endpoint_id`(`endpoint_id`) USING BTREE;

ALTER TABLE `monitor_endpoint_state` 
ADD INDEX `idx_endpoint_id`(`endpoint_id`) USING BTREE,
ADD INDEX `idx_config_id`(`config_id`) USING BTREE;

