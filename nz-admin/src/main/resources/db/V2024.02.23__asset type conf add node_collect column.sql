/*
asset_type_conf add node_collect column
 */

ALTER TABLE `asset_type_conf`
    ADD COLUMN IF NOT EXISTS `node_collect` int(1) NOT NULL DEFAULT 0 COMMENT '是否开启node_exporter采集，默认：0 0：不开启 1：开启 优先使用 node exporter获取';

