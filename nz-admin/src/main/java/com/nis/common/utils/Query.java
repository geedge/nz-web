package com.nis.common.utils;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nis.common.exception.NZException;
import com.nis.common.xss.SQLFilter;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 查询参数
 */
public class Query {

    private Class clz;

    public Class<? extends Object> getClz() {
        return clz;
    }

    public void setClz(Class clz) {
        this.clz = clz;
    }

    public Query(Class clz) {
        this.clz = clz;
    }

    public IPage getPage(Map<String, Object> params) {
        return this.getPage(params, null, false);
    }

    public IPage getPage(Map<String, Object> params, String defaultOrderField, boolean isAsc) {
        //分页参数
        long curPage = 1;
        long limit = Constant.PAGESIZE;

        if(params.get(Constant.PAGE) != null){
            curPage = Long.parseLong((String)params.get(Constant.PAGE));
        }
        if(params.get(Constant.LIMIT) != null){
            limit = Long.parseLong((String)params.get(Constant.LIMIT));
            if(limit == -1){
                limit = Long.MAX_VALUE;
            }
        }

        //分页对象
        Page page = new Page(curPage, limit);

        //分页参数
        params.put(Constant.PAGE, page);

        //排序字段  orderBy=id
        //防止SQL注入（因为sidx、order是通过拼接SQL实现排序的，会有SQL注入风险）
        String orderField = SQLFilter.sqlInject((String)params.get(Constant.ORDER));

        if (StringUtils.isNotEmpty(orderField)) {
            boolean matcheFlag = orderField.trim().matches("-?[a-zA-Z_.-]+");
            if (!matcheFlag) {
                throw new NZException(RCode.ERROR);
            }

            // 获取表名
            Class<?> clz = this.getClz();
            String tableName = "";
            if (clz != null) {
                TableName table = this.getClz().getAnnotation(TableName.class);
                tableName = table.value();
            }
            // 通过表名获取排序字段映射
            Map<String, String> columnAliasMap = Constant.TABLE_NAME_ORDER_FIELD_MAPPING.get(tableName);
            columnAliasMap = ToolUtil.isEmpty(columnAliasMap) ? new HashMap<>():columnAliasMap;
            if (orderField.startsWith("-")) {
                orderField = orderField.substring(1, orderField.length());
                orderField = columnAliasMap.get(orderField) != null ? columnAliasMap.get(orderField) : orderField;
                return page.setDesc(orderField);
            } else {
                orderField = columnAliasMap.get(orderField) != null ? columnAliasMap.get(orderField) : orderField;
                return page.setAsc(orderField);
            }
        }

        // 默认排序
        if(isAsc) {
            page.setAsc(defaultOrderField);
        }else {
            page.setDesc(defaultOrderField);
        }

        return page;
    }
}
