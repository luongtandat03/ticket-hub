/*
 * @ (#) RedisDistributedLockerImpl.java       1.0     8/5/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.distributed.redisson.impl;
/*
 * @author: Luong Tan Dat
 * @date: 8/5/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RedisDistributedLocker;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RedisDistributedService;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j(topic = "REDIS-DISTRIBUTED-LOCKER")
@RequiredArgsConstructor
public class RedisDistributedLockerImpl implements RedisDistributedService {
    private final RedissonClient redissonClient;
    @Override
    public RedisDistributedLocker getRedisDistributedLock(String lockKey) {
        RLock rLock = redissonClient.getLock(lockKey);

        return new RedisDistributedLocker() {

            @Override
            public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
                boolean isLockSuccess = rLock.tryLock(waitTime, leaseTime, unit);
                log.info("{} get lock result:{}", lockKey, isLockSuccess);
                return isLockSuccess;
            }

            @Override
            public void lock(long leaseTime, TimeUnit unit) {
                rLock.lock(leaseTime, unit);
            }

            @Override
            public void unlock() {
                if (isLocked() && isHeldByCurrentThread()) {
                    rLock.unlock();
                }
            }

            @Override
            public boolean isLocked() {
                return rLock.isLocked();
            }

            @Override
            public boolean isHeldByThread(long threadId) {
                return rLock.isHeldByThread(threadId);
            }

            @Override
            public boolean isHeldByCurrentThread() {
                return rLock.isHeldByCurrentThread();
            }
        };

    }
}
