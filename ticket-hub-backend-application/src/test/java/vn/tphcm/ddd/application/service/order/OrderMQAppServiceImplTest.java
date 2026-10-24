package vn.tphcm.ddd.application.service.order;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import vn.tphcm.ddd.application.service.order.cache.StockOrderCacheService;
import vn.tphcm.ddd.application.service.order.impl.OrderMQAppServiceImpl;
import vn.tphcm.ddd.domain.model.OrderQueue;
import vn.tphcm.ddd.domain.model.OutboxEvent;
import vn.tphcm.ddd.domain.model.User;
import vn.tphcm.ddd.domain.repository.OrderQueueRepository;
import vn.tphcm.ddd.domain.repository.OutboxEventRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderMQAppServiceImplTest {
    @Mock StockOrderCacheService stockOrderCacheService;
    @Mock OutboxEventRepository outboxEventRepository;
    @Mock OrderQueueRepository orderQueueRepository;
    @Mock TransactionTemplate transactionTemplate;

    @InjectMocks OrderMQAppServiceImpl service;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId("user-1");
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

        when(transactionTemplate.execute(any())).thenAnswer(inv ->
                ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void placeOrderMQ_success_savesQueueRowAndOutboxEvent() {
        when(stockOrderCacheService.decreaseStockCache("t1", 2)).thenReturn(1);
        when(stockOrderCacheService.getEffectivePrice("t1")).thenReturn(5000L);

        OrderQueue queue = service.placeOrderMQ("t1", 2);

        assertEquals(0, queue.getStatus());
        assertTrue(queue.getToken().startsWith("MQ-"));
        assertEquals("user-1", queue.getUserId());
        verify(orderQueueRepository).save(any(OrderQueue.class));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertEquals(queue.getToken(), captor.getValue().getAggregateId());
        assertEquals(0, captor.getValue().getStatus());
    }

    @Test
    void placeOrderMQ_outboxEventType_matchesTheHandlerThatPublishesIt() {
        // PlaceOrderOutboxHandler chỉ xử lý "PLACE_ORDER"; nếu sự kiện được lưu với
        // loại khác thì OutboxPublisherJob sẽ không bao giờ gửi nó lên Kafka.
        when(stockOrderCacheService.decreaseStockCache("t1", 1)).thenReturn(1);
        when(stockOrderCacheService.getEffectivePrice("t1")).thenReturn(5000L);

        service.placeOrderMQ("t1", 1);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertEquals("PLACE_ORDER", captor.getValue().getEventType());
    }

    @Test
    void placeOrderMQ_cacheMiss_warmsUpThenSucceeds() {
        when(stockOrderCacheService.decreaseStockCache("t1", 1)).thenReturn(-1, 1);
        when(stockOrderCacheService.addStockAvailableToCache("t1")).thenReturn(true);
        when(stockOrderCacheService.getEffectivePrice("t1")).thenReturn(5000L);

        assertEquals(0, service.placeOrderMQ("t1", 1).getStatus());
    }

    @Test
    void placeOrderMQ_unknownTicket_failsWithNotFound() {
        when(stockOrderCacheService.decreaseStockCache("t1", 1)).thenReturn(-1);
        when(stockOrderCacheService.addStockAvailableToCache("t1")).thenReturn(false);

        OrderQueue queue = service.placeOrderMQ("t1", 1);

        assertEquals(2, queue.getStatus());
        assertTrue(queue.getMessage().startsWith("TICKET_NOT_FOUND"));
        verifyNoInteractions(orderQueueRepository, outboxEventRepository);
    }

    @Test
    void placeOrderMQ_soldOut_failsWithOutOfStock() {
        when(stockOrderCacheService.decreaseStockCache("t1", 1)).thenReturn(0);

        OrderQueue queue = service.placeOrderMQ("t1", 1);

        assertEquals(2, queue.getStatus());
        assertTrue(queue.getMessage().startsWith("OUT_OF_STOCK"));
    }

    @Test
    void placeOrderMQ_priceUnknown_rollsBackRedis() {
        when(stockOrderCacheService.decreaseStockCache("t1", 3)).thenReturn(1);
        when(stockOrderCacheService.getEffectivePrice("t1")).thenReturn(0L);

        OrderQueue queue = service.placeOrderMQ("t1", 3);

        assertEquals(2, queue.getStatus());
        assertTrue(queue.getMessage().startsWith("PRICE_NOT_FOUND"));
        verify(stockOrderCacheService).increaseStockCacheByLUA("t1", 3);
    }

    @Test
    void placeOrderMQ_transactionFails_compensatesRedis() {
        when(stockOrderCacheService.decreaseStockCache("t1", 2)).thenReturn(1);
        when(stockOrderCacheService.getEffectivePrice("t1")).thenReturn(5000L);
        doThrow(new IllegalStateException("db")).when(transactionTemplate).execute(any());

        OrderQueue queue = service.placeOrderMQ("t1", 2);

        assertEquals(2, queue.getStatus());
        assertTrue(queue.getMessage().startsWith("INTERNAL_ERROR"));
        verify(stockOrderCacheService).increaseStockCacheByLUA("t1", 2);
    }

    @Test
    void getOrderStatus_found() {
        OrderQueue q = OrderQueue.builder().token("MQ-1").status(1).build();
        when(orderQueueRepository.findByToken("MQ-1")).thenReturn(Optional.of(q));

        assertSame(q, service.getOrderStatus("MQ-1"));
    }

    @Test
    void getOrderStatus_notFound_returnsNull() {
        when(orderQueueRepository.findByToken("nope")).thenReturn(Optional.empty());

        assertNull(service.getOrderStatus("nope"));
    }
}
