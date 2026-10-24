package vn.tphcm.ddd.application.service.ticket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import vn.tphcm.ddd.application.dto.response.TicketDetailResponse;
import vn.tphcm.ddd.application.mapper.TicketDetailMapper;
import vn.tphcm.ddd.application.service.ticket.cache.TicketDetailCacheService;
import vn.tphcm.ddd.domain.model.TicketDetail;
import vn.tphcm.ddd.domain.service.TicketDetailDomainService;
import vn.tphcm.ddd.infrastructure.cache.redis.RedisInfrastructureService;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RedisDistributedLocker;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RedisDistributedService;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Cache cục bộ của service là biến static dùng chung giữa các test,
 * nên mỗi test dùng một ticketId ngẫu nhiên để không ảnh hưởng nhau.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TicketDetailCacheServiceTest {
    @Mock RedisDistributedService redisDistributedService;
    @Mock RedisInfrastructureService redisInfrastructureService;
    @Mock TicketDetailDomainService ticketDetailDomainService;
    @Mock TicketDetailMapper ticketDetailMapper;
    @Mock RedisDistributedLocker locker;

    @InjectMocks TicketDetailCacheService service;

    private String id;

    @BeforeEach
    void setUp() throws Exception {
        id = "ticket-" + UUID.randomUUID();
        when(redisDistributedService.getRedisDistributedLock(anyString())).thenReturn(locker);
        when(locker.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
    }

    private TicketDetailResponse response(long version) {
        TicketDetailResponse r = new TicketDetailResponse();
        r.setId(id);
        r.setVersion(version);
        return r;
    }

    private String redisKey() {
        return "PRO_TICKET:ITEM:" + id;
    }

    @Test
    void redisHit_returnsValueAndServesNextCallFromLocalCache() {
        TicketDetailResponse cached = response(100L);
        when(redisInfrastructureService.getObject(redisKey(), TicketDetailResponse.class)).thenReturn(cached);

        assertSame(cached, service.getTicketDetail(id, null));
        assertSame(cached, service.getTicketDetail(id, null));

        verify(redisInfrastructureService, times(1)).getObject(redisKey(), TicketDetailResponse.class);
        verifyNoInteractions(ticketDetailDomainService);
    }

    @Test
    void cacheMiss_loadsFromDatabaseStampsVersionAndStoresInRedis() {
        TicketDetail entity = new TicketDetail();
        TicketDetailResponse dto = response(0L);
        when(redisInfrastructureService.getObject(redisKey(), TicketDetailResponse.class)).thenReturn(null);
        when(ticketDetailDomainService.getTicketDefaultCacheVip(id)).thenReturn(entity);
        when(ticketDetailMapper.toDto(entity)).thenReturn(dto);

        TicketDetailResponse result = service.getTicketDetail(id, null);

        assertSame(dto, result);
        assertTrue(result.getVersion() > 0);
        verify(redisInfrastructureService).setObject(redisKey(), dto);
        verify(locker).unlock();
    }

    @Test
    void ticketNotInDatabase_cachesNullAndReturnsNull() {
        when(redisInfrastructureService.getObject(redisKey(), TicketDetailResponse.class)).thenReturn(null);
        when(ticketDetailDomainService.getTicketDefaultCacheVip(id)).thenReturn(null);

        assertNull(service.getTicketDetail(id, null));
        verify(redisInfrastructureService).setObject(redisKey(), null);
    }

    @Test
    void lockNotAcquired_returnsNullWithoutQueryingDatabase() throws Exception {
        when(redisInfrastructureService.getObject(redisKey(), TicketDetailResponse.class)).thenReturn(null);
        when(locker.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        assertNull(service.getTicketDetail(id, null));
        verifyNoInteractions(ticketDetailDomainService);
    }

    @Test
    void databaseError_isRethrownAndLockReleased() {
        when(redisInfrastructureService.getObject(redisKey(), TicketDetailResponse.class)).thenReturn(null);
        when(ticketDetailDomainService.getTicketDefaultCacheVip(id)).thenThrow(new IllegalStateException("db"));

        assertThrows(RuntimeException.class, () -> service.getTicketDetail(id, null));
        verify(locker).unlock();
    }

    @Test
    void clientVersionNotNewerThanLocal_isServedFromLocalCache() {
        TicketDetailResponse cached = response(100L);
        when(redisInfrastructureService.getObject(redisKey(), TicketDetailResponse.class)).thenReturn(cached);
        service.getTicketDetail(id, null); // nạp local cache

        assertSame(cached, service.getTicketDetail(id, 100L));
        assertSame(cached, service.getTicketDetail(id, 50L));
        verifyNoInteractions(ticketDetailDomainService);
    }

    @Test
    void clientVersionNewerThanLocal_reloadsFromRedisOrDatabase() {
        TicketDetailResponse stale = response(100L);
        TicketDetailResponse fresh = response(200L);
        when(redisInfrastructureService.getObject(redisKey(), TicketDetailResponse.class)).thenReturn(stale, fresh);
        service.getTicketDetail(id, null); // nạp local cache = stale

        assertSame(fresh, service.getTicketDetail(id, 150L));
    }

    @Test
    void orderTicketByUser_invalidatesLocalAndRedisCache() {
        TicketDetailResponse cached = response(100L);
        when(redisInfrastructureService.getObject(redisKey(), TicketDetailResponse.class)).thenReturn(cached);
        service.getTicketDetail(id, null);

        assertTrue(service.orderTicketByUser(id));

        verify(redisInfrastructureService).deleteObject(redisKey());

        // local cache đã bị xoá nên lần sau phải hỏi lại Redis
        service.getTicketDetail(id, null);
        verify(redisInfrastructureService, times(2)).getObject(redisKey(), TicketDetailResponse.class);
    }
}
