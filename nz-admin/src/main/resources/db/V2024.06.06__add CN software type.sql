/**
 * 新增 CN 软件资产类型
 */

SELECT @LicenseSeq:="License Management Build-in";
INSERT INTO `software_type`(`id`, `name`, `category`, `icon`, `params`, `remark`, `build_in`, `cts`, `uts`)
VALUES
    (NULL, 'Cyber Narrator', 'License', '', '{\"connectorAttributeKeys\":[\"url\",\"token\"]}',  @LicenseSeq, '1', 1701648000000, 1701648000000);
