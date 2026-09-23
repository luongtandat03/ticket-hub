/*
 * @ (#) TicketDetailCacheService.java       1.0     8/5/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.ticket.cache;
/*
 * @author: Luong Tan Dat
 * @date: 8/5/2026
 */

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.tphcm.ddd.application.dto.TicketDetailResponse;
import vn.tphcm.ddd.application.mapper.TicketDetailMapper;
import vn.tphcm.ddd.domain.model.TicketDetail;
import vn.tphcm.ddd.domain.service.TicketDetailDomainService;
import vn.tphcm.ddd.infrastructure.cache.redis.RedisInfrastructureService;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RedisDistributedLocker;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RedisDistributedService;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j(topic = "TICKET-DETAIL-CACHE-SERVICE")
@RequiredArgsConstructor
public class TicketDetailCacheService {
    private final RedisDistributedService redisDistributedService;
    private final RedisInfrastructureService redisInfrastructureService;
    private final TicketDetailDomainService ticketDetailDomainService;
    private final TicketDetailMapper ticketDetailMapper;

    private static final Duration LOCAL_CACHE_TTL = Duration.ofMinutes(10);
    private static final int ITEM_LOCAL_CACHE = 10;
    private static final int THREAD_LOCAL_CACHE = 4;

    private static final Cache<String, TicketDetailResponse> ticketDetailLocalCache = CacheBuilder.newBuilder()
            .initialCapacity(ITEM_LOCAL_CACHE)
            .concurrencyLevel(THREAD_LOCAL_CACHE)
            .expireAfterAccess(LOCAL_CACHE_TTL)
            .build();

    /**
     * Get ticket detail
     */
    public TicketDetailResponse getTicketDetail(String id, Long version) {
        TicketDetailResponse ticketDetailResponse = getTicketDetailLocalCache(id);

        if (ticketDetailResponse != null) {
            log.info("TicketDetailResponse found for id {} and version {}", id, version);

            if (version == null) {
                log.info("01.Get Ticket from local cache for versionUser {} and versionLocal {} ", version, ticketDetailResponse.getVersion());
                return ticketDetailResponse;
            }

            if (version.equals(ticketDetailResponse.getVersion()) || version < ticketDetailResponse.getVersion()) {
                log.info("02.Get Ticket from local cache for versionUser {} and versionLocal {} ", version, ticketDetailResponse.getVersion());
                return ticketDetailResponse;
            }

            log.info("03. Get Ticket from database for versionUser {} and versionLocal {}", version, ticketDetailResponse.getVersion());
            return getTicketDetailDatabase(id);
        }

        return getTicketDetailCache(id);
    }

    /**
     * Get Ticket Detail in Local Cache
     */
    private TicketDetailResponse getTicketDetailCache(String id) {
        TicketDetailResponse ticketDetailResponse = redisInfrastructureService.getObject(genEventItemKey(id), TicketDetailResponse.class);

        if (ticketDetailResponse != null) {
            log.info("Get ticket from cache");
            ticketDetailLocalCache.put(id, ticketDetailResponse);
            return ticketDetailResponse;
        }

        log.info("Get Ticket from distributed locker");
        ticketDetailResponse = getTicketDetailDatabase(id);

        return ticketDetailResponse;
    }

    /**
     * Get Ticket Detail in Database
     */
    private TicketDetailResponse getTicketDetailDatabase(String id) {
        RedisDistributedLocker locker = redisDistributedService.getRedisDistributedLock(genEventItemKeyLock(id));

        try {
            boolean isLock = locker.tryLock(1, 5, TimeUnit.SECONDS);

            if (!isLock) {
                return null;
            }

            TicketDetailResponse ticketDetailResponse = redisInfrastructureService.getObject(genEventItemKey(id), TicketDetailResponse.class);

            if (ticketDetailResponse != null) {
                return ticketDetailResponse;
            }

            TicketDetail ticketDetail = ticketDetailDomainService.getTicketDefaultCacheVip(id);
            if (ticketDetail == null) {
                log.info("Set Null to Redis");
                redisInfrastructureService.setObject(genEventItemKey(id), null);
                return null;
            }

            ticketDetailResponse = ticketDetailMapper.toDto(ticketDetail);
            ticketDetailResponse.setVersion(System.currentTimeMillis());

            log.info("Set Ticket Detail to Redis");
            redisInfrastructureService.setObject(genEventItemKey(id), ticketDetailResponse);

            return ticketDetailResponse;

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            locker.unlock();
        }
    }

    public boolean orderTicketByUser(String ticketId) {
        ticketDetailLocalCache.invalidate(ticketId); // remove local cache
        redisInfrastructureService.deleteObject(genEventItemKey(ticketId));
        return true;
    }


    private TicketDetailResponse getTicketDetailLocalCache(String id) {
        log.info("Implement get Ticket in Redis for id {}", id);
        try {
            return ticketDetailLocalCache.getIfPresent(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String genEventItemKey(String itemId) {
        return "PRO_TICKET:ITEM:" + itemId;
    }

    private String genEventItemKeyLock(String ticketId) {
        return "PRO_LOCK_KEY_ITEM:" + ticketId;
    }

}

