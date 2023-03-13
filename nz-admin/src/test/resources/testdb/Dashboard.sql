truncate `visual_dashboard`;

INSERT INTO `visual_dashboard`(`id`, `name`, `create_by`, `type`, `link`, `pid`, `weight`, `build_in`, `seq`, `param`, `remark`, `uts`, `var_type`, `tmpl_id`) VALUES
(1, 'dashboard_1___________________________this_name_length_is_60', 1, 'dashboard', NULL, 0, 0, '0', '', '{}', '', 0, 0, -1),
(2, 'dashboard_2', 1, 'dashboard', NULL, 0, 0, '0', '', '{}', '', 0, 0, -1),
(3, 'dashboard_3', 1, 'dashboard', NULL, 0, 0, '0', '', '{}', '', 0, 0, -1),
(4, 'dashboard_4', 1, 'dashboard', 0, 10, 20, '1', 'seq', '{\"report\": {\"enable\": false,\"receivers\": [3]}}', '用于复制', 0, 0, -1),
(5, 'dashboard_5', 1, 'dashboard', NULL, 0, 0, '0', '', '{}', '', 0, 0, -1);
