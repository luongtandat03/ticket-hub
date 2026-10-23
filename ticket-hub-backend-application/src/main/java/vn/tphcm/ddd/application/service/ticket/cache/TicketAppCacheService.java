/*
 * @ (#) TicketAppService.java       1.0     9/15/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.ticket.cache;
/*
 * @author: Luong Tan Dat
 * @date: 9/15/2026
 */

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.tphcm.ddd.application.dto.response.TicketListResponse;
import vn.tphcm.ddd.application.dto.response.TicketResponse;
import vn.tphcm.ddd.application.mapper.TicketMapper;
import vn.tphcm.ddd.domain.model.Ticket;
import vn.tphcm.ddd.domain.service.TicketDomainService;
import vn.tphcm.ddd.infrastructure.cache.redis.RedisInfrastructureService;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RedisDistributedLocker;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RedisDistributedService;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "TICKET-APP-SERVICE")
public class TicketAppCacheService {
    private final RedisInfrastructureService redisInfrastructureService;
    private final RedisDistributedService redisDistributedService;
    private final TicketDomainService ticketDomainService;
    private final TicketMapper ticketMapper;

    private static final Duration LOCAL_CACHE_TTL = Duration.ofMinutes(10);
    private static final int ITEM_LOCAL_CACHE = 10;
    private static final int THREAD_LOCAL_CACHE = 4;

    private static final Cache<String, TicketListResponse> ticketLocalCache =
            CacheBuilder.newBuilder()
            .initialCapacity(ITEM_LOCAL_CACHE)
            .concurrencyLevel(THREAD_LOCAL_CACHE)
            .expireAfterAccess(LOCAL_CACHE_TTL)
            .build();


    public List<TicketResponse> getTicketsByStatus(Integer status, Long version) {
        if (status == null) {
            return Collections.emptyList();
        }

        String key = genTicketListKey(status);

        TicketListResponse ticketResponse = getTicketLocalCache(status);

        if (ticketResponse != null) {
            log.info("Ticket Detail found for status {} and version {}", status, version);

            if (version == null) {
                log.info("1. Get Ticket from local");
                return ticketResponse.getTicketList();
            }

            if (version.equals(ticketResponse.getVersion()) || version < ticketResponse.getVersion()) {
                log.info("02.Get Ticket from cache for versionUser {} and versionLocal {} ", version, ticketResponse.getVersion());
                return getTicketCache(status, key);
            }

            log.info("03. Get Ticket from database for versionUser {} and versionLocal {}", version, ticketResponse.getVersion());
            return getTicketDatabase(status, key, version);
        }
        return getTicketCache(status, key);
    }

    private TicketListResponse getTicketLocalCache(Integer status) {
        log.info("Implement get Ticket in Redis for status {}", status);
        try {
            return ticketLocalCache.getIfPresent(genTicketListKey(status));
        } catch (Exception e) {
            throw new RuntimeException("Local Cache not found for status: " + status, e);
        }
    }

    private List<TicketResponse> getTicketCache(Integer status, String key) {
        TicketListResponse ticketListResponse = redisInfrastructureService.getObject(genTicketListKey(status), TicketListResponse.class);

        if (ticketListResponse != null) {
            log.info("Ticket Detail found for status {}", status);
            ticketLocalCache.put(key, ticketListResponse);
            return ticketListResponse.getTicketList();
        }

        log.info("Get Ticket from database");
        return getTicketDatabase(status, key, null);
    }

    private List<TicketResponse> getTicketDatabase(Integer status, String key, Long version) {
        RedisDistributedLocker locker = redisDistributedService.getRedisDistributedLock(genTicketListLockKey(status));

        try {
            boolean isLock = locker.tryLock(1, 5, TimeUnit.SECONDS);
            if (!isLock) {
                TicketListResponse cached = redisInfrastructureService.getObject(key, TicketListResponse.class);
                return cached != null ? cached.getTicketList() : Collections.emptyList();
            }

            TicketListResponse response = redisInfrastructureService.getObject(key, TicketListResponse.class);

            if (response != null && (version == null || response.getVersion() >= version)) {
                ticketLocalCache.put(key, response);
                return response.getTicketList();
            }

            List<Ticket> tickets = ticketDomainService.findByStatus(status);

            List<TicketResponse> responses = tickets == null
                    ? Collections.emptyList()
                    : tickets.stream().map(ticketMapper::toResponse).toList();

            TicketListResponse fresh = new TicketListResponse(responses, System.currentTimeMillis());
            redisInfrastructureService.setObject(key, fresh);
            ticketLocalCache.put(key, fresh);

            return responses;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("Interrupted while waiting for lock", e);
        } finally {
            locker.unlock();
        }

    }

    public void evictTicketsByStatus(Integer status) {
        String key = genTicketListKey(status);
        ticketLocalCache.invalidate(key);
        redisInfrastructureService.deleteObject(key);
    }


    private String genTicketListKey(Integer status) {
        return "PRO_TICKET:LIST:STATUS:" + status;
    }

    private String genTicketListLockKey(Integer status) {
        return "PRO_LOCK_KEY_LIST:STATUS:" + status;
    }

}
