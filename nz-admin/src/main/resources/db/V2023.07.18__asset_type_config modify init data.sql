/**
 * asset_type_conf 内置数据调整
 * 1. id=9,name=Other 数据 id 改为 999. 调整 asset_model&asset_asset 表引用关系
 * 2. 新增 id=9,name=TSG 记录
 * 3. TSG-X & TSG-9140 修改 type 为 TSG
 */

UPDATE asset_type_conf SET id = 99999 WHERE id = 9;
UPDATE asset_model SET type_id = 99999 WHERE type_id = 9;
UPDATE asset_asset SET type_id = 99999 WHERE type_id = 9;


INSERT INTO `asset_type_conf`(`id`, `pid`, `vm`, `vmh`, `name`, `remark`, `build_in`, `auth_protocol`, `snmp_enable`, `snmp_collect`, `ssh_collect`, `ssh_collect_script`, `client_enable`, `icon`)
VALUES (9, 0, '0', '0', 'TSG', '', '1', 1, 1, 0, 0, '', 0, '');


UPDATE asset_model SET type_id = 9 WHERE name = "TSG-X";
UPDATE asset_model SET type_id = 9 WHERE name = "TSG-9140";
