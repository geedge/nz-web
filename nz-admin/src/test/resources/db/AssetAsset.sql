truncate asset_asset;

INSERT INTO `asset_asset`(`id`, `name`, `number`, `sn`, `pid`, `type_id`, `state_id`, `brand_id`, `model_id`, `dc_id`, `cabinet_id`, `cabinet_start`, `cabinet_end`, `manage_ip`, `purchase_date`, `create_at`, `update_at`, `create_by`, `update_by`, `seq`, `auth_protocol_port`, `auth_type`, `auth_username`, `auth_pin`, `auth_pri_key`, `auth_user_tip`, `auth_pin_tip`, `snmp_credential_id`, `client_host`, `client_port`, `client_user`, `client_pin`, `client_token`, `client_enable`)
VALUES
(1, 'OAP-PE-T9K001-NPB01', '', '0220090200001', -1, 3, 2, 3, 3, 1, 1, 2, 4, '10.231.11.1', NULL, '2021-11-03 09:13:20', '2021-11-19 14:01:21', 1, 1, NULL, 23, NULL, '', NULL, '', '', '', NULL, '10.231.11.1', 10092, 'nezha', 'nezha', '088723c3', 1),
(2, 'OAP-PE-T9K001-NPB02', '', '02CB041200010', -1, 3, 2, 3, 3, 1, 1, 5, 7, '10.231.11.2', NULL, '2021-11-03 09:14:10', '2021-11-19 14:01:37', 1, 1, NULL, 23, NULL, '', NULL, '', '', '', NULL, '10.231.11.2', 10092, 'nezha', 'nezha', '088723c3', 1),
(3, 'OAP-PE-T9K001-NPB03', '', '02CB070200095', -1, 3, 2, 3, 3, 1, 25, NULL, NULL, '10.231.11.3', NULL, '2021-11-03 09:14:41', '2021-11-19 14:01:54', 1, 1, NULL, 23, NULL, '', NULL, '', '', '', NULL, '10.231.11.3', 10092, 'nezha', 'nezha', '088723c3', 1),
(4, 'OAP-PE-T9K001-NPB04', '', '02CB070200052', -1, 3, 2, 3, 3, 1, 25, NULL, NULL, '10.231.11.4', NULL, '2021-11-03 09:15:09', '2021-11-19 14:02:07', 1, 1, NULL, 23, NULL, '', NULL, '', '', '', NULL, '10.231.11.4', 10092, 'nezha', 'nezha', '088723c3', 1),
(5, 'OAP-PE-T9K001-NPB05', '', '02CB041200078', -1, 3, 2, 3, 3, 1, 25, NULL, NULL, '10.231.11.5', NULL, '2021-11-03 09:15:29', '2021-11-19 14:02:29', 1, 1, NULL, 23, NULL, '', NULL, '', '', '', NULL, '10.231.11.5', 10092, 'nezha', 'nezha', '088723c3', 1);
