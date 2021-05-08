package com.nis.common.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.alibaba.druid.pool.DruidDataSource;
import com.nis.common.utils.Tool;

@Configuration
public class DataSourceConfig {

	@Value("${database.host}")
	private String databaseHost;
	@Value("${database.port:3306}")
	private int databasePort;
	@Value("${database.name}")
	private String databaseName;
	@Value("${database.user}")
	private String databaseUser;
	@Value("${database.pin}")
	private String databasePin;
    @Value("${mybatis-plus.mapper-locations}")
	private String mapperLocation;
    
    
	/**
	 * 数据源配置项
	 */
	@Value("${spring.datasource.druid.driver-class-name:com.mysql.jdbc.Driver}")
	private String driver;
	@Value("${spring.datasource.druid.url-pattern:jdbc:mysql://{{host}}:{{port}}/{{dbName}}?allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8&useSSL=false}")
	private String urlPattern;
	@Value("${spring.datasource.druid.initial-size:10}")
	private int initialSize;
	@Value("${spring.datasource.druid.max-active:100}")
	private int dbMaxActive;
	@Value("${spring.datasource.druid.min-idle:5}")
	private int dbMinIdle;
	@Value("${spring.datasource.druid.max-wait:60000}")
	private int dbMaxWait;
	@Value("${spring.datasource.druid.pool-prepared-statements:true}")
	private boolean poolPreparedStatements;
	@Value("${spring.datasource.druid.validation-query:SELECT 1}")
	private String validationQuery;
	
    
	/**
	 * 创建自定义数据源
	 * @return
	 */
	@Bean
	public DataSource dataSource() {
		if(!Tool.StrUtil.isAllNotBlank(this.databaseHost,this.databaseUser,this.databaseName)) {
        	return null;
        }
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(this.getDbUrl());
        dataSource.setUsername(this.databaseUser);
        dataSource.setPassword(this.databasePin);
        dataSource.setDriverClassName(this.driver);
        dataSource.setInitialSize(this.initialSize);
        dataSource.setMaxActive(this.dbMaxActive);
        dataSource.setMinIdle(this.dbMinIdle);
        dataSource.setMaxWait(this.dbMaxWait);
        dataSource.setPoolPreparedStatements(this.poolPreparedStatements);
        dataSource.setValidationQuery(this.validationQuery);
        return dataSource;
	}

    /** 返回data数据库的事务
     * @param ds
     * @return
     **/
	@Configuration
	static class DataSourceTransactionManagerConfiguration {

		private final DataSource dataSource;

		private final TransactionManagerCustomizers transactionManagerCustomizers;

		DataSourceTransactionManagerConfiguration(DataSource dataSource,
				ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
			this.dataSource = dataSource;
			this.transactionManagerCustomizers = transactionManagerCustomizers
					.getIfAvailable();
		}

		@Bean
		public TransactionDefinition transactionDefinition() {
			return new DefaultTransactionDefinition();
		}
		
		@Bean
		public DataSourceTransactionManager transactionManager() {
			DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(
					this.dataSource);
			if (this.transactionManagerCustomizers != null) {
				this.transactionManagerCustomizers.customize(transactionManager);
			}
			return transactionManager;
		}

	}
    
	/**
	 * 获取数据库连接url
	 * @return
	 */
	private String getDbUrl() {
		return this.urlPattern.replace("{{host}}", this.databaseHost)
				.replace("{{port}}", String.valueOf(this.databasePort))
				.replace("{{dbName}}",  this.databaseName);
	}
}