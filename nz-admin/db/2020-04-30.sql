/*
   idc_traffic表结构变更
   1. 增加字段如下： host、port、version、community、auth
   2. 删除字段如下： asset_id
*/

-- 增加字段
alter table idc_traffic add column host varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'ip地址';
alter table idc_traffic add column port  int(10) NOT NULL COMMENT 'snmp端口 默认为161';
alter table idc_traffic add column version int(1) NOT NULL COMMENT 'snmp版本 默认为2';
alter table idc_traffic add column community varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'snmp团体名 默认为public';
alter table idc_traffic add column auth varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'snmp认证信息 json格式';

-- 删除字段
alter table idc_traffic drop asset_id;