truncate issue_record;

INSERT INTO `issue_record`(`id`, `name`, `type`, `content`, `state`, `priority`, `cts`, `uts`, `cid`, `uid`, `rid`, `aid`) VALUES (1, 'Server up', 'default', 'Server up', 1, 1, 1659951137576, 1659951137576, 1, 1, 1, 1);
INSERT INTO `issue_record`(`id`, `name`, `type`, `content`, `state`, `priority`, `cts`, `uts`, `cid`, `uid`, `rid`, `aid`) VALUES (2, 'Server left', '111', 'Server left', 3, 2, 1659951137576, 1659951137576, 1, 1, 1, 1);
INSERT INTO `issue_record`(`id`, `name`, `type`, `content`, `state`, `priority`, `cts`, `uts`, `cid`, `uid`, `rid`, `aid`) VALUES (3, 'Server right', '222', 'Server right', 5, 3, 1659951137576, 1659951137576, 1, 1, 1, 1);

truncate issue_asset_rel;

INSERT INTO `issue_asset_rel`(`id`, `issue_id`, `asset_id`) VALUES (1, 1, 1);
INSERT INTO `issue_asset_rel`(`id`, `issue_id`, `asset_id`) VALUES (2, 2, 2);

