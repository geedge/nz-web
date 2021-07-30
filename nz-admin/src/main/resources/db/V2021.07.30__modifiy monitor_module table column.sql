/**
 * 1. monitor_module 删除 type 列
 * 2. sys_config 增加 module 导入导出表头信息
 */

ALTER TABLE `monitor_module` DROP COLUMN `type`;

DELETE from `sys_config` where param_key = "module_export_header";
INSERT INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'module_export_header', '{\"cn\":{\"*名称\":\"名称（必填）\\n示例：Basic\",\"*系统\":\"系统名称（必填）\\n示例：System\",\"Endpoint名称模板\":\"Endpoint名称模板 默认 {{asse.host}}-{{module.name}} \\n示例：{{asse.host}}-{{module.name}}\",\"配置\":\"配置信息 JSON 格式 \\n示例：\\\"[{\\\"type\\\":\\\"metrics\\\",\\\"config\\\":{\\\"protocol\\\":\\\"SNMP\\\",\\\"walk\\\":[\\\"1.3.6.1.2.1.1\\\"],\\\"snmpCredentialsId\\\":1,\\\"host\\\":\\\"{{asset.manageIp}}\\\"},\\\"enable\\\":1},{\\\"type\\\":\\\"logs\\\",\\\"enable\\\":1}]\\\"\",\"图表\":\"Endpoint 类型的图表模板 \\n示例：服务器模板,基本模板\",\"描述\":\"描述 \\n示例：Basic\"},\"en\":{\"*Name\":\"Name(Required) \\nExample: Basic\",\"*Project\":\"Project name(Required) \\nExample: System\",\"Endpoint name template\":\"Endpoint name template Default {{asse.host}}-{{module.name}} \\nExample: {{asse.host}}-{{module.name}}\",\"Config\":\"Configuration information JSON format\\nExample:\\\"[{\\\"type\\\":\\\"metrics\\\",\\\"config\\\":{\\\"protocol\\\":\\\"SNMP\\\",\\\" walk\\\":[\\\"1.3.6.1.2.1.1\\\"],\\\"snmpCredentialsId\\\":1,\\\"host\\\":\\\"{{asset.manageIp}}\\\"},\\\"enable\\\" :1},{\\\"type\\\":\\\"logs\\\",\\\"enable\\\":1}]\\\"\",\"Chart templates\":\"Endpoint chart templates  \\nExample: Server Template,Basic Template\",\"Description\":\"Description \\nExample: This is basic module\"}}', 1, 'module 导入导出模板');
