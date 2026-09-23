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
import vn.tphcm.ddd.application.dto.TicketResponse;
import vn.tphcm.ddd.application.mapper.TicketMapper;
import vn.tphcm.ddd.domain.service.TicketDomainService;
import vn.tphcm.ddd.infrastructure.cache.redis.RedisInfrastructureService;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RedisDistributedService;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

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

    private static final Cache<String, TicketResponse> ticketDetailLocalCache = CacheBuilder.newBuilder()
            .initialCapacity(ITEM_LOCAL_CACHE)
            .concurrencyLevel(THREAD_LOCAL_CACHE)
            .expireAfterAccess(LOCAL_CACHE_TTL)
            .build();


    public List<TicketResponse> getTicketsByStatus(Integer status, String version) {
        List<TicketResponse> ticketResponse = getTicketLocalCache(status);

        if (ticketResponse.isEmpty()) {
//            log.info("")
        }
        return null;
    }

    private List<TicketResponse> getTicketLocalCache(Integer status) {
        log.info("Implement get Ticket in Redis for status {}", status);
        try {
            return Collections.singletonList(ticketDetailLocalCache.getIfPresent(status));
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

}
