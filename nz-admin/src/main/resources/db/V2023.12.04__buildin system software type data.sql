
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
    (NULL, 'License Token', 'token', @LicenseFieldGroupId, '0', '0', 'PASSWORD', 'Token used for authentication or access authorization.', '{\"default\":\"\",\"required\":\"0\"}', 1, @LicenseSeq);


TRUNCATE software_type;
INSERT INTO `software_type`(`id`, `name`, `category`, `icon`, `params`, `remark`, `build_in`, `cts`, `uts`)
VALUES
    (NULL, 'TSG-OS Concentrate', 'License', '', '{\"connectorAttributeKeys\":[\"acc_ip\",\"acc_port\",\"acc_password\"]}',  @LicenseSeq, '1', 1701648000000, 1701648000000),
    (NULL, 'TSG-OS Stand-alone', 'License', '', '{\"connectorAttributeKeys\":[\"acc_ip\",\"acc_port\",\"acc_password\"]}',  @LicenseSeq, '1', 1701648000000, 1701648000000),
    (NULL, 'TSG-CM', 'License', '', '{\"connectorAttributeKeys\":[\"url\",\"token\"]}',  @LicenseSeq, '1', 1701648000000, 1701648000000),
    (NULL, 'Network Zodiac', 'License', '', '{\"connectorAttributeKeys\":[\"url\",\"token\"]}',  @LicenseSeq, '1', 1701648000000, 1701648000000);
