/*
 * Copyright 2019 zhichubao.com All right reserved. This software is the confidential and proprietary information of
 * zhichubao.com ("Confidential Information"). You shall not disclose such Confidential Information and shall use it
 * only in accordance with the terms of the license agreement you entered into with zhichubao.com .
 */
package com.github.acticfox.redis.lock;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisCommand;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.github.acticfox.distributed.lock.DistributedLockStore;
import com.github.acticfox.distributed.lock.InvokeResult;

/**
 * 类RedisLockStore.java的实现描述：TODO 类实现描述
 *
 * @author fanyong.kfy Jun 3, 2019 12:11:43 PM
 */
public class RedisLockStore implements DistributedLockStore {

    private static final String LOCK_PREFIX = "mutex_lock_";

    private static final byte[] NX = "NX".getBytes();
    private static final byte[] EX = "EX".getBytes();

    private StringRedisTemplate redisTemplate;

    private String generateKey(String resource) {
        return LOCK_PREFIX + resource;
    }

    public StringRedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public InvokeResult<Boolean> lock(String resource, String lockToken, int expireTimeInSecond) {
        try {
            Object result = redisTemplate.execute((RedisCallback<?>)connection -> {
                RedisSerializer keySerializer = redisTemplate.getKeySerializer();
                RedisSerializer valueSerializer = redisTemplate.getValueSerializer();
                // SET key value [EX seconds] [PX milliseconds] [NX|XX]
                byte[][] args = {keySerializer.serialize(generateKey(resource)), valueSerializer.serialize(lockToken),
                    NX, EX, String.valueOf(expireTimeInSecond).getBytes()};
                return connection.execute(RedisCommand.SET.toString(), args);
            });
            return InvokeResult.newInvokeResult(true, result != null);
        } catch (Throwable e) {
            return InvokeResult.newInvokeResult(false, false);
        }
    }

    @Override
    public InvokeResult<Boolean> unlock(String resource, String lockToken) {
        String value = redisTemplate.opsForValue().get(generateKey(resource));
        if (!StringUtils.equals(value, lockToken)) {
            return InvokeResult.newInvokeResult(false, false);
        } else {
            try {
                redisTemplate.delete(generateKey(resource));
            } catch (Throwable e) {
                return InvokeResult.newInvokeResult(false, false);
            }
        }

        return InvokeResult.newInvokeResult(true, true);
    }

    @Override
    public InvokeResult<Boolean> updateLockExpireTime(String resource, String lockToken, int expireTimeInSecond) {
        String value = redisTemplate.opsForValue().get(generateKey(resource));
        if (!StringUtils.equals(value, lockToken)) {
            return InvokeResult.newInvokeResult(false, false);
        }
        try {
            redisTemplate.opsForValue().set(generateKey(resource), lockToken, expireTimeInSecond, TimeUnit.SECONDS);
        } catch (Throwable e) {
            return InvokeResult.newInvokeResult(false, false);
        }

        return InvokeResult.newInvokeResult(true, true);
    }

}
