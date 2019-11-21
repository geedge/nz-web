/**

 *

 *

 */

package com.nis.common.aspect;


import com.nis.common.exception.NZException;

import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nis.common.annotation.DataFilter;
import com.nis.common.utils.Constant;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysUserRoleService;
import com.nis.modules.sys.shiro.ShiroUtils;

import java.util.*;

/**
 * 数据过滤，切面处理类
 *

 */
@Aspect
@Component
public class DataFilterAspect {
    @Autowired
    private SysUserRoleService sysUserRoleService;

    @Pointcut("@annotation(com.nis.common.annotation.DataFilter)")
    public void dataFilterCut() {

    }

    @Before("dataFilterCut()")
    public void dataFilter(JoinPoint point) throws Throwable {
        Object params = point.getArgs()[0];
        if(params != null && params instanceof Map){
            SysUserEntity user = ShiroUtils.getUserEntity();

            //如果不是超级管理员，则进行数据过滤
            if(user.getUserId() != Constant.SUPER_ADMIN){
                Map map = (Map)params;
                map.put(Constant.SQL_FILTER, getSQLFilter(user, point));
            }

            return ;
        }

        throw new NZException("数据权限接口，只能是Map类型参数，且不能为NULL");
    }

    /**
     * 获取数据过滤的SQL
     */
    private String getSQLFilter(SysUserEntity user, JoinPoint point){
        MethodSignature signature = (MethodSignature) point.getSignature();
        DataFilter dataFilter = signature.getMethod().getAnnotation(DataFilter.class);
        //获取表的别名
        String tableAlias = dataFilter.tableAlias();
        if(StringUtils.isNotBlank(tableAlias)){
            tableAlias +=  ".";
        }

        StringBuilder sqlFilter = new StringBuilder();
        sqlFilter.append(" (");



        sqlFilter.append(")");

        if(sqlFilter.toString().trim().equals("()")){
            return null;
        }

        return sqlFilter.toString();
    }
}
