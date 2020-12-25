/**
 * 系统初始角色 admin/common remark备注信息变更为英文
 */
update sys_role set remark = 'The system built-in administrator role' where name = 'admin';
update sys_role set remark = 'The system build-in common user role' where name = 'common';