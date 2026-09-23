/*
 * @ (#) RedisDistributedLocker.java       1.0     8/5/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.distributed.redisson;

import java.util.concurrent.TimeUnit;

/*
 * @author: Luong Tan Dat
 * @date: 8/5/2026
 */
public interface RedisDistributedLocker {
    boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException;

    void lock(long leaseTime, TimeUnit unit);

    void unlock();

    boolean isLocked();

    boolean isHeldByThread(long threadId);

    boolean isHeldByCurrentThread();
}
