package com.nis.common.config;

import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.nis.common.utils.Tool;

import cn.hutool.log.Log;
import lombok.Data;

/**
 * redis 初始化配置
 *
 * @author fang
 *
 */
@Configuration
@Data
public class RedisConfig {

	private static final Log log = Log.get();

	@Value("${redis.mode:standalone}")
	private String redisMode;

	@Value("${redis.host}")
	private String redisHost;
	@Value("${redis.port:6379}")
	private int redisPort;
	@Value("${redis.database:0}")
	private int redisDatabase;
	@Value("${redis.pin}")
	private String redisPin;
	@Value("${nezha.inited:0}")
	private int inited;
	// redis sentinel配置项
	@Value("${redis.sentinel.master}")
	private String sentinelMaster;
	@Value("${redis.sentinel.nodes}")
	private String sentinelNodes;
	/**
	 * redis配置项
	 */
	@Value("${spring.redis.jedis.pool.max-idle:10}")
	private int maxIdle;
	@Value("${spring.redis.jedis.pool.min-idle:3}")
	private int minIdle;
	@Value("${spring.redis.jedis.pool.max-wait:100000}")
	private int maxWait;
	@Value("${spring.redis.jedis.pool.max-active:100}")
	private int maxActive;
	@Value("${spring.redis.timeout:100000}")
	private long redisTimeout;

	@Bean(name = "redisConnectionFactory")
	public RedisConnectionFactory redisConnectionFactory() {

		GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
		poolConfig.setMinIdle(this.getMinIdle());
		poolConfig.setMaxIdle(this.getMaxIdle());
		poolConfig.setMaxTotal(this.getMaxActive());
		poolConfig.setMaxWaitMillis(this.getMaxWait());

		LettucePoolingClientConfiguration lettucePoolingClientConfiguration = LettucePoolingClientConfiguration
				.builder().commandTimeout(Duration.ofMillis(this.getRedisTimeout())).shutdownTimeout(Duration.ZERO)
				.poolConfig(poolConfig).build();

		RedisStandaloneConfiguration redisStandaloneConfiguration = null;
		RedisSentinelConfiguration redisSentinelConfiguration = null;
		RedisConnectionFactory factory;

		if (Tool.StrUtil.equalsIgnoreCase(redisMode, "standalone")) {
			log.info("standalone redis config: {}:{}/{}", this.getRedisHost(), this.getRedisPort(),this.getRedisDatabase());
			if (!Tool.ObjectUtil.isAllNotEmpty(this.getRedisHost(), this.getRedisPort(), this.getRedisDatabase())) {
				log.error("redis config error");
				return null;
			}
			redisStandaloneConfiguration = new RedisStandaloneConfiguration();
			redisStandaloneConfiguration.setDatabase(this.getRedisDatabase());
			redisStandaloneConfiguration.setHostName(this.getRedisHost());
			redisStandaloneConfiguration.setPort(this.getRedisPort());
			if (Tool.StrUtil.isNotBlank(this.getRedisPin())){
				redisStandaloneConfiguration.setPassword(this.getRedisPin());
			}
			factory = new LettuceConnectionFactory(redisStandaloneConfiguration, lettucePoolingClientConfiguration);
		} else if (Tool.StrUtil.equalsIgnoreCase(redisMode, "sentinel")) {
			log.info("sentinel redis config: {}, {}, {}", sentinelMaster, sentinelNodes, this.getRedisDatabase());
			if (!Tool.ObjectUtil.isAllNotEmpty(this.getSentinelMaster(), this.getSentinelNodes(),this.getRedisDatabase())) {
				log.error("redis config error");
				return null;
			}
			redisSentinelConfiguration = new RedisSentinelConfiguration();
			redisSentinelConfiguration.master(sentinelMaster);
			List<RedisNode> nodeList = Tool.ListUtil.list(false);
			String[] nodeArr = sentinelNodes.split(",");
			for (String node : nodeArr) {
				nodeList.add(new RedisNode(node.split(":")[0], Tool.NumberUtil.parseInt(node.split(":")[1])));
			}
			redisSentinelConfiguration.setSentinels(nodeList);
			redisSentinelConfiguration.setDatabase(redisDatabase);
			if (Tool.StrUtil.isNotBlank(redisPin)) {
				redisSentinelConfiguration.setPassword(redisPin);
			}
			factory = new LettuceConnectionFactory(redisSentinelConfiguration, lettucePoolingClientConfiguration);
		} else {
			log.error("redis config error, unknow redis mode: {}", redisMode);
			return null;
		}

		return factory;
	}

	@Bean(name = "redisTemplate")
	public RedisTemplate<?, ?> redisTemplate(RedisConnectionFactory redisConnectionFactory)
			throws UnknownHostException {
		RedisTemplate<?, ?> template = new RedisTemplate<>();

		template.setKeySerializer(new StringRedisSerializer());
		template.setHashKeySerializer(new StringRedisSerializer());
		template.setHashValueSerializer(new StringRedisSerializer());
		template.setValueSerializer(new StringRedisSerializer());
		template.setConnectionFactory(redisConnectionFactory);
		template.afterPropertiesSet();
		template.execute(new RedisCallback<Object>() {

			@Override
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				String ping = connection.ping();
				if (Tool.StrUtil.isBlank(ping)) {
					log.error("redis connetion error");
				}
				return null;
			}
		});
		return template;
	}

	@Bean(name = "stringRedisTemplate")
	public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory)
			throws UnknownHostException {
		StringRedisTemplate template = new StringRedisTemplate();
		template.setKeySerializer(new StringRedisSerializer());
		template.setHashKeySerializer(new StringRedisSerializer());
		template.setHashValueSerializer(new StringRedisSerializer());
		template.setValueSerializer(new StringRedisSerializer());
		template.setConnectionFactory(redisConnectionFactory);
		template.afterPropertiesSet();
		return template;
	}

}
