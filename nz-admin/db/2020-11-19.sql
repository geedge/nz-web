/**
 * 新增 notification_script notification_script表
 */

 -- ----------------------------
-- Table structure for notification_script
-- ----------------------------
DROP TABLE IF EXISTS `notification_script`;
 CREATE TABLE `notification_script` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL COMMENT '通知方式名称，唯一主键',
  `file_path` varchar(256) DEFAULT NULL COMMENT '脚本路径，路径为 nz-web所在服务器路径',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4;


 -- ----------------------------
-- Table structure for user_notification
-- ----------------------------
DROP TABLE IF EXISTS `user_notification`;
CREATE TABLE `user_notification` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `user_id` int(10) NOT NULL COMMENT '用户id',
  `script_id` int(10) NOT NULL COMMENT '脚本id',
  `account` varchar(256) NOT NULL COMMENT '通知账号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4;