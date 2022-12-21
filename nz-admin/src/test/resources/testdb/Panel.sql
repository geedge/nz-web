truncate visual_panel;

INSERT INTO `visual_panel`(`id`, `name`, `create_by`, `type`, `link`, `pid`, `weight`, `build_in`, `seq`, `param`, `remark`, `uts`) VALUES
(1, 'panel_1_______________________________this_name_length_is_60', 1, 'dashboard', null, 0, 0, '0', '', '{}', '', 0),
(2, 'panel_2', 1, 'dashboard', null, 0, 0, '0', '', '{}', '', 0),
(3, 'panel_3', 1, 'dashboard', null, 0, 0, '0', '', '{}', '', 0),
(4, 'panel_4', 1, 'dashboard', 0, 10, 20, '1', 'seq', '{"report": {"enable": false,"receivers": [3]}}', '用于复制', 0),
(5, 'panel_5', 1, 'dashboard', null, 0, 0, '0', '', '{}', '', 0);