/*
 * @ (#) RateLimiterServiceImpl.java       1.0     10/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.distributed.redisson.impl;
/*
 * @author: Luong Tan Dat
 * @date: 10/13/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RateLimiterService;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "RATE-LIMITER")
public class RateLimiterServiceImpl implements RateLimiterService {
    private final StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<List> SCRIPT = new DefaultRedisScript<>(
            """
                      local c = redis.call('INCR', KEYS[1])
                      local ttl = redis.call('PTTL', KEYS[1])
                      if c == 1 or ttl < 0 then
                          redis.call('PEXPIRE', KEYS[1], ARGV[1])
                          ttl = tonumber(ARGV[1])
                      end
                      return {c,ttl}
                    """, List.class);

    @Override
    public Result tryAcquire(String key, int limit, Duration window) {
        try {
            List<?> rs = stringRedisTemplate.execute(SCRIPT, List.of("RATELIMIT:" + key), String.valueOf(window.toMillis()));

            long count = ((Number) rs.get(0)).longValue();
            long ttlMs = ((Number) rs.get(1)).longValue();

            boolean allowed = count <= limit;

            return new Result(allowed, Math.max(0, limit - count), (long) Math.ceil(ttlMs / 1000.0));
        } catch (Exception e) {
            log.error("Rate limiter unavailable, allow request. Key={}", key, e);
            return new Result(true, limit, 0);
        }
    }
}
