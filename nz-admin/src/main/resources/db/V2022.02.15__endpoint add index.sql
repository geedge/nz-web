/**
 *
 *    优化endpoint列表查询 缺失索引表添加索引
 *
 **/
 
ALTER TABLE `monitor_endpoint_config` 
ADD INDEX `idx_endpoint_id`(`endpoint_id`) USING BTREE;

ALTER TABLE `monitor_endpoint_state` 
ADD INDEX `idx_endpoint_id`(`endpoint_id`) USING BTREE,
ADD INDEX `idx_config_id`(`config_id`) USING BTREE;

