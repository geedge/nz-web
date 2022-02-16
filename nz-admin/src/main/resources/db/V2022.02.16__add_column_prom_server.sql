
/**
 * 2022年02月16日
 * prom_server新增name字段 历史数据name字段赋值 agent-id
 */
ALTER TABLE `prom_server`
ADD COLUMN `name` varchar(256) NOT NULL DEFAULT '' COMMENT '名称';

UPDATE `prom_server` SET `name` = concat('agent-',`id`);