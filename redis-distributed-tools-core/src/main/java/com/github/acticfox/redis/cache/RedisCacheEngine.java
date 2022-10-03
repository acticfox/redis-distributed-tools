/*
 * Copyright 2019 zhichubao.com All right reserved. This software is the confidential and proprietary information of
 * zhichubao.com ("Confidential Information"). You shall not disclose such Confidential Information and shall use it
 * only in accordance with the terms of the license agreement you entered into with zhichubao.com .
 */
package com.github.acticfox.redis.cache;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;

import com.github.acticfox.distributed.cache.CacheEngine;

/**
 * 类RedisCacheEngine.java的实现描述：TODO 类实现描述
 * 
 * @author fanyong.kfy Jun 3, 2019 12:12:20 PM
 */
/**
 * 类RedisCacheEngine.java的实现描述：TODO 类实现描述
 * 
 * @author fanyong.kfy Jun 17, 2019 12:55:06 PM
 */
public class RedisCacheEngine implements CacheEngine {

    private RedisTemplate<String, Object> redisTemplate;

    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String get(String key) {
        // TODO Auto-generated method stub
        return (String)redisTemplate.opsForValue().get(key);
    }

    @Override
    public <T> T get(String key, Class<T> clz) {
        return clz.cast(redisTemplate.opsForValue().get(key));
    }

    @Override
    public <T extends Serializable> boolean put(String key, T value) {
        redisTemplate.opsForValue().set(key, value);
        return true;
    }

    @Override
    public <T extends Serializable> boolean put(String key, T value, int expiredTime, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, expiredTime, unit);
        return true;
    }

    @Override
    public boolean invalid(String key) {
        redisTemplate.delete(key);
        return true;
    }

    @Override
    public long incrCount(String key, int expireTime, TimeUnit unit) {
        long result = redisTemplate.opsForValue().increment(key, 1L).intValue();
        if (result == 1) {
            redisTemplate.expire(key, unit.toSeconds(expireTime), TimeUnit.SECONDS);
        }
        return result;
    }

    @Override
    public boolean rateLimit(String key, int rateThreshold, int expireTime, TimeUnit unit) {
        long result = redisTemplate.opsForValue().increment(key, 1L).intValue();
        if (result == 1) {
            redisTemplate.expire(key, unit.toSeconds(expireTime), TimeUnit.SECONDS);
        }
        return result > rateThreshold;
    }

}
