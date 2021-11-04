
/**
 * 2021年11月4日10:46:18
 * 更新 内置email 拼写错误
 */

UPDATE `alert_notify_method` SET `name` = 'email', `file_path` = '', `account` = 'email', `state` = 1, `build_in` = 1 WHERE `id` = 1;
