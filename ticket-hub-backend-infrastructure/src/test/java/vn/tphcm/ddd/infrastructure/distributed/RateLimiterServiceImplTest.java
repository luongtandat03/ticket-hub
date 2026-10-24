package vn.tphcm.ddd.infrastructure.distributed;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RateLimiterService;
import vn.tphcm.ddd.infrastructure.distributed.redisson.impl.RateLimiterServiceImpl;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
class RateLimiterServiceImplTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final RateLimiterServiceImpl service = new RateLimiterServiceImpl(redis);

    private void redisReturns(long count, long ttlMs) {
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(count, ttlMs));
    }

    @Test
    void firstCall_isAllowedWithRemainingQuota() {
        redisReturns(1, 60_000);

        RateLimiterService.Result r = service.tryAcquire("k", 5, Duration.ofMinutes(1));

        assertTrue(r.isAllowed());
        assertEquals(4, r.remaining());
        assertEquals(60, r.retryAfterSeconds());
    }

    @Test
    void callAtTheLimit_isStillAllowedWithZeroRemaining() {
        redisReturns(5, 30_000);

        RateLimiterService.Result r = service.tryAcquire("k", 5, Duration.ofMinutes(1));

        assertTrue(r.isAllowed());
        assertEquals(0, r.remaining());
    }

    @Test
    void callOverTheLimit_isRejectedAndReportsRetryAfterRoundedUp() {
        redisReturns(6, 1_200);

        RateLimiterService.Result r = service.tryAcquire("k", 5, Duration.ofMinutes(1));

        assertFalse(r.isAllowed());
        assertEquals(0, r.remaining());
        assertEquals(2, r.retryAfterSeconds());
    }

    @Test
    void usesNamespacedKeyAndWindowInMillis() {
        redisReturns(1, 1000);

        service.tryAcquire("login:alice", 5, Duration.ofSeconds(90));

        verify(redis).execute(any(RedisScript.class), eq(List.of("RATELIMIT:login:alice")), eq("90000"));
    }

    @Test
    void redisFailure_failsOpen() {
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new IllegalStateException("redis down"));

        RateLimiterService.Result r = service.tryAcquire("k", 5, Duration.ofMinutes(1));

        assertTrue(r.isAllowed());
        assertEquals(5, r.remaining());
    }
}
