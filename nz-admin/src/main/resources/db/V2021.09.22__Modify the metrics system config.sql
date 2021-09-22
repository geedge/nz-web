/**
 *  1. sys_config 增加 prometheus 联邦模式开关，只在 metrics本地存储时有效，默认为 2 （remote write）
 *  2. prom_cmd_tmpl 增加 --enable-feature=remote-write-receiver 用以 remote_write 写入 prometheus 数据库
 */

REPLACE INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'prometheus_fed_mode', '2', 1, 'prometheus数据采集模式，只在 metrics本地存储时有效,1：global 使用federation job 集中收集各个 per-datacenter 数据,2：global使用 remote_write 集中收集各个 per-datacenter数据');
REPLACE INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'prom_cmd_tmpl', 'OPTION=\" --config.file=\'/opt/nezha/nz-agent/prometheus/prometheus.yml\' --web.listen-address=\'0.0.0.0:10091\' --web.enable-admin-api --enable-feature=remote-write-receiver --storage.tsdb.path=\'/data/prometheusData\'  --web.enable-lifecycle --storage.tsdb.retention.time=${storageDays}d \"', 1, 'prometheus 启动参数模板');
