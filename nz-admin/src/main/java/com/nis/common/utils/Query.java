/**

 *

 *

 */

package com.nis.common.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nis.common.exception.NZException;
import com.nis.common.xss.SQLFilter;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * 查询参数
 *

 */
public class Query<T> {

    public IPage<T> getPage(Map<String, Object> params) {
        return this.getPage(params, null, false);
    }

    public IPage<T> getPage(Map<String, Object> params, String defaultOrderField, boolean isAsc) {
        //分页参数
        long curPage = 1;
        long limit = Constant.PAGESIZE;

        if(params.get(Constant.PAGE) != null){
            curPage = Long.parseLong((String)params.get(Constant.PAGE));
        }
        if(params.get(Constant.LIMIT) != null){
            limit = Long.parseLong((String)params.get(Constant.LIMIT));
        }

        //分页对象
        Page<T> page = new Page<>(curPage, limit);

        //分页参数
        params.put(Constant.PAGE, page);

        //排序字段  orderBy=id
        //防止SQL注入（因为sidx、order是通过拼接SQL实现排序的，会有SQL注入风险）
        String orderField = SQLFilter.sqlInject((String)params.get(Constant.ORDER));
//        String order = (String)params.get(Constant.ORDER);
        
        if(StringUtils.isNotEmpty(orderField)) {
        	boolean matcheFlag = orderField.trim().matches("-?[a-zA-Z_.-]+");
        	if(!matcheFlag) {
            	throw new NZException(RCode.ERROR);
            }
            
            if(orderField.startsWith("-")) {
            	orderField=orderField.substring(1,orderField.length());
            	return page.setDesc(orderField);
            }else {
            	return page.setAsc(orderField);
            }
            
        }
        
        //前端字段排序
       /* if(StringUtils.isNotEmpty(orderField) && StringUtils.isNotEmpty(order)){
            if(Constant.ASC.equalsIgnoreCase(order)) {
                return page.setAsc(orderField);
            }else {
                return page.setDesc(orderField);
            }
        }*/

        //默认排序
        if(isAsc) {
            page.setAsc(defaultOrderField);
        }else {
            page.setDesc(defaultOrderField);
        }

        return page;
    }
}
