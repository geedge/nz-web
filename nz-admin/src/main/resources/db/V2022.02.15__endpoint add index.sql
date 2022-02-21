/**
 *
 *    优化endpoint列表查询 缺失索引表添加索引
 *
 **/

CREATE INDEX IF NOT EXISTS idx_endpoint_id ON monitor_endpoint_config(`endpoint_id`) USING BTREE;
CREATE INDEX IF NOT EXISTS idx_endpoint_id ON monitor_endpoint_state(`endpoint_id`) USING BTREE;
CREATE INDEX IF NOT EXISTS idx_config_id ON monitor_endpoint_state(`config_id`) USING BTREE;


