truncate sys_file;

INSERT INTO `sys_file`(`id`, `uuid`, `name`, `type`, `size`, `md5`, `meta`, `storage`, `path`, `ts`) VALUES (1, 'uuid1', 'FileName1', 'jpg', 1, 'md5', '{}', 1, '', 1111111);
INSERT INTO `sys_file`(`id`, `uuid`, `name`, `type`, `size`, `md5`, `meta`, `storage`, `path`, `ts`) VALUES (2, 'uuid2', 'FileName2', 'jpg', 1, 'md5', '{}', 1, '', 1111111);
INSERT INTO `sys_file`(`id`, `uuid`, `name`, `type`, `size`, `md5`, `meta`, `storage`, `path`, `ts`) VALUES (3, 'uuid3', 'FileName3', 'jpg', 1, 'md5', '{}', 1, '', 1111111);

truncate sys_file_chunk;