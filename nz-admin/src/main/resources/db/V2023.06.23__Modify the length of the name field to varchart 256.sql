/**
 * 数据库表 `name` 字段统一为 varchar(256)
 * 说明：
    ROW_FORMAT = Compact; name 数据类型为 varchar(255) CHARACTER utf8
    ROW_FORMAT = Dynamic; name 数据类型为 varchar(256)
 */

ALTER TABLE `alert_notify_method`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `alert_rule`
MODIFY COLUMN IF EXISTS `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `alert_severity_conf`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `alert_silence_conf`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `asset_asset`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `asset_brand`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `asset_field_group`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `asset_field_meta`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `asset_model`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `asset_state_conf`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `asset_type_conf`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `cabinet`
MODIFY COLUMN IF EXISTS `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `dc`
MODIFY COLUMN IF EXISTS `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `int_alert_rule`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `int_metadata`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `int_module`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `ipam_ip`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `ipam_subnet`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `link`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `monitor_endpoint`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `monitor_module`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `monitor_project`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `project_topo_icon`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `record_rule`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `snmp_credential`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `sys_api_key`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `sys_role`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `sys_user`
MODIFY COLUMN IF EXISTS `username` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '用户名';


ALTER TABLE `visual_chart_element`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `visual_dashboard`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';


ALTER TABLE `visual_expression_tmpl`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '模板名称',
MODIFY COLUMN IF EXISTS `gname` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'default' COMMENT '组名，默认：default';


ALTER TABLE `visual_playlist`
MODIFY COLUMN IF EXISTS `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称';
