package com.nis.common.utils.SqlHelper;

import cn.hutool.log.Log;
import com.nis.common.utils.SQLUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 *  @author: rui
 *  @Date: 2019/7/31
 *  @Description: oracle数据库大小写敏感，表名、表字段、索引名称等都需要大写或用双引号包裹
 *  ps:使用双引号和没有双引号区别：不使用双引号，oracle会在执行时自动转为大写
 */
public class OracleHelper extends SQLUtils {
    private Log log= Log.get();

    private final String QUOTE="\"";

    public OracleHelper(){
        keywords=new HashMap<KeywordsType, String>();

        keywords.put(KeywordsType.COMPUTE,"DECIMAL");

        keywords.put(KeywordsType.NUMBER,"NUMBER");

        keywords.put(KeywordsType.STRING,"VARCHAR2");

        keywords.put(KeywordsType.DATE,"TIMESTAMP  DEFAULT CURRENT_TIMESTAMP");

        keywords.put(KeywordsType.PRIMARYKEY,"NUMBER(20) PRIMARY KEY");

    }

    @Override
    public String createTable(String tableName, Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("create  table "+QUOTE+tableName+QUOTE+"(");
        Set<Map.Entry<String, String>> entries = fields.entrySet();
        int i=0;
        for(Map.Entry<String,String> entry:entries){
            String field=entry.getKey();
            String restrict=entry.getValue();
            sb.append(QUOTE+field+QUOTE+" "+restrict);
            if(i!=entries.size()-1){
                sb.append(",");
            }
            i++;
        }
        sb.append(")");
        log.debug("create table SQL--> "+sb.toString());
        return sb.toString();
    }

    @Override
    public String queryTables(String tableName) {
        StringBuilder sb=new StringBuilder();
        sb.append("SELECT t.TABLE_NAME AS name,c.COMMENTS AS comments FROM user_tables t, user_tab_comments c WHERE t.table_name = c.table_name ");
        sb.append("AND t.TABLE_NAME =upper('"+tableName+"')  ORDER BY t.TABLE_NAME");

        log.debug("query tables SQL--> "+sb.toString());
        return sb.toString();
    }

    @Override
    public String addIndexs(String tableName, Map<String, String[]> indexs) {
        StringBuilder sb=new StringBuilder();
        Set<Map.Entry<String, String[]>> entries = indexs.entrySet();
        sb.append("begin ");
        for(Map.Entry<String, String[]> entry:entries){
            String indexName=entry.getKey();
            String[] fields=entry.getValue();
            sb.append("Execute immediate 'create  index "+QUOTE+indexName+QUOTE +" on "+QUOTE+tableName+QUOTE+"(");

            for(int i=0;i<fields.length;i++){
                String field=fields[i];
                sb.append(QUOTE+field+QUOTE);
                if(i<fields.length-1){
                    sb.append(",");
                }
            }
            sb.append(")';");
        }
        sb.append(" end;");
        log.debug("add index SQL--> "+sb.toString());
        return sb.toString();
    }

    @Override
    public String delIndexs(String tableName, String... indexNames) {
        StringBuilder sb=new StringBuilder();
        sb.append("begin ");
        for(int i=0;i<indexNames.length;i++){
            sb.append("Execute immediate 'drop index ");//多条语句一起使用mybatis执行时会报错ORA-00911: 无效字符，故采用这种拼接
            String indexName=indexNames[i];
            sb.append(QUOTE+indexName+QUOTE+"';");
        }
        sb.append(" end;");
        log.debug("drop index SQL--> "+sb.toString());
        return sb.toString();
    }

    @Override
    public String dropTable(String tableName) {
        String sql=("drop table  "+QUOTE+ tableName +QUOTE);
        log.debug("drop table SQL--> "+sql);
        return sql;
    }

    @Override
    public String addFields(String tableName, Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("alter table "+ QUOTE+tableName +QUOTE);

        Set<Map.Entry<String, String>> entries = fields.entrySet();
        for(Map.Entry<String, String> entry:entries){
            String fieldName=entry.getKey();
            String restrict=entry.getValue();
            sb.append(" add ("+QUOTE+fieldName+QUOTE+" "+restrict+")");
        }

        log.debug("add fields SQL--> "+sb.toString());
        return sb.toString();
    }

    @Override
    public String delFields(String tableName, String... fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("alter table "+QUOTE+ tableName +QUOTE +" drop(");
        for(int i=0;i<fields.length;i++){
            String field=fields[i];
            sb.append(QUOTE+field+QUOTE);
            if(i!=fields.length-1){
                sb.append(",");
            }
        }
        sb.append(")");
        log.debug("drop fields SQL--> "+sb.toString());
        return sb.toString();
    }

    @Override
    public String modifyFields(String tableName, Map<String, String> fields) {
        StringBuilder sb=new StringBuilder();
        Set<Map.Entry<String, String>> entries = fields.entrySet();
        sb.append("begin ");
        for(Map.Entry<String, String> entry:entries){
            sb.append("Execute immediate 'ALTER TABLE "+QUOTE+tableName+QUOTE);
            String fieldName=entry.getKey();
            String restrict=entry.getValue();

            sb.append(" modify("+QUOTE+fieldName+QUOTE+" "+restrict+")';");
        }
        sb.append("end;");
        log.debug("modify fields SQL--> "+sb.toString());
        return sb.toString();
    }

    @Override
    public String getRestrict(KeywordsType key, Integer dataLenth, Integer limit) {
        if(key.equals(KeywordsType.PRIMARYKEY)||key.equals(KeywordsType.DATE)){
            return keywords.get(key);
        }else if(key.equals(KeywordsType.COMPUTE)){
            dataLenth=(dataLenth==null||dataLenth>65||dataLenth<1)?65:dataLenth;
            limit=(limit==null||limit<1)?2:limit;
            return keywords.get(key)+"("+dataLenth+","+limit+")";
        }else if(key.equals(KeywordsType.NUMBER)){
            dataLenth=(dataLenth==null||dataLenth>38||dataLenth<1)?38:dataLenth;
            return keywords.get(key)+"("+dataLenth+")";
        }else{
            dataLenth=dataLenth==null||dataLenth<1?255:dataLenth;
            return keywords.get(key)+"("+dataLenth+")";
        }
    }

}
