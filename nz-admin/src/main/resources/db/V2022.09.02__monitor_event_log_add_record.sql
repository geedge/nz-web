
/**
 * 2022年9月2日13:56:27
 * 1、event 列增加 唯一索引
 * 2、新增 record 记录
 */

ALTER TABLE `monitor_event_log` 
ADD UNIQUE INDEX  `uniq_event`(`event`);

REPLACE INTO `monitor_event_log`(`id`, `event`, `counter`) VALUES (NULL, 'record', 0);
