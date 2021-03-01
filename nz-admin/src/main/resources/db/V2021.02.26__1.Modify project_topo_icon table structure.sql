/* project_topo_icon 新增unit字段*/
ALTER TABLE `project_topo_icon` 
MODIFY COLUMN `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'icon名称 为空时使用 文件名' AFTER `id`,
ADD COLUMN `unit` varchar(64) NOT NULL COMMENT 'icon组' AFTER `type`;