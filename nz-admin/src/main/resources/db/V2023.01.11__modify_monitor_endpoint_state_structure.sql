
/**
        1. monitor_endpoint_state add column msg
 */

ALTER TABLE `monitor_endpoint_state`
ADD COLUMN  `msg` varchar(512) NOT NULL DEFAULT '' COMMENT '错误信息';

