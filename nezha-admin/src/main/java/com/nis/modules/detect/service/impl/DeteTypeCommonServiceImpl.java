package com.nis.modules.detect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.detect.service.DeteTypeCommonService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeteTypeCommonServiceImpl<M extends BaseMapper<T>,T> extends ServiceImpl<M ,T> implements DeteTypeCommonService<T>  {

    public static final Set<String> DB_KEYWORD_SET=new HashSet<String>();

    public boolean checkFieldExists(String field,String name){

        return checkFieldExists(null,field,name);
    }
    public boolean checkFieldExists(Long id,String field,String name){
        List<T> list = this.list(new QueryWrapper<T>()
                .ne(id!=null,"id",id)
                .eq(StringUtils.isNotEmpty(name)&&StringUtils.isNotEmpty(field),field, name));

        return list!=null&&list.size()>0;
    }
    @Override
    public boolean checkFieldExists(String field, String name, Long typeId) {
        List<T> list = this.list(new QueryWrapper<T>()
                .eq(typeId!=null&&typeId>0,"type_id",typeId)
                .eq(StringUtils.isNotEmpty(name)&&StringUtils.isNotEmpty(field),field, name));

        return list!=null&&list.size()>0;
    }
    /**
     * @Description去除字符串中的特殊字符
     * @Author  rui
     * @Date   2019/7/26
     * @Param
     * @Return
     * @Exception
     */
    public String formatString(String str) {

        return str.replaceAll("\\W", "").toUpperCase();
    }

}
