DROP TABLE IF EXISTS `alert_notify_method`;
CREATE TABLE `alert_notify_method` (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) NOT NULL COMMENT '通知方式名称',
  `file_path` varchar(255) NOT NULL COMMENT '除内置外，必填脚本路径，nz-web所在服务器路径',
  `account` varchar(255) NOT NULL COMMENT '可选值为user表属性id，name，username，email，mobile',
  `state` int(1) DEFAULT NULL COMMENT '状态0：停用1：可用',
  `build_in` int(1) NOT NULL COMMENT '可选值：0：非内置1：内置默认：0系统内置一条 email 通知',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
DROP TABLE IF EXISTS `alert_severity_conf`;

DROP TABLE IF EXISTS `alert_severity_conf`;
CREATE TABLE `alert_severity_conf`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `color` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `weight` int(3) NOT NULL COMMENT '1-100 顺序取值不重复,值越小告警级别越高',
  `remark` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `methods` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '告警消息通过什么方式发送,例：1,2,3',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

ALTER TABLE `alert_rule`
ADD COLUMN `severity_id` int(10) NULL COMMENT '告警级别id,关联ALERT_SEVERITY_CONF';

ALTER TABLE `alert_message`
ADD COLUMN `severity_id` int(10) NULL COMMENT '告警级别id,关联ALERT_SEVERITY_CONF';

ALTER TABLE `alert_rule` 
CHANGE COLUMN `alert_name` `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '告警名称' AFTER `id`;

ALTER TABLE `alert_rule` 
DROP INDEX `idx_alert_name`,
ADD INDEX `idx_name`(`name`) USING BTREE;