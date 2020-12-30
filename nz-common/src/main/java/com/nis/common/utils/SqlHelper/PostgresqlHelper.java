package com.nis.common.utils.SqlHelper;

import cn.hutool.log.Log;
import com.nis.common.utils.SQLUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PostgresqlHelper extends SQLUtils {
    private Log log= Log.get();

    private final String QUOTE="\"";

    public PostgresqlHelper(){
        keywords=new HashMap<KeywordsType, String>();

        keywords.put(KeywordsType.COMPUTE,"decimal");

        keywords.put(KeywordsType.NUMBER,"int8");

        keywords.put(KeywordsType.STRING,"varchar");

        keywords.put(KeywordsType.DATE,"timestamp default current_timestamp");

        keywords.put(KeywordsType.PRIMARYKEY,"int8 primary key");

    }

    @Override
    public String createTable(String tableName, Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("create table if not exists "+QUOTE+tableName+QUOTE+"(");
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
        sb.append("select pgt.tablename as name,pgd.description as comments from pg_tables pgt join pg_class pgc on pgt.tablename=pgc.relname ");
        sb.append("and pgt.schemaname='public' AND pgt.tablename ='" +tableName+"'");
        sb.append("left join pg_description pgd on pgd.objoid=pgc.oid and pgd.objsubid=0");

        log.debug("query tables SQL--> "+sb.toString());
        return sb.toString();
    }

    @Override
    public String addIndexs(String tableName, Map<String, String[]> indexs) {
        StringBuilder sb=new StringBuilder();
        Set<Map.Entry<String, String[]>> entries = indexs.entrySet();
        for(Map.Entry<String, String[]> entry:entries){
            String indexName=entry.getKey();
            String[] fields=entry.getValue();
            sb.append("create index "+QUOTE+indexName+QUOTE +"on"+QUOTE+tableName+QUOTE+"(");

            for(int i=0;i<fields.length;i++){
                String field=fields[i];
                sb.append(QUOTE+field+QUOTE);
                if(i<fields.length-1){
                    sb.append(",");
                }
            }
            sb.append(");");
        }

        log.debug("add index SQL--> "+sb.toString());
        return sb.toString();
    }

    @Override
    public String delIndexs(String tableName, String... indexNames) {
        StringBuilder sb=new StringBuilder();
        sb.append("drop index ");
        for(int i=0;i<indexNames.length;i++){
            String indexName=indexNames[i];
            sb.append(QUOTE+indexName+QUOTE);
            if(i<indexNames.length-1){
                sb.append(",");
            }
        }
        log.debug("drop index SQL--> "+sb.toString());
        return sb.toString();
    }

    @Override
    public String dropTable(String tableName) {
        String sql=("drop table if exists "+QUOTE+ tableName +QUOTE);
        log.debug("drop table SQL--> "+sql);
        return sql;
    }

    @Override
    public String addFields(String tableName, Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("alter table "+ QUOTE+tableName +QUOTE);

        Set<Map.Entry<String, String>> entries = fields.entrySet();
        int i=0;
        for(Map.Entry<String, String> entry:entries){
            String fieldName=entry.getKey();
            String restrict=entry.getValue();
            sb.append(" add column  "+QUOTE+fieldName+QUOTE+" "+restrict);
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
        sb.append("alter table "+QUOTE+ tableName +QUOTE);
        for(int i=0;i<fields.length;i++){
            String field=fields[i];
            sb.append(" drop column if exists "+QUOTE+field+QUOTE);
            if(i!=fields.length-1){
                sb.append(",");
            }
        }
        log.debug("drop fields SQL--> "+sb.toString());
        return sb.toString();
    }

    @Override
    public String modifyFields(String tableName, Map<String, String> fields) {
        StringBuilder sb=new StringBuilder();
        sb.append("ALTER TABLE "+QUOTE+tableName+QUOTE);
        int i=0;
        Set<Map.Entry<String, String>> entries = fields.entrySet();
        for(Map.Entry<String, String> entry:entries){
            String fieldName=entry.getKey();
            String restrict=entry.getValue();

            sb.append("alter column "+QUOTE+fieldName+QUOTE+" type "+restrict +" using "+QUOTE+fieldName+QUOTE+"::"+restrict);
            if(i<entries.size()-1){
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
            if(dataLenth<=16){
                return "int2";
            }else if(dataLenth>16&&dataLenth<=32){
                return "int4";
            }else{
                return "int8";
            }
        }else{
            dataLenth=dataLenth==null||dataLenth<1?255:dataLenth;
            return keywords.get(key)+"("+dataLenth+")";
        }
    }

}
