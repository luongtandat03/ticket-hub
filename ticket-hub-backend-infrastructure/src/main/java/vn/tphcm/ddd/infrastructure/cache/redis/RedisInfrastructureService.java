/*
 * @ (#) RedisInsfrastructure.java       1.0     8/5/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.cache.redis;

import org.springframework.data.redis.core.RedisTemplate;

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

    void zRemove(String key, String rawMember);
}
