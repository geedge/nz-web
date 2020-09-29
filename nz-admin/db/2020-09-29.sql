/**
 * 增加 asset,model 类型 charts 导入导出表头
 */

INSERT INTO `sys_config` VALUES (null, 'default_cabinet_usize', '40', 1, '机柜默认U位值');



TRUNCATE sys_area;
INSERT INTO `sys_area` VALUES ('1', 'Aktau', '0', '0', '73.063460', '50.223100', '{\"cn\":\"Aktau\",\"en\":\"Aktau\"}');
INSERT INTO `sys_area` VALUES ('2', 'Aktubinsk', '0', '0', '57.100000', '50.170000', '{\"cn\":\"Aktubinsk\",\"en\":\"Aktubinsk\"}');
INSERT INTO `sys_area` VALUES ('3', 'Almaty', '0', '0', '76.928611', '43.256667', '{\"cn\":\"Almaty\",\"en\":\"Almaty\"}');
INSERT INTO `sys_area` VALUES ('4', 'Nur-Sultan', '0', '0', '71.450000', '51.166666', '{\"cn\":\"Nur-Sultan\",\"en\":\"Nur-Sultan\"}');
INSERT INTO `sys_area` VALUES ('5', 'Atyrau', '0', '0', '51.883333', '47.116667', '{\"cn\":\"Atyrau\",\"en\":\"Atyrau\"}');
INSERT INTO `sys_area` VALUES ('6', 'Karaganda', '0', '0', '73.100000', '49.500000', '{\"cn\":\"Karaganda\",\"en\":\"Karaganda\"}');
INSERT INTO `sys_area` VALUES ('7', 'Kokshetau', '0', '0', '69.400000', '53.283333', '{\"cn\":\"Kokshetau\",\"en\":\"Kokshetau\"}');
INSERT INTO `sys_area` VALUES ('8', 'Kostanay', '0', '0', '63.606160', '53.227440', '{\"cn\":\"Kostanay\",\"en\":\"Kostanay\"}');
INSERT INTO `sys_area` VALUES ('9', 'Kyzylorda', '0', '0', '65.495400', '44.854740', '{\"cn\":\"Kyzylorda\",\"en\":\"Kyzylorda\"}');
INSERT INTO `sys_area` VALUES ('10', 'Pavlodar', '0', '0', '76.966666', '52.283333', '{\"cn\":\"Pavlodar\",\"en\":\"Pavlodar\"}');
INSERT INTO `sys_area` VALUES ('11', 'Petropavl', '0', '0', '69.150000', '54.866666', '{\"cn\":\"Petropavl\",\"en\":\"Petropavl\"}');
INSERT INTO `sys_area` VALUES ('12', 'Semey', '0', '0', '80.266694', '50.426749', '{\"cn\":\"Semey\",\"en\":\"Semey\"}');
INSERT INTO `sys_area` VALUES ('13', 'Shymkent', '0', '0', '69.600000', '42.300000', '{\"cn\":\"Shymkent\",\"en\":\"Shymkent\"}');
INSERT INTO `sys_area` VALUES ('14', 'Taldykurgan', '0', '0', '78.383333', '45.016666', '{\"cn\":\"Taldykurgan\",\"en\":\"Taldykurgan\"}');
INSERT INTO `sys_area` VALUES ('15', 'Taraz', '0', '0', '71.366667', '42.900000', '{\"cn\":\"Taraz\",\"en\":\"Taraz\"}');
INSERT INTO `sys_area` VALUES ('16', 'Uralsk', '0', '0', '51.434590', '51.249110', '{\"cn\":\"Uralsk\",\"en\":\"Uralsk\"}');
INSERT INTO `sys_area` VALUES ('17', 'Ust-Kamenogorsk', '0', '0', '82.600000', '49.966666', '{\"cn\":\"Öskemen\",\"en\":\"Öskemen\"}');
INSERT INTO `sys_area` VALUES ('18', 'Zhezkazgan', '0', '0', '67.766667', '47.783333', '{\"cn\":\"Zhezkazgan\",\"en\":\"Zhezkazgan\"}');



update module set project_id=1 where id=1;

delete project where id=2;