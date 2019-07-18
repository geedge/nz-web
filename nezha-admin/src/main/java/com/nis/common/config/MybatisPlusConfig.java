/**

 *

 *

 */

package com.nis.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.baomidou.mybatisplus.core.incrementer.IKeyGenerator;
import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.extension.injector.LogicSqlInjector;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;

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
		@Override
		public String executeSql(String incrementerName) {
			return String.format("select seq_nextval('%s')", incrementerName);
		}
    }
    
}
