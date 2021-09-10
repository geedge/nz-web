/**
 *  1. 修改 sys_user username 字段长度限制
 */

ALTER TABLE  `sys_user`
MODIFY COLUMN `username` varchar(64) NOT NULL COMMENT '用户名';
