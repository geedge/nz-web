package com.nis.common.utils;

import com.nis.common.utils.SqlHelper.KeywordsType;

import java.util.Map;

public abstract class SQLUtils {
    protected Map<KeywordsType, String> keywords;


    /**
    * @Description 创建表
    * @Author  rui
    * @Date   2019/7/31
    * @Param  tableName:表名     fields：字段及其字段约束  字段名--字段约束
    * @Return
    * @Exception
    */
    public abstract String createTable(String tableName, Map<String,String> fields);

    /**
    * @Description 根据表名查询表是否存在数据库
    * @Author  rui
    * @Date   2019/7/31
    * @Param  tableName：表名
    * @Return
    * @Exception
    */
    public abstract String queryTables(String tableName);

    /**
    * @Description 添加索引
    * @Author  rui
    * @Date   2019/7/31
    * @Param  tableName:表名 indexs:索引名称及要添加索引的字段名称数组
    * @Return
    * @Exception
    */
    public abstract String addIndexs(String tableName,Map<String,String[]> indexs);

    /**
    * @Description 删除索引
    * @Author  rui
    * @Date   2019/7/31
    * @Param  tableName:表名 indexNames :索引名称数组
    * @Return
    * @Exception
    */
    public abstract String delIndexs(String tableName,String ... indexNames);

    /**
    * @Description 删除表
    * @Author  rui
    * @Date   2019/7/31
    * @Param  tableName:表名
    * @Return
    * @Exception
    */
    public abstract String dropTable(String tableName);

    /**
    * @Description 添加表字段
    * @Author  rui
    * @Date   2019/7/31
    * @Param  tableName:表名     fields：字段及其字段约束  字段名--字段约束
    * @Return
    * @Exception
    */
    public abstract String addFields(String tableName,Map<String,String> fields);

    /**
    * @Description 删除表字段
    * @Author  rui
    * @Date   2019/7/31
    * @Param  tableName:表名     fields：字段名称数组
    * @Return
    * @Exception
    */
    public abstract String delFields(String tableName,String ... fields);

    /**
    * @Description 修改字段类型及长度
    * @Author  rui
    * @Date   2019/7/31
    * @Param  tableName:表名     fields：字段及其字段约束  字段名--字段约束
    * @Return
    * @Exception
    */
    public abstract String modifyFields(String tableName,Map<String,String> fields);

    /**
    * @Description 生成字段约束
    * @Author  rui
    * @Date   2019/7/31
    * @Param  key ：字段的类型 dataLenth 字段长度，
     *@Param  decimal类型默认及最大长度65 整数类型 mysql默认及最大长度20 postgresql默认及最大64 oracle默认及最大38
     * @Param  limit 只对decimal类型生效，默认2
    * @Return
    * @Exception
    */
    public abstract String getRestrict(KeywordsType key, Integer dataLenth, Integer limit);
}
