/*
 * @ (#) RedissonConfig.java       1.0     8/5/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.distributed.redisson.config;
/*
 * @author: Luong Tan Dat
 * @date: 8/5/2026
 */

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private String port;

    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + host + ":" + port).setConnectionPoolSize(50).setDatabase(0);

        return Redisson.create(config);
    }
}
