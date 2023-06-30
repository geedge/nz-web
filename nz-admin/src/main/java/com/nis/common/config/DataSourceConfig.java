package com.nis.common.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
import com.alibaba.druid.pool.ha.HighAvailableDataSource;
import com.alibaba.druid.pool.ha.selector.DataSourceSelector;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;

import cn.hutool.db.Db;
import cn.hutool.db.ds.simple.SimpleDataSource;
import cn.hutool.log.Log;
import lombok.Data;

@Configuration
public class DataSourceConfig {

	private static final Log log = Log.get();

//	@Value("${database.host}")
//	private String databaseHost;
//	@Value("${database.port:3306}")
//	private int databasePort;
	/**
	 * 格式： IP:port, 例：192.168.44.36:3306,192.168.44.36:3307
	 */
	@Value("${database.url}")
	private String databaseUrl;
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
	/**
	 * 默认10s检查一次
	 */
	@Value("${ha.checkingIntervalSeconds:10}")
	private int haCheckingIntervalSeconds;
	/**
	 * 默认超过3次失败，放入黑名单
	 */
	@Value("${ha.blacklistThreshold:3}")
	private int haBlacklistThreshold;
	/**
	 * 默认延迟5分钟启动检查
	 */
	@Value("${ha.checkingDelaySeconds:300}")
	private int haCheckingDelaySeconds;
	
	@Data
	@Component
	@ConfigurationProperties("spring.datasource")
	static class DruidProperties implements InitializingBean {

		private Properties druid;

		@Override
		public void afterPropertiesSet() throws Exception {
			System.setProperty("druid.mysql.usePingMethod", "false");
			if (druid != null) {
				Properties temp = new Properties();
				druid.forEach((key, val) -> {
					String keyStr = Tool.StrUtil.toCamelCase(key.toString(), '-');
					keyStr = Tool.StrUtil.concat(true, "druid.", keyStr);
					temp.put(keyStr, val);
				});
				this.druid = temp;
			}
		}
	}

	/**
	 * 创建自定义数据源
	 * 
	 * @return
	 * @throws SQLException
	 */
	@Bean
	public DataSource dataSource() throws SQLException {
		if (!Tool.StrUtil.isAllNotBlank(this.databaseUrl, this.databaseUser, this.databaseName)) {
			return null;
		}
		/* 数据库密码解密 */
		databasePin = Tool.AesUtil.decrypt(this.databasePin, Constant.AES_SECRET_KEY);
		List<String> urlList = Tool.ListUtil.list(false, this.databaseUrl.split(","));
		urlList.sort(Comparator.naturalOrder());

		Map<String, DataSource> dataSourceMap = Tool.MapUtil.newHashMap();
		String[] urlArr = urlList.get(0).split(":");
		// 自动创建数据库
		this.createDatabase(urlArr[0], Tool.NumberUtil.parseInt(urlArr[1]), this.databaseUser, this.databasePin);
		// 创建 master datasource
		DruidDataSource dataSource = new DruidDataSource();
		// 设置 druid config
		dataSource.setConnectProperties(druidConfig.getDruid());
		dataSource.setUrl(this.getDbUrl(urlArr[0], Tool.NumberUtil.parseInt(urlArr[1])));
		dataSource.setUsername(this.databaseUser);
		dataSource.setPassword(this.databasePin);
		dataSource.init();
		dataSourceMap.put(MasterFirstDataSourceSelector.MASTER_KEY, dataSource);
		log.info("master datasource init, url: {}", dataSource.getUrl());
		if (urlList.size() > 1) {
			urlArr = urlList.get(1).split(":");
			DruidDataSource slaveDataSource = new DruidDataSource();
			// 设置 druid config
			slaveDataSource.setConnectProperties(druidConfig.getDruid());
			slaveDataSource.setUrl(this.getDbUrl(urlArr[0], Tool.NumberUtil.parseInt(urlArr[1])));
			slaveDataSource.setUsername(this.databaseUser);
			slaveDataSource.setPassword(this.databasePin);
			slaveDataSource.init();
			log.info("slave datasource init, url: {}", slaveDataSource.getUrl());
			dataSourceMap.put(MasterFirstDataSourceSelector.SLAVE_KEY, slaveDataSource);
		}
		// 创建高可用 DataSource
		HighAvailableDataSource haDS = new HighAvailableDataSource();
		DataSourceSelector selector = new MasterFirstDataSourceSelector(haDS);
		haDS.setDataSourceSelector(selector);
		haDS.setDataSourceMap(dataSourceMap);
		haDS.init();
		return haDS;
	}

	/**
	 * 返回data数据库的事务
	 * 
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
			this.transactionManagerCustomizers = transactionManagerCustomizers.getIfAvailable();
		}

		@Bean
		public TransactionDefinition transactionDefinition() {
			return new DefaultTransactionDefinition();
		}

		@Bean
		public DataSourceTransactionManager transactionManager() {
			DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(this.dataSource);
			if (this.transactionManagerCustomizers != null) {
				this.transactionManagerCustomizers.customize(transactionManager);
			}
			return transactionManager;
		}

	}

	/**
	 * 获取数据库连接url
	 * 
	 * @return
	 */
	private String getDbUrl(String host, int port) {
		return this.urlPattern.replace("{{host}}", host).replace("{{port}}", String.valueOf(port)).replace("{{dbName}}",
				this.databaseName);
	}

	/**
	 * 没有数据库时自动创建
	 */
	@SuppressWarnings("all")
	private void createDatabase(String host, int port, String username, String pin) {
		SimpleDataSource ds = null;
		try {
			String url = this.urlPattern.replace("{{host}}", host).replace("{{port}}", String.valueOf(port))
					.replace("/{{dbName}}", "");
			ds = new SimpleDataSource(url, username, pin, this.driver);
			Db db = Tool.DbUtil.use(ds);
			List<String> dbNames = db.query("show databases;", String.class);
			if (dbNames != null) {
				boolean match = dbNames.stream().anyMatch(databaseName::equalsIgnoreCase);
				if (!match) {
					int execute = db.execute("create database if not exists `" + this.databaseName
							+ "` default character set utf8mb4 COLLATE utf8mb4_general_ci");
					log.info("database {} not exists,auto create database,result:{}", databaseName, execute);
				}
			}
		} catch (Exception e) {
			log.error("init database error", e);
		} finally {
			Tool.DbUtil.close(ds);
		}
	}

	class MasterFirstDataSourceSelector implements DataSourceSelector {
		public static final String MASTER_KEY = "master";
		public static final String SLAVE_KEY = "slave";
		private HighAvailableDataSource highAvailableDataSource;
		private volatile boolean inited = false;
		private volatile Map<String, Long> lastSuccessTimes = new ConcurrentHashMap<String, Long>();

		public MasterFirstDataSourceSelector(HighAvailableDataSource highAvailableDataSource) {
			this.highAvailableDataSource = highAvailableDataSource;
		}

		@Override
		public DataSource get() {
			init();
			Map<String, DataSource> dataSourceMap = highAvailableDataSource.getAvailableDataSourceMap();
			if (dataSourceMap == null || dataSourceMap.isEmpty()) {
				return null;
			}
			if (dataSourceMap.size() == 1) {
				for (DataSource v : dataSourceMap.values()) {
					return v;
				}
			}
			return dataSourceMap.get(MASTER_KEY);
		}

		@Override
		public void setTarget(String name) {
		}

		@Override
		public void init() {
			if (inited) {
				return;
			}
			// 设置初始值
			highAvailableDataSource.getDataSourceMap().forEach((key, v) -> {
				lastSuccessTimes.put(key, Tool.DateUtil.current());
			});
			ScheduledThreadPoolExecutor executor = Tool.ThreadUtil.createScheduledExecutor(1);
			executor.scheduleWithFixedDelay(new Runnable() {
				@SuppressWarnings("static-access")
				@Override
				public void run() {
					Thread.currentThread().setName("HA datasource check");
					try {
						// 1、获取所有datasource
						Map<String, DataSource> dataSourceMap = highAvailableDataSource.getDataSourceMap();
						if (Tool.MapUtil.isEmpty(dataSourceMap)) {
							log.warn("no one datasource");
						}
						// 2、遍历datasource检查是否可用
						dataSourceMap.forEach((key, ds) -> {
							if (!(ds instanceof DruidDataSource)) {
								return;
							}
							DruidDataSource dds = (DruidDataSource) ds;
							Connection conn = null;
							try {
								conn = dds.getConnection(1000);
								dds.validateConnection(conn);
								lastSuccessTimes.put(key, Tool.DateUtil.current());
							} catch (Exception e) {
								log.warn("datasource error, key: {}, url: {}", key, dds.getUrl());
							} finally {
								Tool.DbUtil.close(conn);
							}
						});
						// 3、将不可用datasource 放入黑名单
						lastSuccessTimes.forEach((key, t) -> {
							if ((Tool.DateUtil.current() - t) / haCheckingIntervalSeconds
									/ 1000 > haBlacklistThreshold) {
								// 超过阈值 放入黑名单
								highAvailableDataSource.addBlackList(key);
								log.info("{} datasource last success time {},add to blacklist", key, t);
							} else {
								// 未超过阈值，从黑名单取出
								highAvailableDataSource.removeBlackList(key);
								log.info("{} datasource last success time {}", key, t);
							}
						});
					} catch (Exception e) {
						log.warn(e);
					}
				}
			}, haCheckingDelaySeconds, haCheckingIntervalSeconds, TimeUnit.SECONDS); // 延迟5分钟启动
			inited = true;
		}

		@Override
		public String getName() {
			return "master-first";
		}

		@Override
		public void destroy() {
		}

	}

}