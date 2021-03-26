/**
 * 1. 新增 asset_asset table
 * 2. asset_field_meta type 默认值 'TEXT'
 * 3. asset_field_group 新增默认组
 * 4. 新增asset导入表头
 */
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for asset_asset
-- ----------------------------
DROP TABLE IF EXISTS `asset_asset`;
CREATE TABLE `asset_asset`  (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '不重复，唯一索引',
  `number` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '资产编号',
  `sn` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '资产序列号',
  `pid` int(10) NOT NULL DEFAULT -1 COMMENT '默认：-1\r\n\r\n只有type vm = 1可以且必须填写此字段，且 pid.type vmh = 1',
  `type_id` int(10) NOT NULL,
  `state_id` int(10) NOT NULL,
  `ssh_port` int(6) NOT NULL DEFAULT -1,
  `telnet_port` int(6) NOT NULL DEFAULT -1,
  `brand_id` int(10) NOT NULL,
  `model_id` int(10) NOT NULL,
  `dc_id` int(10) NOT NULL,
  `cabinet_id` int(10) NULL DEFAULT NULL,
  `cabinet_start` int(10) NULL DEFAULT NULL,
  `cabinet_end` int(10) NULL DEFAULT NULL,
  `manage_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '管理ip',
  `purchase_date` date NULL DEFAULT NULL COMMENT '购买日期',
  `create_at` datetime(0) NULL DEFAULT NULL COMMENT '添加时间',
  `update_at` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `create_by` int(10) NULL DEFAULT NULL COMMENT '添加人，与user表关联',
  `update_by` int(10) NULL DEFAULT NULL COMMENT '最后更新人',
  `seq` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 92 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;


/**
 * asset_field_meta type 默认值 'TEXT'
 */
ALTER TABLE `asset_field_meta`
MODIFY COLUMN `type` varchar(32) NOT NULL DEFAULT 'TEXT' COMMENT '属性输入类型';


/**
 * asset_field_group 新增默认组
 */
INSERT INTO `asset_field_group`(`id`, `name`, `remark`, `build_in`, `seq`) VALUES (1, 'default', 'Default meta group', '1', '');


/**
 * 新增asset导入表头
 */
DELETE from `sys_config` where param_key = "asset_asset_export_header";
INSERT INTO `sys_config`(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'asset_asset_export_header', '{\"cn\":{\"*资产名称\":\"资产名称，唯一标识(必填) \\n示例: DELL服务器\",\"*SN码\":\"资产序列号，唯一标识(必填) \\n示例: A0420200101\",\"虚拟机宿主\":\"虚拟机宿主，当资产为虚拟机时必填 \\n示例: DELL物理机\",\"*管理ip\":\"主机地址(必填),IP格式 \\n示例: 172.0.0.1\",\"*品牌\":\"品牌(必填) \\n示例: DELL\",\"*型号\":\"型号(必填) \\n示例: DELL-D3\",\"*数据中心\":\"数据中心(必填) \\n示例: 北京电信中心机房\",\"机柜\":\"机柜 \\n示例: cabinet-001\",\"开始U位\":\"机柜开始位置 此信息一般和机柜结束位置联合使用 \\n示例: 1\",\"结束U位\":\"机柜结束位置 此信息一般与机柜开始位置联合使用 \\n示例: 2\",\"*资产状态\":\"资产状态 \\n示例: 在库\",\"*资产类型\":\"资产类型 \\n示例: 服务器\",\"端口\":\"ssh/telnet端口 \\n示例: ssh/22,telnet/23\",\"购买日期\":\"购买日期 \\n示例: 2020/2/25\",\"属性信息\":\"资产属性信息,格式: key=value 如需填写多个请使用回车换行 \\n示例: NetworkCard=eth0 \\n user=root\"},\"en\":{\"*Asset Name\":\"Asset name, unique identifier (required) \\nExample: DELL server\",\"*Sn\":\"Asset serial number, unique identifier (required) \\nExample: A0420200101\",\"*Virtual machine host\":\"Virtual machine host, required when the asset is a virtual machine \\nExample: DELL physical machine\",\"*Management ip\":\"Host address (required), IP format \\nExample: 172.0.0.1\",\"*Brand\":\"Brand (required) \\nExample: DELL\",\"*Model\":\"Model (required) \\nExample: DELL-D3\",\"*DC\":\"Data Center (Required) \\nExample: Beijing Telecom Center Computer Room\",\"Cabinet\":\"Cabinet \\nExample: cabinet-001\",\"Cabinet Start\":\"Cabinet start position This information is generally used in conjunction with the cabinet end position \\nExample: 1\",\"Cabinet End\":\"Cabinet end position This information is generally used in conjunction with the cabinet start position \\nExample: 2\",\"*Asset state\":\"Asset status \\nExample: In stock\",\"*Asset type\":\"Asset Type\\nExample: Server\",\"Port\":\"ssh/telnet port \\nExample: ssh/22,telnet/23\",\"Purchase date\":\"Purchase date \\nExample: 2020/2/25\",\"Field Information\":\"Asset field information, format: key=value If you need to fill in more than one, please use carriage return and line feed \\nExample: NetworkCard=eth0 \\n user=root\"}}', 1, NULL);


SET FOREIGN_KEY_CHECKS = 1;
