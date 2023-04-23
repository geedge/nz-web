/**

    1. alert_rule 新增 dashboard_id 列

 */

ALTER TABLE `alert_rule`
ADD COLUMN IF NOT EXISTS  `dashboard_id` int(10) NOT NULL DEFAULT -1 COMMENT 'dashboard_id,  默认：-1（没有关联dashboard）';

