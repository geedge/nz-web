/**
 *  create table monitor_endpoint_agent
 */
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for monitor_endpoint_agent
-- ----------------------------
DROP TABLE IF EXISTS `monitor_endpoint_agent`;
CREATE TABLE `monitor_endpoint_agent`  (
    `endpoint_id` int(10) NOT NULL,
    `agent_id` int(10) NOT NULL,
    PRIMARY KEY (`endpoint_id`) USING BTREE
) ENGINE = InnoDB ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;