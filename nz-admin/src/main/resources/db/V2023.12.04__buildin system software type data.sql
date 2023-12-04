
/**
 * 1. build-in software_type data

    asset_field_group{ License }
        asset_field_meta[ url & token]

    software_type[ TSG-OS Concentrate,TSG-OS Stand-alone,TSG-CM,Network Zodiac ]

 */

SELECT @LicenseSeq:="License Management Build-in";

INSERT INTO `asset_field_group`(`id`, `name`, `remark`, `build_in`, `seq`)
VALUES
    (NULL, 'License', '', '1', @LicenseSeq);

SELECT @LicenseFieldGroupId:=id FROM asset_field_group WHERE name = "License";

INSERT INTO `asset_field_meta`(`id`, `name`, `meta_key`, `group_id`, `search`, `display`, `type`, `remark`, `param`, `build_in`, `seq`)
VALUES
    (NULL, 'License URL', 'url', @LicenseFieldGroupId, '0', '0', 'TEXT', 'License service url', '{\"required\":\"1\"}', 1, @LicenseSeq),
    (NULL, 'License Token', 'token', @LicenseFieldGroupId, '0', '0', 'TEXT', 'Token used for authentication or access authorization.', '{\"default\":\"\",\"required\":\"0\"}', 1, @LicenseSeq);


TRUNCATE software_type;
INSERT INTO `software_type`(`id`, `name`, `category`, `icon`, `params`, `remark`, `build_in`, `cts`, `uts`)
VALUES
    (NULL, 'TSG-OS Concentrate', 'License', '', '{\"acc_ip\":{\"name\":\"IP\",\"metaKey\":\"acc_ip\",\"search\":0,\"display\":0,\"type\":\"TEXT\",\"param\":\"{\\\"required\\\":\\\"1\\\"}\",\"remark\":\"\",\"group\":{\"name\":\"ACC\"},\"value\":[\"\"],\"starred\":0,\"leaf\":true,\"disabled\":false,\"action\":0},\"acc_port\":{\"name\":\"Port\",\"metaKey\":\"acc_port\",\"search\":0,\"display\":0,\"type\":\"INTEGER\",\"param\":\"{\\\"default\\\":1947,\\\"required\\\":\\\"1\\\"}\",\"remark\":\"\",\"group\":{\"name\":\"ACC\"},\"value\":[1947],\"starred\":0,\"leaf\":true,\"disabled\":false,\"action\":0},\"acc_password\":{\"name\":\"Password\",\"metaKey\":\"acc_password\",\"search\":0,\"display\":0,\"type\":\"PASSWORD\",\"param\":\"{\\\"required\\\":\\\"0\\\"}\",\"remark\":\"\",\"group\":{\"name\":\"ACC\"},\"value\":[\"\"],\"starred\":0,\"leaf\":true,\"disabled\":false,\"action\":0}}', @LicenseSeq, '1', 1701648000000, 1701648000000),
    (NULL, 'TSG-OS Stand-alone', 'License', '', '{\"acc_ip\":{\"name\":\"IP\",\"metaKey\":\"acc_ip\",\"search\":0,\"display\":0,\"type\":\"TEXT\",\"param\":\"{\\\"required\\\":\\\"1\\\"}\",\"remark\":\"\",\"group\":{\"name\":\"ACC\"},\"value\":[\"\"],\"starred\":0,\"leaf\":true,\"disabled\":false,\"action\":0},\"acc_port\":{\"name\":\"Port\",\"metaKey\":\"acc_port\",\"search\":0,\"display\":0,\"type\":\"INTEGER\",\"param\":\"{\\\"default\\\":1947,\\\"required\\\":\\\"1\\\"}\",\"remark\":\"\",\"group\":{\"name\":\"ACC\"},\"value\":[1947],\"starred\":0,\"leaf\":true,\"disabled\":false,\"action\":0},\"acc_password\":{\"name\":\"Password\",\"metaKey\":\"acc_password\",\"search\":0,\"display\":0,\"type\":\"PASSWORD\",\"param\":\"{\\\"required\\\":\\\"0\\\"}\",\"remark\":\"\",\"group\":{\"name\":\"ACC\"},\"value\":[\"\"],\"starred\":0,\"leaf\":true,\"disabled\":false,\"action\":0}}', @LicenseSeq, '1', 1701648000000, 1701648000000),
    (NULL, 'TSG-CM', 'License', '', '{\"url\":{\"name\":\"License URL\",\"metaKey\":\"url\",\"search\":0,\"display\":0,\"type\":\"TEXT\",\"param\":\"{\\\"required\\\":\\\"1\\\"}\",\"remark\":\"License service url\",\"group\":{\"name\":\"License\"},\"value\":[\"\"],\"starred\":0,\"leaf\":true,\"disabled\":false,\"action\":0},\"token\":{\"name\":\"License Token\",\"metaKey\":\"token\",\"search\":0,\"display\":0,\"type\":\"TEXT\",\"param\":\"{\\\"default\\\":\\\"\\\",\\\"required\\\":\\\"0\\\"}\",\"remark\":\"Token used for authentication or access authorization.\",\"group\":{\"name\":\"License\"},\"value\":[\"\"],\"starred\":0,\"leaf\":true,\"disabled\":false,\"action\":0}}', @LicenseSeq, '1', 1701648000000, 1701648000000),
    (NULL, 'Network Zodiac', 'License', '', '{\"url\":{\"name\":\"License URL\",\"metaKey\":\"url\",\"search\":0,\"display\":0,\"type\":\"TEXT\",\"param\":\"{\\\"required\\\":\\\"1\\\"}\",\"remark\":\"License service url\",\"group\":{\"name\":\"License\"},\"value\":[\"\"],\"starred\":0,\"leaf\":true,\"disabled\":false,\"action\":0},\"token\":{\"name\":\"License Token\",\"metaKey\":\"token\",\"search\":0,\"display\":0,\"type\":\"TEXT\",\"param\":\"{\\\"default\\\":\\\"\\\",\\\"required\\\":\\\"0\\\"}\",\"remark\":\"Token used for authentication or access authorization.\",\"group\":{\"name\":\"License\"},\"value\":[\"\"],\"starred\":0,\"leaf\":true,\"disabled\":false,\"action\":0}}', @LicenseSeq, '1', 1701648000000, 1701648000000);

