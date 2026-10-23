package vn.tphcm.ddd.application.service.ticket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.tphcm.ddd.application.dto.response.TicketListResponse;
import vn.tphcm.ddd.application.dto.response.TicketResponse;
import vn.tphcm.ddd.application.mapper.TicketMapper;
import vn.tphcm.ddd.application.service.ticket.cache.TicketAppCacheService;
import vn.tphcm.ddd.domain.model.Ticket;
import vn.tphcm.ddd.domain.service.TicketDomainService;
import vn.tphcm.ddd.infrastructure.cache.redis.RedisInfrastructureService;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RedisDistributedLocker;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RedisDistributedService;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Cache cục bộ là static nên mỗi test dùng một status riêng để không ảnh hưởng nhau.
 */
class TicketAppCacheServiceTest {
    private final RedisInfrastructureService redis = mock(RedisInfrastructureService.class);
    private final RedisDistributedService lockService = mock(RedisDistributedService.class);
    private final RedisDistributedLocker locker = mock(RedisDistributedLocker.class);
    private final TicketDomainService domain = mock(TicketDomainService.class);
    private final TicketMapper mapper = mock(TicketMapper.class);
    private final TicketAppCacheService service = new TicketAppCacheService(redis, lockService, domain, mapper);

    @BeforeEach
    void setUp() throws Exception {
        when(lockService.getRedisDistributedLock(anyString())).thenReturn(locker);
        when(locker.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
    }

    private String key(int status) {
        return "PRO_TICKET:LIST:STATUS:" + status;
    }

    @Test
    void nullStatus_returnsEmptyList() {
        assertTrue(service.getTicketsByStatus(null, null).isEmpty());
        verifyNoInteractions(redis, domain);
    }

    @Test
    void cacheMiss_loadsFromDatabaseAndStoresInRedis() {
        Ticket ticket = new Ticket();
        TicketResponse response = new TicketResponse();
        when(domain.findByStatus(101)).thenReturn(List.of(ticket));
        when(mapper.toResponse(ticket)).thenReturn(response);

        List<TicketResponse> result = service.getTicketsByStatus(101, null);

        assertEquals(List.of(response), result);
        verify(redis).setObject(eq(key(101)), any(TicketListResponse.class));
        verify(locker).unlock();
    }

    @Test
    void secondCall_isServedFromLocalCache() {
        when(domain.findByStatus(102)).thenReturn(List.of());

        service.getTicketsByStatus(102, null);
        service.getTicketsByStatus(102, null);

        verify(domain, times(1)).findByStatus(102);
    }

    @Test
    void redisHit_doesNotQueryDatabase() {
        TicketListResponse cached = new TicketListResponse(List.of(new TicketResponse()), 100L);
        when(redis.getObject(key(103), TicketListResponse.class)).thenReturn(cached);

        assertEquals(1, service.getTicketsByStatus(103, null).size());
        verifyNoInteractions(domain);
    }

    @Test
    void emptyResult_isCachedToo() {
        when(domain.findByStatus(104)).thenReturn(null);

        assertTrue(service.getTicketsByStatus(104, null).isEmpty());
        verify(redis).setObject(eq(key(104)), any(TicketListResponse.class));
    }

    @Test
    void lockBusy_fallsBackToRedisWithoutQueryingDatabase() throws Exception {
        when(locker.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        assertTrue(service.getTicketsByStatus(105, null).isEmpty());
        verifyNoInteractions(domain);
    }

    @Test
    void clientVersionNewerThanCache_reloadsFromDatabase() {
        TicketListResponse stale = new TicketListResponse(List.of(), 100L);
        when(redis.getObject(key(106), TicketListResponse.class)).thenReturn(stale, stale, null);
        when(domain.findByStatus(106)).thenReturn(List.of());

        service.getTicketsByStatus(106, null);          // nạp local cache với version 100
        service.getTicketsByStatus(106, 200L);         // client mới hơn -> bỏ qua cache

        verify(domain).findByStatus(106);
    }

    @Test
    void invalidVersion_isIgnored() {
        TicketListResponse cached = new TicketListResponse(List.of(new TicketResponse()), 100L);
        when(redis.getObject(key(107), TicketListResponse.class)).thenReturn(cached);

        assertEquals(1, service.getTicketsByStatus(107, 100L).size());
    }

    @Test
    void evict_clearsLocalAndRedis() {
        when(domain.findByStatus(108)).thenReturn(List.of());
        service.getTicketsByStatus(108, null);

        service.evictTicketsByStatus(108);
        service.getTicketsByStatus(108, null);

        verify(redis).deleteObject(key(108));
        verify(domain, times(2)).findByStatus(108);
    }
}