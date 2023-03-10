/**

        1. create int_module table
        2. create int_metadata table
        3. create int_alert_rule table

 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for int_module
-- ----------------------------
DROP TABLE IF EXISTS `int_module`;
CREATE TABLE `int_module`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `remark` VARCHAR(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '备注',
  `icon` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL  COMMENT '图标',
  `configuration` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL  COMMENT '配置, markdown 格式',
  `tempId` int(10) NOT NULL DEFAULT -1 COMMENT '仪表盘模板id ',
  `status` int(1) NOT NULL DEFAULT 1 COMMENT '状态, 0：隐藏 ,1：显示,默认：1 ',
  `build_in` int(1) NOT NULL DEFAULT 0 COMMENT '可选值：0：非内置 1：内置 默认：0 ',
  `uid` int(10) NOT NULL COMMENT '更新人id',
  `uts` bigint(20) NOT NULL COMMENT '更新时间戳',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;


-- ----------------------------
-- Table structure for int_metadata
-- ----------------------------
DROP TABLE IF EXISTS `int_metadata`;
CREATE TABLE `int_metadata`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '指标名称',
  `int_module_id` int(10) NOT NULL COMMENT '集成模块id',
  `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '指标描述',
  `type` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '可选值: counter, gauge ,histogram,summary ',
  `status` int(1) NOT NULL DEFAULT 1 COMMENT '状态, 0：隐藏 ,1：显示,默认：1 ',
  `build_in` int(1) NOT NULL DEFAULT 0 COMMENT '可选值：0：非内置 1：内置 默认：0 ',
  `uid` int(10) NOT NULL COMMENT '更新人id',
  `uts` bigint(20) NOT NULL COMMENT '更新时间戳',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;


-- ----------------------------
-- Table structure for int_alert_rule
-- ----------------------------
DROP TABLE IF EXISTS `int_alert_rule`;
CREATE TABLE `int_alert_rule`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `int_module_id` int(10) NOT NULL COMMENT '集成模块id',
  `name` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '告警名称',
  `type` int(1) NOT NULL COMMENT '类型:1：Metrics 2：Logs 3：SNMP TRAP',
  `inr` int(10) NOT NULL DEFAULT 0 COMMENT '执行周期: 默认值：0，单位：s 不小于 15 当 < 15, 则为 Scrape timeout',
  `timeout` int(10) NOT NULL DEFAULT 0  COMMENT '自动失效超时时间: 默认值：0，单位：s 不小于 15 当 < 15, 则为 Scrape timeout',
  `expr` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '运算表达式',
  `cond` VARCHAR(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '判断条件',
  `ext_labels` VARCHAR(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '扩展标签',
  `unit` int(10) NOT NULL COMMENT '单位',
  `last` int(10) COMMENT '持续时间',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL  COMMENT '告警摘要 ',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL  COMMENT '告警详细描述 ',
  `receiver` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '通知用户 ',
  `method` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '通知方式 ',
  `state` INT(1) NOT NULL DEFAULT 1 COMMENT '告警规则状态,默认：开启, 0：关闭 1：开启',
  `auto_expired` INT(1) NOT NULL DEFAULT 1 COMMENT '是否自动失效: 默认：1 1：开启自动失效判断 0：关闭自动失效判断',
  `sched_enable` INT(1) NOT NULL DEFAULT 0 COMMENT '定时是否开启: 默认：0 1：开启 0：关闭',
  `sched_days` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '1,2,3,4,5,6,7' COMMENT '定时开启天: 默认：1,2,3,4,5,6,7 sched_enable = 1 时，不能为空',
  `sched_stime` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '00:00' COMMENT '定时开启 开始时间 ',
  `sched_etime` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '23:59' COMMENT '定时开启 关闭时间 ',
  `notify_active` int(1) NOT NULL DEFAULT 1 COMMENT 'active通知开关 默认：1 1：开启 0：关闭',
  `notify_expired` int(1) NOT NULL DEFAULT 1 COMMENT 'expired通知开关 默认：1 1：开启 0：关闭',
  `trb_shot` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL  COMMENT 'trouble shooting',
  `build_in` char(1) NOT NULL DEFAULT 0 COMMENT '是否内置 1：内置，0：非内置，默认：0 ',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;


SET FOREIGN_KEY_CHECKS = 1;
