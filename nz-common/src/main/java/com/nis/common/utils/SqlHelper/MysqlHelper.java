package com.nis.common.utils.SqlHelper;

import cn.hutool.log.Log;
import com.nis.common.utils.SQLUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MysqlHelper extends SQLUtils {
    private Log log= Log.get();

    public MysqlHelper(){
        keywords=new HashMap<KeywordsType, String>();

        keywords.put(KeywordsType.COMPUTE,"decimal");

        keywords.put(KeywordsType.NUMBER,"bigint");

        keywords.put(KeywordsType.STRING,"varchar");

        keywords.put(KeywordsType.DATE,"timestamp default current_timestamp");

        keywords.put(KeywordsType.PRIMARYKEY,"bigint(20) primary key");

    }

    @Override
    public  String createTable(String tableName, Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("create table if not exists `"+tableName+"`(");
        Set<Map.Entry<String, String>> entries = fields.entrySet();
        int i=0;
        for(Map.Entry<String,String> entry:entries){
            String field=entry.getKey();
            String restrict=entry.getValue();
            sb.append("`"+field+"` "+restrict);
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
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT t.table_name AS name,t.TABLE_COMMENT AS comments FROM information_schema.`TABLES` t WHERE t.TABLE_SCHEMA = (select database()) AND t.TABLE_NAME = ");
        sb.append("'"+tableName+"'");
        sb.append(" ORDER BY t.TABLE_NAME");

        log.debug("query tables SQL--> "+sb.toString());
        return sb.toString();
    }

    @Override
    public String addIndexs(String tableName, Map<String, String[]> indexs) {
        StringBuilder sb = new StringBuilder();
        sb.append("alter table `"+ tableName +"` ");

        Set<Map.Entry<String, String[]>> entries = indexs.entrySet();
        int i=0;
        for(Map.Entry<String, String[]> entry:entries){
            String indexName=entry.getKey();
            String[] fieldNames=entry.getValue();
            sb.append("add index if not exists `"+indexName+"`(");
            for(int j=0;j<fieldNames.length;j++){
                String fieldName=fieldNames[j];
                sb.append("`"+fieldName+"`");

                if(j!=fieldNames.length-1){
                    sb.append(",");
                }
            }
            sb.append(")");
            if(i!=entries.size()-1){
                sb.append(",");
            }
            i++;
        }

        log.debug("add index SQL--> "+sb.toString());
        return sb.toString();
    }

    @Override
    public String delIndexs(String tableName, String... indexNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("alter table `"+ tableName +"` ");
        for(int i=0;i<indexNames.length;i++){
            String indexName=indexNames[i];
            sb.append("drop index if exists `"+indexName+"`");
            if(i!=indexNames.length-1){
                sb.append(",");
            }
        }
        log.debug("drop index SQL--> "+sb.toString());
        return sb.toString();
    }

    @Override
    public String dropTable(String  tableName) {
        String sql=("drop table if exists `"+ tableName +"` ");
        log.debug("drop table SQL--> "+sql);
        return sql;
    }

    @Override
    public String addFields(String tableName, Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("alter table `"+ tableName +"` ");

        Set<Map.Entry<String, String>> entries = fields.entrySet();
        int i=0;
        for(Map.Entry<String, String> entry:entries){
            String fieldName=entry.getKey();
            String restrict=entry.getValue();
            sb.append("add column if not exists `"+fieldName+"`"+restrict);
            if(i!=entries.size()-1){
                sb.append(",");
            }
            i++;
        }

        log.debug("add fields SQL--> "+sb.toString());
        return sb.toString();
    }

    @Override
    public String delFields(String tableName, String... fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("alter table `"+ tableName +"` ");
        for(int i=0;i<fields.length;i++){
            String field=fields[i];
            sb.append("drop column if exists `"+field+"`");
            if(i!=fields.length-1){
                sb.append(",");
            }
        }
        log.debug("drop fields SQL--> "+sb.toString());
        return sb.toString();
    }

    @Override
    public String modifyFields(String tableName, Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("alter table `"+ tableName +"` ");

        Set<Map.Entry<String, String>> entries = fields.entrySet();
        int i=0;
        for(Map.Entry<String, String> entry:entries){
            String fieldName=entry.getKey();
            String restrict=entry.getValue();
            sb.append("modify column `"+fieldName+"` "+restrict);
            if(i!=entries.size()-1){
                sb.append(",");
            }
            i++;
        }

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
            dataLenth=dataLenth>20||dataLenth<1?20:dataLenth;
            return keywords.get(key)+"("+dataLenth+")";
        } else{
            dataLenth=dataLenth==null||dataLenth<1?255:dataLenth;
            return keywords.get(key)+"("+dataLenth+")";
        }
    }


}
