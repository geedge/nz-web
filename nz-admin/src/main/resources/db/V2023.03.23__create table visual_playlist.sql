
/**
 *  create table visual_playlist
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for visual_playlist
-- ----------------------------
DROP TABLE IF EXISTS `visual_playlist`;
CREATE TABLE `visual_playlist`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `intvl` int(10) NOT NULL DEFAULT 300 COMMENT '间隔; 单位： s; 默认：300',
  `dashboard_ids` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'json 数组，例：[1,2]',
  `uid` int(10) NOT NULL COMMENT '更新人 id',
  `uts` bigint(20) NOT NULL COMMENT '更新时间戳',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
