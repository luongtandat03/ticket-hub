/*
 * @ (#) RateLimiterService.java       1.0     10/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.distributed.redisson;

import java.time.Duration;

/*
 * @author: Luong Tan Dat
 * @date: 10/13/2026
 */
public interface RateLimiterService {
    Result tryAcquire(String key, int limit, Duration window);

    record Result(boolean isAllowed, long remaining, long retryAfterSeconds) {
    }
}
