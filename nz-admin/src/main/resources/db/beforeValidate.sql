/**
 * event: beforeValidate
 *
 * 2023-07-13
 * 1. 删除 sys_timezone 表，清空 flyway_schema_history 中 ’R__AZ_sys_timezone.sql‘记录，避免验证失败
 *
 */

DELETE
FROM
	flyway_schema_history
WHERE
	script = "R__AZ_sys_timezone.sql";


