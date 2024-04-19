/**
 *  1. add remark column to dc
 *  2. update sys_config dc_export_header
 */

ALTER TABLE `dc`
ADD COLUMN IF NOT EXISTS `remark` varchar(256) NOT NULL DEFAULT '' COMMENT '备注';


REPLACE INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (null, 'dc_export_header', '[{\"name\":\"Name\",\"i18n\":\"overall.name\",\"comment\":\"config.dc.name.comment\"},{\"name\":\"Location\",\"i18n\":\"asset.location\",\"comment\":\"config.dc.location.comment\"},{\"name\":\"Telephone\",\"i18n\":\"config.dc.tel\",\"comment\":\"config.dc.tel.comment\"},{\"name\":\"Principal\",\"i18n\":\"config.dc.principal\",\"comment\":\"config.dc.principal.comment\"},{\"name\":\"Longitude\",\"i18n\":\"config.dc.longitude\",\"comment\":\"config.dc.longitude.comment\"},{\"name\":\"Latitude\",\"i18n\":\"config.dc.latitude\",\"comment\":\"config.dc.latitude.comment\"},{\"name\":\"State\",\"i18n\":\"overall.state\",\"comment\":\"config.dc.state.comment\"},{\"name\":\"Remark\",\"i18n\":\"overall.dc.remark\",\"comment\":\"config.dc.remark.comment\"}]', 1, 'dc 导入导出表头信息');
