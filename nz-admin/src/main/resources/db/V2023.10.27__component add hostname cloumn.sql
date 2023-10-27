/*
 sys_component add hostname
 */

ALTER TABLE `sys_component`
    ADD COLUMN IF NOT EXISTS `hostname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '主机名称';