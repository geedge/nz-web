/**
        1. visual_panel add column var_type, tmpl_id
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


ALTER TABLE `visual_panel`
    ADD COLUMN  `var_type` INT(10) NOT NULL DEFAULT '0' COMMENT '模板或使用模板创建有效\r\n0: none\r\n1: asset\r\n2: endpoint',
    ADD COLUMN  `tmpl_id` INT(10) NOT NULL DEFAULT '-1' COMMENT '默认：-1\r\n模板创建有效，关联 visual_panel.id';


SET FOREIGN_KEY_CHECKS = 1;
