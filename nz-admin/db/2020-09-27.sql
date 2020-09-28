/**
 * 修改表字段类型
 * 增加 endpoint 导入导出表头
 */
ALTER TABLE `alert_message`
MODIFY COLUMN `summary`  text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '告警摘要' AFTER `severity`,
MODIFY COLUMN `description`  text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '告警描述' AFTER `summary`,
MODIFY COLUMN `labels`  text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '告警labels信息' AFTER `description`;

INSERT INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'endpoint_export_header', '{\"cn\":{\"*系统\":\"系统名称(必填) \\n示例: Nezha\",\"*组件\":\"组件名称(必填) \\n示例: node_exporter\",\"*组件类型\":\"组件类型(必填) 可选: HTTP、SNMP 当type = SNMP时，组件必须存在 \\n示例: HTTP\",\"资产序列号\":\"资产序列号(非必填) 与 主机地址 不能同时为空 \\n示例: A0420200101\",\"端口号\":\"端口号 范围0-65535 \\n示例: 9100\",\"参数\":\"端点查询参数，JSON格式， \\n示例: {\\\"key\\\":\\\"value\\\"}\",\"*路径\":\"路径（必填） 请求路径，\\n示例: /metrics\",\"主机地址\":\"主机地址（非必填） 与 资产序列号 不能同时为空 \\n示例; 127.0.0.1\"},\"en\":{\"*Project Name\":\"Project Name(Required) \\nExample: Nezha\",\"*Module Name\":\"Module Name(Required) \\nExample: node_exporter\",\"*Module Type\":\"Module type(Required) Optional: HTTP or SNMP，When type = SNMP, the component must exist \\nExample: HTTP\",\"Sn\":\"Asset serial number(Not required) And host address cannot be empty at the same time \\nExample: A0420200101\",\"Port\":\"Port number range 0-65535 \\nExample: 9100\",\"Parameter\":\"Endpoint query parameters, JSON format  \\nExample: {\\\"key\\\":\\\"value\\\"}\",\"*Path\":\"Path (required) Request path \\nExample: /metrics\",\"Host\":\"Host address (not required) and asset serial number cannot be empty at the same time \\nExample; 127.0.0.1\"}}', 1, NULL);