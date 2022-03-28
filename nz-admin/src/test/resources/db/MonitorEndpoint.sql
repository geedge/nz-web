truncate monitor_endpoint;

INSERT INTO `monitor_endpoint`(`id`, `name`, `module_id`, `asset_id`, `seq`) VALUES
(1, 'node-exporter-192.168.44.17', 1, 9, ''),
(2, 'node-exporter-192.168.44.18', 1, 11, ''),
(3, 'NODE-EXPORTER-BOL-PE-T9K001-NPB07', 5, 393, ''),
(4, 'NODE-EXPORTER-BOL-PE-T9K001-NPB08', 5, 395, ''),
(5, 'NODE-EXPORTER-BOL-IGW-T9K001-NPB01', 5, 397, ''),
(6, 'NODE-EXPORTER-BOL-IGW-T9K001-NPB02', 5, 399, ''),
(7, 'NODE-EXPORTER-BOL-IGW-T9K001-NPB03', 5, 401, ''),
(8, 'CM-API-CM Server-1', 2, 245, ''),
(9, 'CM-API-CM Server-2', 2, 247, '');
