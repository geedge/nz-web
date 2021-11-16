
/**
 * 2021年11月15日
 * sys_user新增字段 最后修改密码时间 同时将以往数据进行赋值  dc经纬度坐标小数位由5变为7
 */

ALTER TABLE `sys_user` 
ADD COLUMN `pin_time` datetime NOT NULL COMMENT '最后修改密码时间';

update sys_user set pin_time = now();


ALTER TABLE `dc` 
MODIFY COLUMN `longitude` double(10, 7) NULL DEFAULT NULL COMMENT '经度 -180,180' AFTER `y`,
MODIFY COLUMN `latitude` double(10, 7) NULL DEFAULT NULL COMMENT '纬度 -90，90' AFTER `longitude`;