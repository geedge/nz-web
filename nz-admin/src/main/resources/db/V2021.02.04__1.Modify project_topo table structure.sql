/**
 *  1. 清除 project_topo 残留数据
 *  2. project_topo topo 字段类型由 text 改为 MEDIUMTEXT
 */

TRUNCATE project_topo;
ALTER TABLE project_topo MODIFY COLUMN `topo` MEDIUMTEXT DEFAULT NULL COMMENT '拓扑图配置信息';