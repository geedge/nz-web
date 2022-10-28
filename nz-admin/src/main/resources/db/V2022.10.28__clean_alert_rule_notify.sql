/**
    clean deleted notify_method of alert_rule
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

UPDATE alert_rule rule
LEFT JOIN (
    SELECT
        t.id,
        ifnull( GROUP_CONCAT( anm.id ), '' ) ids
    FROM (
        SELECT
            id,
            SUBSTRING_INDEX( SUBSTRING_INDEX( `method`, ',', ht.help_topic_id + 1 ), ',', - 1 ) AS methodid
        FROM alert_rule
        INNER JOIN ( SELECT @rownum := @rownum + 1 AS help_topic_id FROM ( SELECT @rownum :=- 1 ) t, alert_rule ) ht ON ht.help_topic_id < ( LENGTH( `method` ) - LENGTH( REPLACE ( `method`, ',', '' ) ) + 1 ) WHERE LENGTH( `method` ) > 0
    ) t
    LEFT JOIN alert_notify_method anm ON t.methodid = anm.id GROUP BY t.id
) s ON rule.id = s.id
SET rule.`method` = s.ids
WHERE
    s.id IS NOT NULL;

UPDATE alert_rule set receiver = '', notify_active = 0, notify_expired = 0 where `method` = '' or `method` is null;

SET FOREIGN_KEY_CHECKS = 1;
