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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import vn.tphcm.ddd.infrastructure.cache.redis.RedisInfrastructureService;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j(topic = "REDIS-INFRASTRUCTURE-SERVICE")
@RequiredArgsConstructor
public class RedisInfrastructureServiceImpl implements RedisInfrastructureService {
    private static final Duration OBJECT_TTL = Duration.ofMinutes(15);
    private static final Duration OTP_TTL = Duration.ofMinutes(10);


    private final RedisTemplate<String, Object> redisTemplate;

    private final StringRedisTemplate stringRedisTemplate;

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
            stringRedisTemplate.opsForZSet().add(key, member.toString(), expire);
        } catch (Exception e) {
            log.error("zAdd error : {}", e.getMessage());
        }
    }

    @Override
    public Set<String> zRangeByScore(String key, long start, long end, int limit) {
        try {
            Set<String> result = stringRedisTemplate.opsForZSet().rangeByScore(key, start, end, 0, limit);

            return result != null ? result : new HashSet<>();
        } catch (Exception e) {
            log.error("zRangeByScore error : {}", e.getMessage());
        }

        return Set.of();
    }

    @Override
    public boolean zRemove(String key, String rawMember) {
        try {
            log.debug("ZREM {} member={}", key, rawMember);
            Long removed = stringRedisTemplate.opsForZSet().remove(key, rawMember);
            return removed != null && removed > 0;
        } catch (Exception e) {
            log.error("Failed to remove key : {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void setToken(String key, String token, Duration ttl) {
        try {
            log.info("setToken token:{}", token);
            redisTemplate.opsForValue().set(key, token, ttl);
        } catch (Exception e) {
            log.error("Failed to set token : {}", e.getMessage());
        }
    }

    @Override
    public Object getToken(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void deleteToken(String key) {
        try {
            log.info("delete token:{}", key);
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Failed to delete token : {}", e.getMessage());
        }
    }

    @Override
    public void addBlacklistToken(String key, String token, Duration remainingTimeMs) {
        try {
            redisTemplate.opsForValue().set(key, token, remainingTimeMs);
        } catch (Exception e) {
            log.error("Failed to set blacklist token: {}", e.getMessage());
        }
    }

    @Override
    public boolean isBlacklisted(String key) {
        return key != null && redisTemplate.hasKey(key);
    }

    @Override
    public void setOTP(String key, String otp) {
        try {
            log.info("setOTP OTP:{}", otp);
            redisTemplate.opsForValue().set(key, otp, OTP_TTL);
        } catch (Exception e) {
            log.error("Failed to set otp : {}", e.getMessage());
        }
    }

    @Override
    public String getString(String key) {
        try {
            Object result = redisTemplate.opsForValue().get(key);
            log.info("getString result:{}", result);

            if (result == null) {
                return null;
            }

            return objectMapper.convertValue(result, String.class);
        } catch (Exception e) {
            log.error("Failed to get cached string : {}", e.getMessage());
            deleteObject(key);
        }

        return null;
    }
}
