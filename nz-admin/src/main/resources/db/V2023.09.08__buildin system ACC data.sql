/**
 * 1. build-in ACC data
 */

SELECT @ACCBuildInSeq:="ACC License Management Build-in";

INSERT INTO `asset_field_group`(`id`, `name`, `remark`, `build_in`, `seq`)
VALUES
    (NULL, 'ACC', 'ACC meta group', '1', @ACCBuildInSeq);

SELECT @ACCFieldGroupId:=id FROM asset_field_group WHERE name = "ACC";

INSERT INTO `asset_field_meta`(`id`, `name`, `meta_key`, `group_id`, `search`, `display`, `type`, `remark`, `param`, `build_in`, `seq`)
VALUES
    (NULL, 'IP', 'acc_ip', @ACCFieldGroupId, '0', '0', 'TEXT', '', '{\"required\":\"1\"}', 1, @ACCBuildInSeq),
    (NULL, 'Port', 'acc_port', @ACCFieldGroupId, '0', '0', 'INTEGER', '', '{\"default\":1947,\"required\":\"1\"}', 1, @ACCBuildInSeq),
    (NULL, 'Password', 'acc_password', @ACCFieldGroupId, '0', '0', 'PASSWORD', '', '{\"required\":\"0\"}', 1, @ACCBuildInSeq);

INSERT INTO `software_type`(`id`, `name`, `category`, `icon`, `params`, `remark`, `build_in`, `cts`, `uts`)
VALUES
    (NULL, 'ACC', 'License', '', '{\"acc_ip\":{\"name\":\"IP\",\"metaKey\":\"acc_ip\",\"search\":0,\"display\":0,\"type\":\"TEXT\",\"param\":\"{\\\"required\\\":\\\"1\\\"}\",\"remark\":\"\",\"group\":{\"name\":\"ACC\"},\"value\":[\"\"],\"starred\":0,\"leaf\":true,\"disabled\":false,\"action\":0},\"acc_port\":{\"name\":\"Port\",\"metaKey\":\"acc_port\",\"search\":0,\"display\":0,\"type\":\"INTEGER\",\"param\":\"{\\\"default\\\":1947,\\\"required\\\":\\\"1\\\"}\",\"remark\":\"\",\"group\":{\"name\":\"ACC\"},\"value\":[1947],\"starred\":0,\"leaf\":true,\"disabled\":false,\"action\":0},\"acc_password\":{\"name\":\"Password\",\"metaKey\":\"acc_password\",\"search\":0,\"display\":0,\"type\":\"PASSWORD\",\"param\":\"{\\\"required\\\":\\\"0\\\"}\",\"remark\":\"\",\"group\":{\"name\":\"ACC\"},\"value\":[\"\"],\"starred\":0,\"leaf\":true,\"disabled\":false,\"action\":0}}', '', '1', 1694599888971, 1694600241925);
