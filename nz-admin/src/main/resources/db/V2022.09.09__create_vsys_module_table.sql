/**

        1. asset_model add column tsg_appliance
        2. create vsys_task table
        3. create vsys_result table
        4. create vsys_asset_rel table

 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


ALTER TABLE `asset_model`
ADD COLUMN IF NOT EXISTS `tsg_appliance` int(10) NOT NULL DEFAULT 0 COMMENT '默认：0 ;   0：非tsg 设备 1：tsg 设备，支持vsys';

-- ----------------------------
-- Table structure for vsys_task
-- ----------------------------
DROP TABLE IF EXISTS `vsys_task`;
CREATE TABLE `vsys_task`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `task_uuid` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '任务唯一标识',
  `type` int(1) NOT NULL COMMENT '任务类型 1：add 2：update 3：delete',
  `vsys_id` int(10) NOT NULL COMMENT 'vsys id',
  `device_tag` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '设备组',
  `params` varchar(4096) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '参数',
  `concurrency` int(10) NOT NULL DEFAULT 0 COMMENT '并发数 默认： 0 ，不限制',
  `retry` int(10) NOT NULL DEFAULT 0 COMMENT '失败重试次数 默认： 0 ',
  `progress` double(5, 2) NOT NULL COMMENT '进度 默认： 0 ',
  `total` int(10) NOT NULL DEFAULT 0 COMMENT '下发设备总数量 默认： 0 ',
  `success` int(10) NOT NULL DEFAULT 0 COMMENT '成功数量 默认： 0 ',
  `failed` int(10) NOT NULL DEFAULT 0 COMMENT '失败数量 默认： 0 ',
  `ts` bigint(20) NOT NULL COMMENT '时间戳',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;


-- ----------------------------
-- Table structure for vsys_result
-- ----------------------------
DROP TABLE IF EXISTS `vsys_result`;
CREATE TABLE `vsys_result`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `task_id` int(10) NOT NULL COMMENT '任务id',
  `asset_id` int(10) NOT NULL COMMENT '资产id',
  `vsys_id` int(10) NOT NULL COMMENT 'vsys id',
  `type` int(10) NOT NULL COMMENT '任务类型 1：add 2：update 3：delete',
  `state` int(10) NOT NULL COMMENT '状态 0：失败 1：成功',
  `msg` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '执行信息',
  `num` int(10) NOT NULL DEFAULT 1 COMMENT '执行第几次 默认： 1',
  `ts` bigint(20) NOT NULL COMMENT '时间戳',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;


-- ----------------------------
-- Table structure for vsys_asset_rel
-- ----------------------------
DROP TABLE IF EXISTS `vsys_asset_rel`;
CREATE TABLE `vsys_asset_rel`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `asset_id` int(10) NOT NULL COMMENT '资产id',
  `vsys_id` int(10) NOT NULL COMMENT 'vsys id',
  `uts` bigint(20) NOT NULL COMMENT '更新时间戳',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;


SET FOREIGN_KEY_CHECKS = 1;
