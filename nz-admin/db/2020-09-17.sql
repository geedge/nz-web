/*
 * panel chart chart_element 表增加 seq 导入唯一标识列
 *
 * sys_config 表增加 chart 表头
 *
 * alert_rule 表 threshold 改为 double 类型
 */

 alter table panel add column seq varchar(64) COMMENT '导入唯一标识 全局唯一，用于导入撤销';
 alter table chart add column seq varchar(64) COMMENT '导入唯一标识 全局唯一，用于导入撤销';
 alter table chart_element add column seq varchar(64) COMMENT '导入唯一标识 全局唯一，用于导入撤销';


INSERT INTO sys_config(`id`, `param_key`, `param_value`, `status`, `remark`) VALUES (NULL, 'chart_export_header', '{\"cn\":{\"*面板\":\"面板名称(必填) \\n示例: 系统面板\",\"*图表名称\":\"图表名称(必填) \\n示例: CPU使用率\",\"*图表类型\":\"图表类型(必填) 可选: line,bar,table,stackArea,singleStat,url,,alertList \\n示例: line\",\"*宽度\":\"宽度(必填) 可选：1-12 \\n示例: 10\",\"*高度\":\"高度(必填),默认单位（px/像素） \\n示例:400\",\"单位\":\"单位(必填) 可选: \\nMisc: none,short,percent(0-100),percent(0.0-1.0),local format\\nData: bits,bytes,kilobytes,megabytes,gigabytes,terabytes,petabytes\\nDataRate: packets/sec,bits/sec,bytes/sec,kilobytes/sec,kilobits/sec,megabytes/sec,megabits/sec,gigabytes/sec,gigabits/sec,terabytes/sec,terabits/sec,petabytes/sec,petabits/sec\\nTime: Hertz(1/s),nanoseconds(ns),microseconds(us),milliseconds(ms),seconds(s),minutes(m),hours(h),days(d)\\n示例: short\",\"*参数\":\"单位 根据type不同，内容不同，格式为JSON格式 \\n示例: {\\\"statistics\\\":\\\"average\\\"}\",\"表达式\":\"当type=line，bar，stackArea时，包含 elements key，值为对象数组。\\n 示例:[{\\\"expression\\\":\\\"sum(up)\\\",\\\"legend\\\":\\\"test\\\"}]\"},\"en\":{\"*Panel Name\":\"Panel(Required) \\nExample: System panel\",\"*Chart Title\":\"Chart title(Required) \\nExample: CPU usage\",\"*Chart Type\":\"Chart type(Required) Optional: line,bar,table,stackArea,singleStat,url,,alertList \\nExample: line\",\"*Width\":\"width(Required) Optional：1-12 \\nExample: 10\",\"*Height\":\"height(Required),Default unit（px/Pixel） \\nExample:400\",\"Unit\":\"unit(Required) Optional: \\nMisc: none,short,percent(0-100),percent(0.0-1.0),local format\\nData: bits,bytes,kilobytes,megabytes,gigabytes,terabytes,petabytes\\nDataRate: packets/sec,bits/sec,bytes/sec,kilobytes/sec,kilobits/sec,megabytes/sec,megabits/sec,gigabytes/sec,gigabits/sec,terabytes/sec,terabits/sec,petabytes/sec,petabits/sec\\nTime: Hertz(1/s),nanoseconds(ns),microseconds(us),milliseconds(ms),seconds(s),minutes(m),hours(h),days(d)\\nExample: short\",\"*Parameter\":\"Parameters match type，The format is JSON format \\nExample: {\\\"statistics\\\":\\\"average\\\"}\",\"Expressions\":\"When the type is line, table, stackArea, singleStat, histogram, it contains elements key and the value is an object array.\\nExample:[{\\\"expression\\\":\\\"sum(up)\\\",\\\"legend\\\":\\\"test\\\"}]\"}}', 1, NULL);


 alter table alert_rule modify  column threshold double(20,5) not null COMMENT "阈值";