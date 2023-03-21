/**
  1.name 属性长度由 64 调整为 128
  2.code 属性长度由 64 调整为 128
  3.value 属性类型由 text 调整为 longtext
 */

ALTER TABLE `sys_i18n`
    MODIFY COLUMN `name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL ,
    MODIFY COLUMN `code` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL ,
    MODIFY COLUMN `value` LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL;