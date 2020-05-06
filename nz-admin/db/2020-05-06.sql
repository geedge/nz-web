/*
   alert_rule表结构变更
     1. 结构变更字段如下： receiver

   conf_event表新增traffic数据

   idc_traffic表增加 insert，update，delete 触发器，修改 conf_event 表 value值加一
*/


-- 结构变更
alter table alert_rule modify column receiver varchar(64);

-- conf_event 新增数据
INSERT INTO `conf_event` VALUES ('idc_traffic', 0);


-- idc_traffic表新增 insert，update，delete 触发器

-- ----------------------------
-- Triggers structure for table idc_traffic
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_idc_traffic_insert`;
delimiter ;;
CREATE TRIGGER `tri_idc_traffic_insert` AFTER INSERT ON `idc_traffic` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'idc_traffic';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table idc_traffic
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_idc_traffic_update`;
delimiter ;;
CREATE TRIGGER `tri_idc_traffic_update` AFTER UPDATE ON `idc_traffic` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'idc_traffic';
end
;;
delimiter ;

-- ----------------------------
-- Triggers structure for table idc_traffic
-- ----------------------------
DROP TRIGGER IF EXISTS `tri_idc_traffic_delete`;
delimiter ;;
CREATE TRIGGER `tri_idc_traffic_delete` AFTER DELETE ON `idc_traffic` FOR EACH ROW begin
 update conf_event t set t.value = t.value +1 where t.table = 'idc_traffic';
end
;;
delimiter ;