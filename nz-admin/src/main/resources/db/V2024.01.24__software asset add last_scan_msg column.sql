/*
 software_asset add last_scan_msg
 */
    
ALTER TABLE `software_asset` 
    ADD COLUMN IF NOT EXISTS `last_scan_msg` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '最后一次扫描信息';