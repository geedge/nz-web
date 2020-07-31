package com.nis.common.config;

import com.baomidou.mybatisplus.core.incrementer.IKeyGenerator;
import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.extension.injector.LogicSqlInjector;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.nis.common.exception.NZException;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * mybatis-plus配置
 *

 */
@Configuration
public class MybatisPlusConfig {
	
    /**
     * 分页插件
     */
    @Bean
    public PaginationInterceptor paginationInterceptor() {
        return new PaginationInterceptor();
    }

    @Bean
    public SeqKeyGenerator seqKeyGenerator() {
    	return new SeqKeyGenerator();
    }
    
    @Bean
    public ISqlInjector sqlInjector() {
        return new LogicSqlInjector();
    }
    
    /**
     * 自定义主键生成器
     * @author fang
     *
     */
    static class SeqKeyGenerator implements IKeyGenerator {
    	@Autowired
    	DatabaseIdProvider databaseIdProvider;
    	@Autowired
    	DataSource dataSource;
    	
		@Override
		public String executeSql(String incrementerName) {
	        String databaseId = null;
			try {
				databaseId = databaseIdProvider.getDatabaseId(dataSource);
			} catch (SQLException e) {
				e.printStackTrace();
			}

			if(ToolUtil.isEmpty(databaseId)){
				throw new NZException(RCode.ERROR);
			}
			
			switch (databaseId){
	            case Constant.MYSQL:
	                return String.format("select seq_nextval('%s')", incrementerName);
	            case Constant.POSTGRESQL:
	                return String.format("select seq_nextval('%s')", incrementerName);
	            case Constant.ORACLE:
	                return String.format("select %s.nextval from dual", incrementerName);
	            default:
	                return String.format("select seq_nextval('%s')", incrementerName);
	        }
		}
    }
}
