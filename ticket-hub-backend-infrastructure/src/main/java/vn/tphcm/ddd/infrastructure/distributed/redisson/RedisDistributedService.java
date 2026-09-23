/*
 * @ (#) RedisDistributedService.java       1.0     8/5/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.distributed.redisson;

/*
 * @author: Luong Tan Dat
 * @date: 8/5/2026
 */

public interface RedisDistributedService {
    RedisDistributedLocker getRedisDistributedLock(String lockKey);
}
