/**

    1. vsys_result 新增 req_param 列, 该内容包含 sf ruleSet chain 三个接口请求参数记录
    2. vsys_task device_tag 字段调整长度

 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `vsys_result`
ADD COLUMN   `req_param` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL  COMMENT '接口请求参数';

ALTER TABLE `vsys_task`
MODIFY COLUMN `device_tag` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL  COMMENT '设备组';

SET FOREIGN_KEY_CHECKS = 1;
