/*
 * Copyright 2019 github.com All right reserved. This software is the
 * confidential and proprietary information of github.com ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with github.com .
 */
package com.github.acticfox.redis.tools.starter;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import com.github.acticfox.redis.tools.starter.properties.RedisProperties;
import com.zhichubao.shared.distributed.common.RateLimitTemplate;
import com.zhichubao.shared.distributed.limit.RateLimitAspect;
import com.zhichubao.shared.distributed.lock.DistributedLockStore;
import com.zhichubao.shared.distributed.lock.LockConfig;
import com.zhichubao.shared.redis.cache.RedisCacheEngine;
import com.zhichubao.shared.redis.lock.RedisLockStore;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 类RedisToolsAutoConfiguration.java的实现描述：TODO 类实现描述
 *
 * @author fanyong.kfy Jun 11, 2019 10:41:06 AM
 */
@Configuration
@ConditionalOnClass({JedisConnection.class, RedisOperations.class, Jedis.class})
@EnableConfigurationProperties(RedisProperties.class)
@AutoConfigureBefore(RedisAutoConfiguration.class)
public class RedisToolsAutoConfiguration {

	/**
	 * Redis connection configuration.
	 */
	@Configuration
	@ConditionalOnClass(GenericObjectPool.class)
	protected static class RedisConnectionConfiguration {

		private final RedisProperties properties;

		public RedisConnectionConfiguration(RedisProperties properties) {
			this.properties = properties;
		}

		@Bean
		@ConditionalOnMissingBean(RedisConnectionFactory.class)
		public JedisConnectionFactory redisConnectionFactory() throws UnknownHostException {
			return applyProperties(createJedisConnectionFactory());
		}

		protected final JedisConnectionFactory applyProperties(JedisConnectionFactory factory) {
			configureConnection(factory);
			if (this.properties.isSsl()) {
				factory.setUseSsl(true);
			}
			factory.setDatabase(this.properties.getDatabase());
			if (this.properties.getTimeout() > 0) {
				factory.setTimeout(this.properties.getTimeout());
			}
			return factory;
		}

		private void configureConnection(JedisConnectionFactory factory) {
			if (StringUtils.hasText(this.properties.getUrl())) {
				configureConnectionFromUrl(factory);
			} else {
				factory.setHostName(this.properties.getHost());
				factory.setPort(this.properties.getPort());
				if (this.properties.getPassword() != null) {
					factory.setPassword(this.properties.getPassword());
				}
			}
		}

		private void configureConnectionFromUrl(JedisConnectionFactory factory) {
			String url = this.properties.getUrl();
			if (url.startsWith("rediss://")) {
				factory.setUseSsl(true);
			}
			try {
				URI uri = new URI(url);
				factory.setHostName(uri.getHost());
				factory.setPort(uri.getPort());
				if (uri.getUserInfo() != null) {
					String password = uri.getUserInfo();
					int index = password.lastIndexOf(":");
					if (index >= 0) {
						password = password.substring(index + 1);
					}
					factory.setPassword(password);
				}
			} catch (URISyntaxException ex) {
				throw new IllegalArgumentException("Malformed 'spring.redis.url' " + url, ex);
			}
		}

		private JedisConnectionFactory createJedisConnectionFactory() {
			JedisPoolConfig poolConfig = this.properties.getPool() != null ? jedisPoolConfig() : new JedisPoolConfig();

			return new JedisConnectionFactory(poolConfig);
		}

		private JedisPoolConfig jedisPoolConfig() {
			JedisPoolConfig config = new JedisPoolConfig();
			RedisProperties.Pool props = this.properties.getPool();
			config.setMaxTotal(props.getMaxActive());
			config.setMaxIdle(props.getMaxIdle());
			config.setMinIdle(props.getMinIdle());
			config.setMaxWaitMillis(props.getMaxWait());
			return config;
		}

	}

	/**
	 * Standard Redis configuration.
	 */
	@Configuration
	protected static class RedisConfiguration {

		@Bean
		@ConditionalOnMissingBean(name = "redisTemplate")
		public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory)
				throws UnknownHostException {
			RedisTemplate<String, Object> template = new RedisTemplate<String, Object>();
			template.setConnectionFactory(redisConnectionFactory);

			RedisSerializer<String> stringSerializer = new StringRedisSerializer();
			template.setKeySerializer(stringSerializer);
			template.setHashKeySerializer(stringSerializer);
			return template;
		}

		@Bean
		@ConditionalOnMissingBean(StringRedisTemplate.class)
		public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory)
				throws UnknownHostException {
			StringRedisTemplate template = new StringRedisTemplate();
			template.setConnectionFactory(redisConnectionFactory);
			return template;
		}

	}

	@Configuration
	protected static class RedisCacheConfiguration {
		@Bean(name = "redisCacheEngine")
		@ConditionalOnMissingBean(RedisCacheEngine.class)
		public RedisCacheEngine redisCacheEngine(RedisTemplate<String, Object> template) throws UnknownHostException {
			RedisCacheEngine cacheEngine = new RedisCacheEngine();
			cacheEngine.setRedisTemplate(template);
			return cacheEngine;
		}

		@Bean
		@ConditionalOnMissingBean(RedisLockStore.class)
		public RedisLockStore lockStore(StringRedisTemplate template) throws UnknownHostException {
			RedisLockStore lockStore = new RedisLockStore();
			lockStore.setRedisTemplate(template);
			return lockStore;
		}

		@Bean(initMethod = "init")
		@ConditionalOnMissingBean(LockConfig.class)
		public LockConfig lockConfig(DistributedLockStore lockStore) throws UnknownHostException {
			LockConfig lockConfig = new LockConfig();
			lockConfig.setDistributedLockStore(lockStore);
			return lockConfig;
		}

		@Bean
		@ConditionalOnMissingBean(RateLimitTemplate.class)
		public RateLimitTemplate rateLimitTemplate(RedisCacheEngine redisCacheEngine) throws UnknownHostException {
			RateLimitTemplate rateLimitTemplate = new RateLimitTemplate();
			rateLimitTemplate.setCacheEngine(redisCacheEngine);

			return rateLimitTemplate;
		}

		@Bean
		@ConditionalOnMissingBean(RateLimitAspect.class)
		public RateLimitAspect rateLimitAspect(RateLimitTemplate rateLimitTemplate) throws UnknownHostException {
			RateLimitAspect rateLimitAspect = new RateLimitAspect();
			rateLimitAspect.setRateLimitTemplate(rateLimitTemplate);

			return rateLimitAspect;
		}

	}

}
