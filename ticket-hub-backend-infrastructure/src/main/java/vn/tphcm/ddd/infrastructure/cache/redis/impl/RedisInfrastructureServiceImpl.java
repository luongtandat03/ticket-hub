/*
 * @ (#) RedisInfrastructureServiceImpl.java       1.0     8/5/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.cache.redis.impl;
/*
 * @author: Luong Tan Dat
 * @date: 8/5/2026
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import vn.tphcm.ddd.infrastructure.cache.redis.RedisInfrastructureService;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j(topic = "REDIS-INFRASTRUCTURE-SERVICE")
@RequiredArgsConstructor
public class RedisInfrastructureServiceImpl implements RedisInfrastructureService {
    private static final Duration OBJECT_TTL = Duration.ofMinutes(15);

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper;

    @Override
    public void setObject(String key, Object value) {
        if (!StringUtils.hasLength(key)) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, value, OBJECT_TTL);
        } catch (Exception e) {
            log.error("setObject error : {}", e.getMessage());
        }
    }

    @Override
    public <T> T getObject(String key, Class<T> targetClass) {
        try {
            Object result = redisTemplate.opsForValue().get(key);
            log.info("getObject result:{}", result);

            if (result == null) {
                return null;
            }

            return objectMapper.convertValue(result, targetClass);
        } catch (Exception e) {
            log.error("Failed to get cached : {}", e.getMessage());
            deleteObject(key);
        }

        return null;
    }

    @Override
    public void deleteObject(String key) {
        try {
            log.info("Deleted key:{}", key);
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Failed to delete cached : {}", e.getMessage());
        }
    }

    @Override
    public RedisTemplate<String, Object> redisTemplate() {
        return redisTemplate;
    }

    @Override
    public void zAdd(String key, Object member, long expire) {
        if (!StringUtils.hasLength(key)) {
            return;
        }
        try {
            redisTemplate.opsForZSet().add(key, member, System.currentTimeMillis());
            redisTemplate.expire(key, expire, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("zAdd error : {}", e.getMessage());
        }
    }

    @Override
    public Set<String> zRangeByScore(String key, long start, long end, int limit) {
        try {
            Set<Object> result = redisTemplate.opsForZSet().rangeByScore(key, start, end, 0, limit);

            return result != null ? result.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toSet()) : new HashSet<>();
        } catch (Exception e) {
            log.error("zRangeByScore error : {}", e.getMessage());
        }

        return Set.of();
    }

    @Override
    public void zRemove(String key, String rawMember) {
        try {
            log.info("Remove key: {}", key);
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Failed to remove key : {}", e.getMessage());
        }
    }
}
