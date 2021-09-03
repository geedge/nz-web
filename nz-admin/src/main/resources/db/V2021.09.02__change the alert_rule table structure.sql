/**
 *  1. 增加 alert rule
 */


ALTER TABLE  `alert_rule`
ADD COLUMN  `type` int(1) NOT NULL COMMENT '1：Metrics\r\n\r\n2：Logs\r\n\r\n3：SNMP TRAP',
ADD COLUMN  `inr` int(10) NOT NULL DEFAULT 0 COMMENT '默认值：0，单位：s\r\n\r\n不小于 15 \r\n\r\n当 < 15, 则为 Scrape interval ',
ADD COLUMN  `timeout` int(10) NOT NULL DEFAULT 0 COMMENT '默认值：0，单位：s\r\n\r\n不小于 15 \r\n\r\n当 < 15, 则为 Scrape timeout';
