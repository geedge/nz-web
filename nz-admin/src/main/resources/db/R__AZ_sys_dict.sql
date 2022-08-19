/**
 *  按照时间 描述记录每次变化内容
 * eg:2021年11月27日  新建sys_dict 表重复执行sql文件
 * 
 *  2021年11月27日 新增图表数据源内置类型
 */

REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表数据源', 'chartDatasource', 1, 'metrics', '', 0, '指标数据源', 0, 1, '2021-11-27 10:56:27', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表数据源', 'chartDatasource', 2, 'logs', '', 0, '日志数据源', 0, 1, '2021-11-27 10:56:27', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表数据源', 'chartDatasource', 3, 'system', '', 0, '系统数据源', 0, 1, '2021-11-27 10:56:27', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表数据源', 'chartDatasource', 4, 'misc', '', 0, 'misc数据源', 0, 1, '2021-11-27 10:56:27', NULL);

Delete from sys_dict where code = 'chartType';

REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表类型', 'chartType', 8, 'text', '', 8, '告警', 0, 1, '2020-05-27 17:50:33', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表类型', 'chartType', 9, 'pie', '', 9, '饼图', 0, 1, '2021-02-05 10:57:33', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表类型', 'chartType', 10, 'group', '', 10, '组', 0, 0, '2021-04-07 14:30:18', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表类型', 'chartType', 11, 'diagram', '', 0, 'diagram图表', 0, 1, '2021-06-24 17:53:19', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表类型', 'chartType', 12, 'log', '', 0, 'logs图表', 0, 1, '2021-06-24 17:53:19', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表类型', 'chartType', 1, 'line', '', 1, '折线图', 0, 1, '2019-12-13 17:53:19', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表类型', 'chartType', 2, 'bar', '', 2, '条形图', 0, 1, '2019-12-13 17:53:19', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表类型', 'chartType', 3, 'table', '', 3, '表格', 0, 1, '2019-12-13 17:53:19', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表类型', 'chartType', 4, 'area', '', 4, '堆栈面积图', 0, 1, '2019-12-13 17:53:19', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表类型', 'chartType', 5, 'stat', '', 5, 'singleStat', 0, 1, '2020-03-31 10:57:30', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表类型', 'chartType', 6, 'url', '', 6, 'url', 0, 1, '2020-03-31 10:57:33', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表类型', 'chartType', 7, 'alertList', '', 0, '告警', 0, 1, '2020-05-27 17:50:33', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表类型', 'chartType', 13, 'point', '', 0, '点', 0, 1, '2020-05-27 17:50:33', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表类型', 'chartType', 14, 'gauge', '', 0, 'gauge', 0, 1, '2020-05-27 17:50:33', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表类型', 'chartType', 15, 'treemap', '', 0, '树状图', 0, 1, '2020-05-27 17:50:33', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表类型', 'chartType', 16, 'assetInfo', '', 0, '资产详情图', 0, 1, '2020-05-27 17:50:33', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表类型', 'chartType', 17, 'endpointInfo', '', 0, 'endpoint图', 0, 1, '2020-05-27 17:50:33', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表类型', 'chartType', 18, 'topology', '', 0, '拓扑图', 0, 1, '2020-05-27 17:50:33', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表类型', 'chartType', 19, 'map', '', 0, '图', 0, 1, '2020-05-27 17:50:33', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表类型', 'chartType', 21, 'hexagon', '', 0, 'hexagon', 0, 1, '2020-05-27 17:50:33', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (null, '图表类型', 'chartType', 23, 'clock', '', 0, 'clock', 0, 1, '2020-05-27 17:50:33', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (NULL, '图表类型', 'chartType', 24, 'bubble', '', 0, 'bubble', 0, 1, '2022-07-20 10:00:00', NULL);
REPLACE INTO `sys_dict`(`id`, `name`, `type`, `code`, `value`, `i18n_code`, `order_num`, `remark`, `del_flag`, `operator`, `op_time`, `seq`) VALUES (NULL, '图表类型', 'chartType', 25, 'sankey', '', 0, 'sankey', 0, 1, '2022-08-17 10:00:00', NULL);
