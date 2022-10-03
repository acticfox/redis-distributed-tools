/*
 * Copyright 2019 github.com All right reserved. This software is the
 * confidential and proprietary information of github.com ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with github.com .
 */
package com.github.acticfox.redis.tools.starter;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import com.github.acticfox.redis.tools.starter.properties.RedisIdempotentProperties;
import com.zhichubao.shared.distributed.common.IdempotentTemplate;
import com.zhichubao.shared.distributed.idempotent.ServiceIdempotentAspect;
import com.zhichubao.shared.distributed.idempotent.dao.CommonIdempotentResultDao;
import com.zhichubao.shared.distributed.idempotent.service.IdempotentService;

/**
 * 类TairAutoConfiguration.java的实现描述：TODO 类实现描述
 * 
 * @author fanyong.kfy Jan 3, 2019 10:51:44 AM
 */
@Configuration
@EnableConfigurationProperties(RedisIdempotentProperties.class)
@ConditionalOnProperty(name = "redis.idempotent.enabled", havingValue = "true")
@Import({RedisIdemmpotentAutoConfiguration.RedisIdempotentRegistrar.class})
@AutoConfigureAfter(RedisToolsAutoConfiguration.class)
public class RedisIdemmpotentAutoConfiguration {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(RedisIdemmpotentAutoConfiguration.class);

	public static class RedisIdempotentRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

		private ConfigurableEnvironment environment;

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.context.EnvironmentAware#setEnvironment(org. springframework.core.env.Environment)
		 */
		@Override
		public void setEnvironment(Environment environment) {
			this.environment = (ConfigurableEnvironment) environment;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.context.annotation.ImportBeanDefinitionRegistrar#
		 * registerBeanDefinitions(org.springframework.core.type. AnnotationMetadata,
		 * org.springframework.beans.factory.support.BeanDefinitionRegistry)
		 */
		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			// 创建firstIdempotentDao
			BeanDefinitionBuilder firstIdempotentDaoBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(CommonIdempotentResultDao.class);
			firstIdempotentDaoBuilder.addPropertyValue("cacheEngine", registry.getBeanDefinition("redisCacheEngine"));
			registry.registerBeanDefinition("redisIdempotentResultDao", firstIdempotentDaoBuilder.getBeanDefinition());

			// 创建IdempotentService
			BeanDefinitionBuilder idempotentServiceBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(IdempotentService.class);
			idempotentServiceBuilder.addPropertyValue("firstLevelIdempotentResultDao",
					registry.getBeanDefinition("redisIdempotentResultDao"));
			registry.registerBeanDefinition("idempotentService", idempotentServiceBuilder.getBeanDefinition());

			// 创建IdempotentTemplate
			BeanDefinitionBuilder idempotentTemplateBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(IdempotentTemplate.class);
			idempotentTemplateBuilder.addPropertyValue("idempotentService",
					registry.getBeanDefinition("idempotentService"));
			registry.registerBeanDefinition("idempotentTemplate", idempotentTemplateBuilder.getBeanDefinition());

			// 创建ServiceIdempotentAspect
			BeanDefinitionBuilder serviceIdempotentAspectBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(ServiceIdempotentAspect.class);
			serviceIdempotentAspectBuilder.addPropertyValue("idempotentTemplate",
					registry.getBeanDefinition("idempotentTemplate"));
			registry.registerBeanDefinition("serviceIdempotentAspect",
					serviceIdempotentAspectBuilder.getBeanDefinition());

		}

	}

}
