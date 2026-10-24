package vn.tphcm.ddd.application.service.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import vn.tphcm.ddd.domain.model.TicketDetail;
import vn.tphcm.ddd.domain.service.TicketDetailDomainService;
import org.springframework.data.redis.core.script.RedisScript;
import vn.tphcm.ddd.application.dto.response.TicketDetailResponse;
import vn.tphcm.ddd.application.service.order.cache.StockOrderCacheService;
import vn.tphcm.ddd.application.service.ticket.cache.TicketDetailCacheService;
import vn.tphcm.ddd.infrastructure.cache.redis.RedisInfrastructureService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings({"unchecked", "rawtypes"})
class StockOrderCacheServiceTest {
    @Mock TicketDetailCacheService ticketDetailCacheService;
    @Mock RedisInfrastructureService redisInfrastructureService;
    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock ValueOperations<String, Object> valueOps;
    @Mock TicketDetailDomainService ticketDetailDomainService;

    @InjectMocks StockOrderCacheService service;

    @BeforeEach
    void setUp() {
        when(redisInfrastructureService.redisTemplate()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    private TicketDetail ticket(int stock) {
        TicketDetail t = new TicketDetail();
        t.setStockAvailable(stock);
        return t;
    }

    private TicketDetailResponse detail(BigDecimal original, BigDecimal flash, int stock) {
        TicketDetailResponse d = new TicketDetailResponse();
        d.setPriceOriginal(original);
        d.setPriceFlash(flash);
        d.setStockAvailable(stock);
        return d;
    }

    // ---------------- getEffectivePrice ----------------

    @Test
    void effectivePrice_prefersFlashPriceWhenPositive() {
        when(ticketDetailCacheService.getTicketDetail("t1", null))
                .thenReturn(detail(new BigDecimal("100000"), new BigDecimal("80000"), 10));

        assertEquals(80000L, service.getEffectivePrice("t1"));
    }

    @Test
    void effectivePrice_fallsBackToOriginalWhenFlashIsZeroOrNull() {
        when(ticketDetailCacheService.getTicketDetail("t1", null))
                .thenReturn(detail(new BigDecimal("100000"), BigDecimal.ZERO, 10));
        assertEquals(100000L, service.getEffectivePrice("t1"));

        when(ticketDetailCacheService.getTicketDetail("t2", null))
                .thenReturn(detail(new BigDecimal("50000"), null, 10));
        assertEquals(50000L, service.getEffectivePrice("t2"));
    }

    @Test
    void effectivePrice_returnsMinusOneWhenNoPriceOrNoTicket() {
        when(ticketDetailCacheService.getTicketDetail("t1", null)).thenReturn(detail(null, null, 10));
        assertEquals(-1L, service.getEffectivePrice("t1"));

        when(ticketDetailCacheService.getTicketDetail("none", null)).thenReturn(null);
        assertEquals(-1L, service.getEffectivePrice("none"));
    }

    // ---------------- addStockAvailableToCache ----------------

    @Test
    void addStock_nullTicketId_returnsFalse() {
        assertFalse(service.addStockAvailableToCache(null));
        verifyNoInteractions(ticketDetailCacheService);
    }

    @Test
    void addStock_ticketMissing_returnsFalseAndWritesNothing() {
        when(ticketDetailDomainService.getTicketDefaultCacheVip("t1")).thenReturn(null);

        assertFalse(service.addStockAvailableToCache("t1"));
        verifyNoInteractions(valueOps);
    }

    @Test
    void addStock_readsDatabaseAndWritesOnlyIfKeyAbsent() {
        when(ticketDetailDomainService.getTicketDefaultCacheVip("t1")).thenReturn(ticket(77));

        assertTrue(service.addStockAvailableToCache("t1"));
        verify(valueOps).setIfAbsent("TICKET:t1:STOCK", 77);
        verify(valueOps, never()).set(anyString(), any());
        verify(redisInfrastructureService, never()).setObject(any(), any());
        verifyNoInteractions(ticketDetailCacheService); // không dùng cache chi tiết vé (có thể cũ)
    }

    // ---------------- Lua wrappers ----------------

    @Test
    void decreaseByLua_returnsScriptResult() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(1L);

        assertEquals(1, service.decreaseStockCache("t1", 2));
    }

    @Test
    void decreaseByLua_nullResultMeansCacheMiss() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(null);

        assertEquals(-1, service.decreaseStockCache("t1", 2));
    }

    @Test
    void increaseByLua_trueOnlyWhenScriptReturnsOne() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(1L);
        assertTrue(service.increaseStockCacheByLUA("t1", 2));

        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(0L);
        assertFalse(service.increaseStockCacheByLUA("t1", 2));

        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(null);
        assertFalse(service.increaseStockCacheByLUA("t1", 2));
    }

    @Test
    void decreaseByLuaUpperCase_returnsOldStockFromScript() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(9L);

        assertEquals(9, service.decreaseStockCacheByLUA("t1", 1));
    }
}
