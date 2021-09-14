/**
 *  1. alert_rule timeout 默认 300
 *  2. 把之前 timeout = 0 的数据调整为 300
 */

UPDATE alert_rule SET timeout = 300 WHERE timeout = 0;

ALTER TABLE  `alert_rule`
MODIFY COLUMN  `timeout` int(10) NOT NULL DEFAULT 300 COMMENT '默认值：0，单位：s\r\n\r\n不小于 15 \r\n\r\n当 < 15, 则为 Scrape timeout';
