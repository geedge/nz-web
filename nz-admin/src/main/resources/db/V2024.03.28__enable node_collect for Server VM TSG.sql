
/*
1. Server Virtual machine TSG 资产类型打开 node_collect 开关
 */

UPDATE `asset_type_conf` SET `node_collect` = 1 WHERE `name` IN ( "Server", "Virtual machine", "TSG" );