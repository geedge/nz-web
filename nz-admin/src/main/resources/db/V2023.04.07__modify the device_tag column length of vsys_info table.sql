/**

        1. modify the device_tag column length of vsys_info table

 */

ALTER TABLE `vsys_info`
MODIFY COLUMN  `device_tag` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '设备组';

