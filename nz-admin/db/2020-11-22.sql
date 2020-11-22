
 /**
  * chart 新增 weight 字段 , 作为排序使用, prev,next 暂时弃用
  */
 ALTER TABLE chart ADD COLUMN weight INT(10) NOT NULL DEFAULT 0;
 ALTER TABLE chart MODIFY COLUMN `prev` int(10);
 ALTER TABLE chart MODIFY COLUMN `next` int(10);