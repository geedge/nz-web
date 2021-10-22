/**
 *  1、visual chart 增加xy字段
 */

TRUNCATE TABLE `asset_state_conf`;
-- ----------------------------
-- Records of asset_state_conf
-- ----------------------------
INSERT INTO `asset_state_conf` VALUES (1, 'Not in stock', '0', '0', '0', 'The asset is not in stock', '1');
INSERT INTO `asset_state_conf` VALUES (2, 'In use', '1', '1', '1', 'The asset is in use', '1');
INSERT INTO `asset_state_conf` VALUES (3, 'Idle', '1', '0', '1', 'The asset is suspended for now, and you can put it in use later', '1');
INSERT INTO `asset_state_conf` VALUES (4, 'Repair', '1', '1', '0', 'The asset is suspended because of hardware fault or software upgrade', '1');
INSERT INTO `asset_state_conf` VALUES (5, 'Scrapped', '1', '0', '0', 'The asset is permanently suspended', '1');


TRUNCATE TABLE `alert_severity_conf`;
-- ----------------------------
-- Records of alert_severity_conf
-- ----------------------------
INSERT INTO `alert_severity_conf` VALUES (1, 'P1', '#eb1010', 1, 'Critical, alert that can cause business paralysis');
INSERT INTO `alert_severity_conf` VALUES (2, 'P2', '#f89984', 2, 'Major, alert that requires urgent attention, but does not affect the business yet');
INSERT INTO `alert_severity_conf` VALUES (3, 'P3', '#f7ba78', 3, 'Minor, alert that need to be dealt with, but not urgent');