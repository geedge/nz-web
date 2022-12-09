/**
 * asset attribute
 * 除默认 default group，预留 100000 个
 */
UPDATE asset_field_group SET id = id + 100000 WHERE id != 1 ORDER BY id DESC;
UPDATE asset_field_meta  SET id = id + 100000 ORDER BY id DESC;
UPDATE asset_field_meta  SET group_id = group_id + 100000 WHERE group_id != 1;
UPDATE asset_field_value SET meta_id = meta_id + 100000;

/**
 * alert rule
 * 除默认 default snmptrap，预留 100000 个
 * 由于 Default snmptrap 之前 build_in = 0 , 解决部分环境删除 Default snmptrap 问题
 */
INSERT INTO `alert_rule`
(`id`, `name`, `expr`, `last`, `summary`, `description`, `receiver`, `build_in`, `operator`, `threshold`, `unit`, `seq`, `severity_id`, `method`, `state`, `type`, `inr`, `timeout`, `auto_expired`, `sched_enable`, `sched_days`, `sched_stime`, `sched_etime`, `notify_active`, `notify_expired`, `trb_shot`)
SELECT
1, 'Default snmptrap', '1.3.6.1', 3600, '{{$message}}', '{{$variables}}', '', '1', '', NULL, 0, NULL, 3, '', 1, 3, 0, 300, 1, 0, '1,2,3,4,5,6,7', '00:00', '23:59', 1, 1, '' FROM dual
WHERE NOT EXISTS (SELECT name FROM `alert_rule` WHERE name = "Default snmptrap" or id = 1);

UPDATE alert_rule SET build_in = 1 WHERE id = 1;
UPDATE alert_rule SET build_in = 1 WHERE name = "Default snmptrap";

UPDATE alert_message_active  am LEFT JOIN alert_rule ar ON am.rule_id = ar.id SET am.rule_id = am.rule_id + 100000 WHERE ar.build_in != 1;
UPDATE alert_message_history am LEFT JOIN alert_rule ar ON am.rule_id = ar.id SET am.rule_id = am.rule_id + 100000 WHERE ar.build_in != 1;
UPDATE alert_message_silence am LEFT JOIN alert_rule ar ON am.rule_id = ar.id SET am.rule_id = am.rule_id + 100000 WHERE ar.build_in != 1;

UPDATE alert_rule_eval_log   el LEFT JOIN alert_rule ar ON el.rule_id = ar.id SET el.rule_id = el.rule_id + 100000 WHERE ar.build_in != 1;
UPDATE alert_rule_sched_log  el LEFT JOIN alert_rule ar ON el.rule_id = ar.id SET el.rule_id = el.rule_id + 100000 WHERE ar.build_in != 1;

UPDATE alert_rule SET id = id + 100000 WHERE build_in = 0 ORDER BY id DESC;


/**
 * record rule
 */
UPDATE record_rule SET id = id + 100000 WHERE build_in != 1 ORDER BY id DESC;




/**
 * panel & chart
 * 内置 Overview panel
 */
INSERT INTO `visual_panel`
(`id`, `name`, `create_by`, `type`, `link`, `pid`, `weight`, `build_in`, `seq`, `param`, `remark`, `uts`)
SELECT
1, 'Overview', 1, 'dashboard', 0, 0, 0, '1', 'v3.2-buildin', '{}', '', 0 FROM dual
WHERE NOT EXISTS (SELECT name FROM `visual_panel` WHERE ( name = "Overview" or id = 1) and seq = "v3.2-buildin");

UPDATE visual_panel SET build_in = 1 WHERE id = 1;
UPDATE visual_panel SET build_in = 1 WHERE name = "Overview";

/**
 *  visual_chart_element id、chart_id
 */
UPDATE visual_chart_element vce LEFT JOIN visual_chart vc ON vce.chart_id = vc.id SET vce.chart_id = vce.chart_id + 100000 WHERE vc.build_in != 1;
UPDATE visual_chart_element SET id = id + 100000 WHERE 	build_in != 1 ORDER BY id DESC;


/**
 * visual_chart group_id、pid、id、panel_id
 */
UPDATE visual_chart a LEFT JOIN visual_chart b ON a.pid = b.id SET a.pid = a.pid + 100000 WHERE a.pid IS NOT NULL 	AND b.build_in != 1;
UPDATE visual_chart a LEFT JOIN visual_chart b ON a.group_id = b.id SET a.group_id = a.group_id + 100000 WHERE 	b.build_in != 1;

UPDATE asset_model model
LEFT JOIN (
	SELECT
		t.id,
		GROUP_CONCAT( IF ( build_in != 1, t.chart_id + 100000, t.chart_id ) ) ids
	FROM
		(
		SELECT
			id,
			SUBSTRING_INDEX( SUBSTRING_INDEX( chart_ids, ',', ht.help_topic_id + 1 ), ',', - 1 ) AS chart_id
		FROM
			asset_model
			INNER JOIN ( SELECT @rownum := @rownum + 1 AS help_topic_id FROM ( SELECT @rownum :=- 1 ) t, visual_chart ) ht ON ht.help_topic_id < ( LENGTH( chart_ids ) - LENGTH( REPLACE ( chart_ids, ',', '' ) ) + 1 ) WHERE LENGTH( chart_ids ) > 0
		) t
		LEFT JOIN visual_chart vc ON t.chart_id = vc.id
	WHERE
		vc.id IS NOT NULL
	GROUP BY
		t.id
	) s ON model.id = s.id
	SET model.chart_ids = s.ids
WHERE
	s.id IS NOT NULL;


UPDATE monitor_module module
LEFT JOIN (
	SELECT
		t.id,
		GROUP_CONCAT( IF ( build_in != 1, t.chart_id + 100000, t.chart_id ) ) ids
	FROM
		(
		SELECT
			id,
			SUBSTRING_INDEX( SUBSTRING_INDEX( chart_ids, ',', ht.help_topic_id + 1 ), ',', - 1 ) AS chart_id
		FROM
			monitor_module
			INNER JOIN ( SELECT @rownum := @rownum + 1 AS help_topic_id FROM ( SELECT @rownum :=- 1 ) t, visual_chart ) ht ON ht.help_topic_id < ( LENGTH( chart_ids ) - LENGTH( REPLACE ( chart_ids, ',', '' ) ) + 1 ) WHERE LENGTH( chart_ids ) > 0
		) t
		LEFT JOIN visual_chart vc ON t.chart_id = vc.id
	WHERE
		vc.id IS NOT NULL
	GROUP BY
		t.id
	) s ON module.id = s.id
	SET module.chart_ids = s.ids
WHERE
	s.id IS NOT NULL;


UPDATE visual_chart SET id = id + 100000 WHERE 	build_in != 1 ORDER BY id DESC;

UPDATE visual_chart vc LEFT JOIN visual_panel vp ON vc.panel_id = vp.id SET vc.panel_id = vc.panel_id + 100000 WHERE vp.build_in != 1;

UPDATE visual_panel a LEFT JOIN visual_panel b ON a.pid = b.id SET a.pid = a.pid + 100000 WHERE a.pid != 0 	AND b.build_in != 1;
UPDATE visual_panel SET id = id + 100000 WHERE build_in != 1 ORDER BY id DESC;


/**
 * asset brand
 */
UPDATE asset_model model LEFT JOIN asset_brand brand ON model.brand_id = brand.id SET model.brand_id = model.brand_id + 100000 WHERE brand.name not in ("Generic","GDNT");
UPDATE asset_asset asset LEFT JOIN asset_brand brand ON asset.brand_id = brand.id SET asset.brand_id = asset.brand_id + 100000 WHERE brand.name not in ("Generic","GDNT");
UPDATE asset_brand SET id = id + 100000 WHERE name not in ("Generic","GDNT") ORDER BY id DESC;

INSERT INTO `asset_brand` (`id`, `name`, `remark`, `seq`)
SELECT
1, 'Generic', '', 'build-in' FROM dual
WHERE NOT EXISTS (SELECT name FROM `asset_brand` WHERE name = "Generic");

INSERT INTO `asset_brand` (`id`, `name`, `remark`, `seq`)
SELECT
2, 'GDNT', '', 'build-in' FROM dual
WHERE NOT EXISTS (SELECT name FROM `asset_brand` WHERE name = "GDNT");



/**
 * asset model
 */
SELECT @GenericBrandId:=id FROM asset_brand WHERE name = 'Generic';
SELECT @GDNTBrandId:=id FROM asset_brand WHERE name = 'GDNT';


UPDATE asset_asset asset LEFT JOIN asset_model model ON asset.model_id = model.id SET asset.model_id = asset.model_id + 100000 WHERE
model.name NOT IN ( 'Generic Linux', 'Generic switch', 'Generic router', 'Generic firewall', 'Generic VM', 'Generic computer', 'TSG-X', 'TSG-7400', 'TSG-9140' );

UPDATE asset_model SET id = id + 100000 WHERE name NOT IN ( 'Generic Linux', 'Generic switch', 'Generic router', 'Generic firewall', 'Generic VM', 'Generic computer', 'TSG-X', 'TSG-7400', 'TSG-9140' ) ORDER BY id DESC;

INSERT INTO `asset_model` (`id`, `brand_id`, `name`, `remark`, `seq`, `usize`, `chart_ids`, `type_id`, `sys_object_id`, `tsg_appliance`)
SELECT
1, @GenericBrandId, 'Generic Linux',    '', 'builld-in', 1, '', 6, '', 0 FROM dual
WHERE NOT EXISTS (SELECT name FROM `asset_model` WHERE name = "Generic Linux")
AND EXISTS ( SELECT NAME FROM `asset_type_conf` WHERE id = 6 );

INSERT INTO `asset_model` (`id`, `brand_id`, `name`, `remark`, `seq`, `usize`, `chart_ids`, `type_id`, `sys_object_id`, `tsg_appliance`)
SELECT
2, @GenericBrandId, 'Generic switch',   '', 'builld-in', 1, '', 4, '', 0 FROM dual
WHERE NOT EXISTS (SELECT name FROM `asset_model` WHERE name = "Generic switch")
AND EXISTS ( SELECT NAME FROM `asset_type_conf` WHERE id = 4 );

INSERT INTO `asset_model` (`id`, `brand_id`, `name`, `remark`, `seq`, `usize`, `chart_ids`, `type_id`, `sys_object_id`, `tsg_appliance`)
SELECT
3, @GenericBrandId, 'Generic router',   '', 'builld-in', 1, '', 2, '', 0 FROM dual
WHERE NOT EXISTS (SELECT name FROM `asset_model` WHERE name = "Generic router")
AND EXISTS ( SELECT NAME FROM `asset_type_conf` WHERE id = 2 );

INSERT INTO `asset_model` (`id`, `brand_id`, `name`, `remark`, `seq`, `usize`, `chart_ids`, `type_id`, `sys_object_id`, `tsg_appliance`)
SELECT
4, @GenericBrandId, 'Generic firewall', '', 'builld-in', 1, '', 3, '', 0 FROM dual
WHERE NOT EXISTS (SELECT name FROM `asset_model` WHERE name = "Generic firewall")
AND EXISTS ( SELECT NAME FROM `asset_type_conf` WHERE id = 3 );

INSERT INTO `asset_model` (`id`, `brand_id`, `name`, `remark`, `seq`, `usize`, `chart_ids`, `type_id`, `sys_object_id`, `tsg_appliance`)
SELECT
5, @GenericBrandId, 'Generic VM',       '', 'builld-in', 1, '', 7, '', 0 FROM dual
WHERE NOT EXISTS (SELECT name FROM `asset_model` WHERE name = "Generic VM")
AND EXISTS ( SELECT NAME FROM `asset_type_conf` WHERE id = 7 );

INSERT INTO `asset_model` (`id`, `brand_id`, `name`, `remark`, `seq`, `usize`, `chart_ids`, `type_id`, `sys_object_id`, `tsg_appliance`)
SELECT
6, @GenericBrandId, 'Generic computer', '', 'builld-in', 1, '', 5, '', 0 FROM dual
WHERE NOT EXISTS (SELECT name FROM `asset_model` WHERE name = "Generic computer")
AND EXISTS ( SELECT NAME FROM `asset_type_conf` WHERE id = 5 );

INSERT INTO `asset_model` (`id`, `brand_id`, `name`, `remark`, `seq`, `usize`, `chart_ids`, `type_id`, `sys_object_id`, `tsg_appliance`)
SELECT
7, @GDNTBrandId, 'TSG-X',            '', 'builld-in', 1, '', 3, '', 1 FROM dual
WHERE NOT EXISTS (SELECT name FROM `asset_model` WHERE name = "TSG-X")
AND EXISTS ( SELECT NAME FROM `asset_type_conf` WHERE id = 3 );

INSERT INTO `asset_model` (`id`, `brand_id`, `name`, `remark`, `seq`, `usize`, `chart_ids`, `type_id`, `sys_object_id`, `tsg_appliance`)
SELECT
8, @GDNTBrandId, 'TSG-7400',         '', 'builld-in', 1, '', 8, '', 0 FROM dual
WHERE NOT EXISTS (SELECT name FROM `asset_model` WHERE name = "TSG-7400")
AND EXISTS ( SELECT NAME FROM `asset_type_conf` WHERE id = 8 );

INSERT INTO `asset_model` (`id`, `brand_id`, `name`, `remark`, `seq`, `usize`, `chart_ids`, `type_id`, `sys_object_id`, `tsg_appliance`)
SELECT
9, @GDNTBrandId, 'TSG-9140',         '', 'builld-in', 1, '', 3, '', 0 FROM dual
WHERE NOT EXISTS (SELECT name FROM `asset_model` WHERE name = "TSG-9140")
AND EXISTS ( SELECT NAME FROM `asset_type_conf` WHERE id = 3 );

/**
 * update table auto_increment
 */
ALTER TABLE asset_field_group auto_increment = 100000;
ALTER TABLE asset_field_meta auto_increment = 100000;
ALTER TABLE alert_rule auto_increment = 100000;
ALTER TABLE record_rule auto_increment = 100000;
ALTER TABLE visual_chart_element auto_increment = 100000;
ALTER TABLE visual_chart auto_increment = 100000;
ALTER TABLE visual_panel auto_increment = 100000;
ALTER TABLE asset_brand auto_increment = 100000;
ALTER TABLE asset_model auto_increment = 100000;

