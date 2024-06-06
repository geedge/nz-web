/**
 * 重置 SYS_ROLE_MENU amdin&common 数据
 */

 
UPDATE `sys_role` SET `name` = 'admin', `i18n` = 'admin', `remark` = 'Built-in administrator role with all permissions', `build_in` = 1 WHERE `id` = 1;
UPDATE `sys_role` SET `name` = 'common', `i18n` = 'common', `remark` = 'Built-in read-only permission role', `build_in` = 1 WHERE `id` = 2;


/**
 *  先清空 sys_role_menu 表中 admin&common 相关数据，再导入
 */
delete from sys_role_menu where role_id in (1,2);

-- admin
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3001);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3002);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3003);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3004);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3020);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3051);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3052);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3053);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3054);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3041);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3042);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3043);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3044);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3011);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3012);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3013);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3014);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3101);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3102);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3103);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3104);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3105);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3131);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3132);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3133);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3134);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3141);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3142);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3143);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3144);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3121);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3122);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3123);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3124);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3161);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3162);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3163);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3164);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3201);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3202);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3203);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3204);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3211);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3212);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3213);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3214);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3221);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3222);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3223);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3224);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3231);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3232);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3233);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3234);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3240);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3301);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3302);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3303);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3311);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3312);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3313);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3314);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3321);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3322);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3323);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3324);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3331);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3332);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3333);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3334);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3411);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3412);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3413);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3414);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3421);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3422);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3423);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3424);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3431);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3432);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3451);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3452);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3471);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3472);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3473);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3474);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3481);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3482);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3483);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3484);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3491);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3492);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3510);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3521);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3522);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3531);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3532);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3541);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3542);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3561);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3562);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3563);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3610);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3620);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3630);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3640);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 1, 3700);


-- common
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3001);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3011);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3020);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3041);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3051);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3101);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3121);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3131);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3141);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3161);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3201);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3211);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3221);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3231);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3240);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3301);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3311);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3321);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3331);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3411);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3421);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3431);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3451);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3610);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3620);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3630);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3640);
INSERT INTO `sys_role_menu`(`id`, `role_id`, `menu_id`) VALUES (NULL, 2, 3700);


-- 处理 v24 菜单变化
DELIMITER //

CREATE PROCEDURE update_role_menu_from_backup_v24()
BEGIN
    -- 检查备份表是否存在
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_menu_backup_v24') THEN
        -- 执行更新操作，通过 menu.code 匹配新的对应关系
        UPDATE sys_role_menu AS rm
        INNER JOIN sys_menu_backup_v24 AS bm ON rm.menu_id = bm.id
        INNER JOIN sys_menu AS m ON bm.code = m.code
        SET rm.menu_id = m.id
        WHERE
            rm.role_id NOT IN ( 1, 2 );

        -- 删除临时表
        DROP TABLE IF EXISTS `sys_menu_backup_v24`;
    END IF;
END //

DELIMITER ;

-- 调用存储过程
CALL update_role_menu_from_backup_v24();

-- 删除存储过程
DROP PROCEDURE IF EXISTS update_role_menu_from_backup_v24;
