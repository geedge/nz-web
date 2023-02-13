package com.nis.common.config;

import cn.hutool.log.Log;
import com.nis.common.utils.Tool;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

import java.net.UnknownHostException;
import java.time.Duration;

/**
 * redis 初始化配置
 * @author fang
 *
 */
@Configuration
@Data
public class RedisConfig {
	
	private static final Log log = Log.get();
	
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
		if(!Tool.ObjectUtil.isAllNotEmpty(this.getRedisHost(),this.getRedisPort(),this.getRedisDatabase())) {
			return null;
		}
		log.info("redis config: {}:{}/{}",this.getRedisHost(),this.getRedisPort(),this.getRedisDatabase());
		RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
		configuration.setDatabase(this.getRedisDatabase());
		configuration.setHostName(this.getRedisHost());
		configuration.setPort(this.getRedisPort());
		if(Tool.StrUtil.isNotBlank(this.getRedisPin())) {
			configuration.setPassword(this.getRedisPin());
		}
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMinIdle(this.getMinIdle());
		poolConfig.setMaxIdle(this.getMaxIdle());
		poolConfig.setMaxTotal(this.getMaxActive());
		poolConfig.setMaxWait(Duration.ofMillis(this.getMaxWait()));

		LettucePoolingClientConfiguration lettucePoolingClientConfiguration = LettucePoolingClientConfiguration.builder()
				.commandTimeout(Duration.ofMillis(this.getRedisTimeout()))
				.shutdownTimeout(Duration.ZERO)
				.poolConfig(poolConfig)
				.build();

		RedisConnectionFactory factory = new LettuceConnectionFactory(configuration, lettucePoolingClientConfiguration);
		// setValidateConnection
		//((LettuceConnectionFactory) factory).setValidateConnection(true);
		return factory;
	}
	
	
	@Bean(name = "redisTemplate")
	public RedisTemplate<?, ?> redisTemplate(RedisConnectionFactory redisConnectionFactory) throws UnknownHostException {
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
				if(Tool.StrUtil.isBlank(ping)) {
					log.error("redis connetion error");
				}
				return null;
			}
		});
		return template;
	}

	@Bean(name = "stringRedisTemplate")
	public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) throws UnknownHostException {
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


