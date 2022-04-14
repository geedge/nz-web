package com.nis.common.config;

import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.alibaba.druid.pool.DruidDataSource;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;

import cn.hutool.db.Db;
import cn.hutool.db.ds.simple.SimpleDataSource;
import cn.hutool.log.Log;
import lombok.Data;

@Configuration
public class DataSourceConfig {

	private static final Log log = Log.get();
	
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
    @Autowired
    private DruidProperties druidConfig;
    
	/**
	 * 数据源配置项
	 */
	@Value("${spring.datasource.druid.url-pattern:jdbc:mariadb://{{host}}:{{port}}/{{dbName}}?allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8&useSSL=false&useMysqlMetadata=true}")
	private String urlPattern;
	@Value("${spring.datasource.druid.driver-class-name:com.mysql.cj.jdbc.Driver}")
	private String driver;
	
	@Data
	@Component
	@ConfigurationProperties("spring.datasource")
	static class DruidProperties implements InitializingBean{
		
		private Properties druid;

		@Override
		public void afterPropertiesSet() throws Exception {
			System.setProperty("druid.mysql.usePingMethod","false");
			if(druid != null) {
				Properties temp = new Properties();
				druid.forEach((key,val) -> {
					String keyStr = Tool.StrUtil.toCamelCase(key.toString(),'-');
					keyStr = Tool.StrUtil.concat(true, "druid.",keyStr);
					temp.put(keyStr, val);
				});
				this.druid = temp;
			}
		}
	}
	
	/**
	 * 创建自定义数据源
	 * @return
	 * @throws SQLException 
	 */
	@Bean
	public DataSource dataSource() throws SQLException {
		if(!Tool.StrUtil.isAllNotBlank(this.databaseHost,this.databaseUser,this.databaseName)) {
        	return null;
        }
		/* 数据库密码解密 */
		databasePin = Tool.AesUtil.decrypt(this.databasePin, Constant.AES_SECRET_KEY);
		//初始化时创建 数据库
		this.createDatabase();
		log.info("datasource config,user:{}, url: {}",this.databaseUser,this.getDbUrl());
        DruidDataSource dataSource = new DruidDataSource();
        //设置 druid config
        dataSource.setConnectProperties(druidConfig.getDruid());
        dataSource.setUrl(this.getDbUrl());
        dataSource.setUsername(this.databaseUser);
        dataSource.setPassword(this.databasePin);
        dataSource.init();
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
	
	/**
	 * 没有数据库时自动创建
	 */
	@SuppressWarnings("all")
	private void createDatabase() {
		SimpleDataSource dataSource = null;
		try {
			String url = this.urlPattern.replace("{{host}}", this.databaseHost)
					.replace("{{port}}", String.valueOf(this.databasePort)).replace("/{{dbName}}", "");
			dataSource = new SimpleDataSource(url, this.databaseUser, this.databasePin, this.driver);
			Db db = Tool.DbUtil.use(dataSource);
			List<String> dbNames = db.query("show databases;", String.class);
			if(dbNames != null) {
				boolean match = dbNames.stream().anyMatch(databaseName::equalsIgnoreCase);
				if(!match) {
					int execute = db.execute("create database if not exists `" + this.databaseName + "` default character set utf8mb4 COLLATE utf8mb4_general_ci");
					log.info("database {} not exists,auto create database,result:{}",databaseName,execute);
				}
			}
		} catch (Exception e) {
			log.error("init database error",e);
		}finally {
			Tool.DbUtil.close(dataSource);
		}
	}
}