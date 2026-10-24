package vn.tphcm.ddd.application.service.order.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import vn.tphcm.ddd.application.schedule.OrderCancelSchedule;
import vn.tphcm.ddd.application.service.order.cache.StockOrderCacheService;
import vn.tphcm.ddd.domain.model.TicketOrder;
import vn.tphcm.ddd.domain.repository.IdempotencyKeyRepository;
import vn.tphcm.ddd.domain.repository.OrderQueueRepository;
import vn.tphcm.ddd.domain.service.OrderDeductionDomainService;
import vn.tphcm.ddd.domain.service.TicketOrderDomainService;
import vn.tphcm.ddd.infrastructure.mq.order.PlaceOrderMQMessage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KafkaOrderConsumerTest {
    @Mock IdempotencyKeyRepository idempotencyKeyRepository;
    @Mock TicketOrderDomainService ticketOrderDomainService;
    @Mock StockOrderCacheService stockOrderCacheService;
    @Mock OrderQueueRepository orderQueueRepository;
    @Mock OrderDeductionDomainService orderDeductionDomainService;
    @Mock OrderCancelSchedule orderCancelSchedule;
    @Mock TransactionTemplate transactionTemplate;
    @Mock TransactionStatus txStatus;

    KafkaOrderConsumer consumer;

    private static final String T1 = "ticket-1";
    private static final String T2 = "ticket-2";
    private static final String UUID_USER = "0b4f2a8e-1c2d-4e5f-8a9b-0c1d2e3f4a5b"; // userId thật là UUID

    @BeforeEach
    void setUp() {
        consumer = new KafkaOrderConsumer(idempotencyKeyRepository, ticketOrderDomainService, stockOrderCacheService,
                orderQueueRepository, orderDeductionDomainService, orderCancelSchedule, transactionTemplate);

        // Chạy callback như Spring thật; txStatus ghi nhận rollback
        when(transactionTemplate.execute(any()))
                .thenAnswer(inv -> inv.<TransactionCallback<?>>getArgument(0).doInTransaction(txStatus));
        when(idempotencyKeyRepository.tryInsert(anyString(), any())).thenReturn(true);
        when(ticketOrderDomainService.decreaseStock(anyString(), anyInt())).thenReturn(true);
    }

    private static PlaceOrderMQMessage msg(String token, String ticketId, int quantity, String userId) {
        return new PlaceOrderMQMessage(token, ticketId, quantity, userId, 5000L, System.currentTimeMillis());
    }

    @Test
    void batch_stockUpdateIsLastStatementThenScheduleAfterCommit() {
        consumer.processOrders(List.of(msg("tk1", T1, 2, "user-1")));

        InOrder order = inOrder(orderDeductionDomainService, idempotencyKeyRepository, orderQueueRepository,
                ticketOrderDomainService, orderCancelSchedule);
        order.verify(orderDeductionDomainService).insertOrder(anyString(), any());
        order.verify(idempotencyKeyRepository).tryInsert(eq("tk1"), any());
        order.verify(orderQueueRepository).updateStatus(eq("tk1"), eq(1), startsWith("OKS-SGN-"), isNull());
        order.verify(ticketOrderDomainService).decreaseStock(T1, 2);
        order.verify(orderCancelSchedule).scheduleTimeout(startsWith("OKS-SGN-"), anyString(), eq(T1), eq(2));
        verify(txStatus, never()).setRollbackOnly();
        verify(stockOrderCacheService, never()).increaseStockCacheByLUA(any(), anyInt());
    }

    @Test
    void orderTable_isTheMonthOfTheOrderNumber() {
        // Huỷ đơn / thanh toán tìm bảng tbl_ticket_order_yyyyMM bằng tháng suy ra từ mã đơn,
        // nên đơn phải được ghi đúng vào bảng đó (sai "yyyyHH" sẽ ghi vào bảng theo giờ)
        consumer.processOrders(List.of(msg("tk1", T1, 1, UUID_USER)));

        ArgumentCaptor<String> table = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<TicketOrder> order = ArgumentCaptor.forClass(TicketOrder.class);
        verify(orderDeductionDomainService).insertOrder(table.capture(), order.capture());

        long millis = Long.parseLong(order.getValue().getOrderNumber().split("-")[2]);
        String monthOfOrderNumber = DateTimeFormatter.ofPattern("yyyyMM")
                .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()));
        assertEquals(monthOfOrderNumber, table.getValue());
        verify(orderCancelSchedule).scheduleTimeout(anyString(), eq(monthOfOrderNumber), eq(T1), eq(1));
    }

    @Test
    void batch_updatesStockOncePerTicketWithTotalQuantity() {
        consumer.processOrders(List.of(
                msg("a", T1, 1, "user-1"),
                msg("b", T1, 2, "user-2"),
                msg("c", T2, 1, "user-3"),
                msg("d", T1, 1, "user-4")));

        verify(ticketOrderDomainService, times(1)).decreaseStock(T1, 4);
        verify(ticketOrderDomainService, times(1)).decreaseStock(T2, 1);
        verify(ticketOrderDomainService, times(2)).decreaseStock(anyString(), anyInt());
        verify(orderDeductionDomainService, times(4)).insertOrder(anyString(), any());
        verify(orderCancelSchedule, times(4)).scheduleTimeout(anyString(), anyString(), anyString(), anyInt());
        verify(txStatus, never()).setRollbackOnly();
    }

    @Test
    void batch_sameUserTwice_getsUniqueOrderNumbers() {
        consumer.processOrders(List.of(msg("a", T1, 1, UUID_USER), msg("b", T1, 1, UUID_USER), msg("c", T1, 1, UUID_USER)));

        ArgumentCaptor<TicketOrder> orders = ArgumentCaptor.forClass(TicketOrder.class);
        verify(orderDeductionDomainService, times(3)).insertOrder(anyString(), orders.capture());
        List<String> numbers = orders.getAllValues().stream().map(TicketOrder::getOrderNumber).toList();
        assertEquals(3, new HashSet<>(numbers).size(), "order_number là UNIQUE: " + numbers);
        numbers.forEach(n -> assertEquals(54, n.length())); // vừa cột VARCHAR(54)
    }

    @Test
    void batch_notEnoughDbStock_fallsBackToOneByOne() {
        when(ticketOrderDomainService.decreaseStock(T1, 4)).thenReturn(false); // cả lô không đủ
        when(ticketOrderDomainService.decreaseStock(T1, 2)).thenReturn(false); // đơn "b" hết vé

        consumer.processOrders(List.of(msg("a", T1, 1, "user-1"), msg("b", T1, 2, "user-2"), msg("c", T1, 1, "user-3")));

        // Mock không rollback thật: mỗi đơn ghi status 1 một lần trong lô (bị rollback) và một lần khi xử lý lại
        verify(orderQueueRepository, times(2)).updateStatus(eq("a"), eq(1), anyString(), isNull());
        verify(orderQueueRepository, times(2)).updateStatus(eq("c"), eq(1), anyString(), isNull());
        verify(orderQueueRepository).updateStatus("b", 2, null, "Sold out");
        verify(txStatus, times(2)).setRollbackOnly(); // cả lô + đơn "b"
        verify(stockOrderCacheService, times(1)).increaseStockCacheByLUA(T1, 2);
        verify(orderCancelSchedule, times(2)).scheduleTimeout(anyString(), anyString(), eq(T1), eq(1));
    }

    @Test
    void batch_withDuplicate_fallsBackAndSkipsOnlyTheDuplicate() {
        when(idempotencyKeyRepository.tryInsert(eq("b"), any())).thenReturn(false);

        consumer.processOrders(List.of(msg("a", T1, 1, "user-1"), msg("b", T1, 1, "user-2")));

        verify(ticketOrderDomainService, never()).decreaseStock(T1, 2); // lô bị huỷ trước khi trừ kho
        verify(ticketOrderDomainService, times(1)).decreaseStock(T1, 1); // chỉ "a"
        verify(orderQueueRepository, times(2)).updateStatus(eq("a"), eq(1), anyString(), isNull()); // lô + xử lý lại
        verify(orderQueueRepository, never()).updateStatus(eq("b"), anyInt(), any(), any());
        verify(orderCancelSchedule, times(1)).scheduleTimeout(anyString(), anyString(), eq(T1), eq(1));
    }

    @Test
    void singleOrderSoldOutInDb_marksSoldOutAndRestoresRedisOnce() {
        when(ticketOrderDomainService.decreaseStock(T1, 3)).thenReturn(false);

        consumer.processOrders(List.of(msg("tk1", T1, 3, "user-1")));

        verify(txStatus, times(2)).setRollbackOnly(); // lô + xử lý lại đơn đó
        verify(orderQueueRepository).updateStatus("tk1", 2, null, "Sold out");
        verify(stockOrderCacheService, times(1)).increaseStockCacheByLUA(T1, 3);
        verify(orderCancelSchedule, never()).scheduleTimeout(any(), any(), any(), anyInt());
    }

    @Test
    void scheduleTimeoutFails_ordersStayCommitted() {
        doThrow(new IllegalStateException("redis")).when(orderCancelSchedule)
                .scheduleTimeout(anyString(), anyString(), anyString(), anyInt());

        assertDoesNotThrow(() -> consumer.processOrders(List.of(msg("tk1", T1, 1, "user-1"))));
        verify(txStatus, never()).setRollbackOnly();
        verify(stockOrderCacheService, never()).increaseStockCacheByLUA(any(), anyInt());
    }
}
