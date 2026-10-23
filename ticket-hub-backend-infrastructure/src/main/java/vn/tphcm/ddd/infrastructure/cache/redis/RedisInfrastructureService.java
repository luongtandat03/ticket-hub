/*
 * @ (#) RedisInsfrastructure.java       1.0     8/5/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.cache.redis;

import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Set;

/*
 * @author: Luong Tan Dat
 * @date: 8/5/2026
 */
public interface RedisInfrastructureService {
//    void setString(String key, String value);
//    String getString(String key);

    void setObject(String key, Object value);
    <T> T getObject(String key, Class<T> targetClass);

    void deleteObject(String key);

    RedisTemplate<String, Object> redisTemplate();

    void zAdd(String key, Object member, long expire);

    Set<String> zRangeByScore(String key, long start, long end, int limit);

    boolean zRemove(String key, String rawMember);

    void setToken(String key, String token, Duration ttl);

    Object getToken(String key);

    void deleteToken(String key);

    void addBlacklistToken(String key, String token, Duration remainingTimeMs);

    boolean isBlacklisted(String key);

    void setOTP(String key, String otp);

    String getString(String key);

}
