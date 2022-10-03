/*
 * Copyright 2019 github.com All right reserved. This software is the
 * confidential and proprietary information of github.com ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with github.com .
 */
package com.github.acticfox.shared.cache;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * 类RedisTest.java的实现描述：TODO 类实现描述
 * 
 * @author fanyong.kfy Jun 5, 2019 1:09:30 PM
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class RedisTest {

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@Test
	public void testRedisTemplate() {
		redisTemplate.opsForValue().set("123", "456");
		System.out.println("ans: " + redisTemplate.opsForValue().get("123"));
	}
}
