/*
 * Copyright 2019 github.com All right reserved. This software is the
 * confidential and proprietary information of github.com ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with github.com .
 */
package com.github.acticfox.redis.tools.starter.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 类TairsExtendProperties.java的实现描述：TODO 类实现描述
 * 
 * @author fanyong.kfy May 22, 2019 2:35:54 PM
 */
@ConfigurationProperties(prefix = "redis.idempotent")
public class RedisIdempotentProperties {

    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
