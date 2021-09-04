/**
 * 将之前版本 内置的 snmp trap 改为 type = 3
 */ 

UPDATE `alert_rule` SET `name` = 'Default snmptrap', `expr` = '1.3.6.1', `last` = 3600, `summary` = '{{$message}}', `description` = '{{$variables}}', `receiver` = NULL, `build_in` = '0', `operator` = '', `threshold` = NULL, `unit` = 0, `seq` = NULL, `severity_id` = 3, `method` = NULL, `state` = 1, `type` = 3, `inr` = 0, `timeout` = 0 WHERE `id` = 1;
