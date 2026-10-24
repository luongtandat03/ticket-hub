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
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import vn.tphcm.ddd.application.dto.PageResponse;
import vn.tphcm.ddd.application.dto.response.PlaceOrderResponse;
import vn.tphcm.ddd.application.dto.response.TicketOrderResponse;
import vn.tphcm.ddd.application.schedule.OrderCancelSchedule;
import vn.tphcm.ddd.application.service.order.cache.StockOrderCacheService;
import vn.tphcm.ddd.application.service.order.impl.TicketOrderAppServiceImpl;
import vn.tphcm.ddd.domain.model.PaymentTransaction;
import vn.tphcm.ddd.domain.model.TicketOrder;
import vn.tphcm.ddd.domain.model.User;
import vn.tphcm.ddd.domain.service.OrderDeductionDomainService;
import vn.tphcm.ddd.domain.service.PaymentTransactionDomainService;
import vn.tphcm.ddd.domain.service.TicketOrderDomainService;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RedisDistributedLocker;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RedisDistributedService;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TicketOrderAppServiceImplTest {
    @Mock StockOrderCacheService stockOrderCacheService;
    @Mock TicketOrderDomainService ticketOrderDomainService;
    @Mock OrderDeductionDomainService orderDeductionDomainService;
    @Mock OrderCancelSchedule orderCancelScheduleService;
    @Mock RedisDistributedService redisDistributedService;
    @Mock RedisDistributedLocker locker;
    @Mock PaymentTransactionDomainService paymentTransactionDomainService;
    @Mock TransactionTemplate transactionTemplate;
    @Mock TransactionStatus txStatus;

    @InjectMocks TicketOrderAppServiceImpl service;

    private static final String TICKET = "t1";
    private static final String USER = "user-1";
    private final String orderNumber = "OKS-SGN-" + System.currentTimeMillis() + "-" + "abc123";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void setUp() throws Exception {
        User user = new User();
        user.setId(USER);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

        when(redisDistributedService.getRedisDistributedLock(anyString())).thenReturn(locker);
        when(locker.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        // Chạy callback như Spring thật: BEGIN -> callback -> COMMIT/ROLLBACK (txStatus ghi nhận rollback)
        when(transactionTemplate.execute(any()))
                .thenAnswer(inv -> inv.<TransactionCallback<?>>getArgument(0).doInTransaction(txStatus));
    }

    /** Một dòng kết quả native-query theo đúng thứ tự cột mà service đọc. */
    private Object[] row(String userId, int status) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.of(2026, 10, 1, 10, 0));
        return new Object[]{
                "id1", userId, TICKET, orderNumber, 2, status,
                new BigDecimal("10000"), "OKS-SGN", now, "note", now, now
        };
    }

    // ---------------- placeOrderCAS ----------------

    @Test
    void placeOrder_success_createsPendingOrderAndSchedulesTimeout() {
        when(stockOrderCacheService.decreaseStockCache(TICKET, 2)).thenReturn(1);
        when(ticketOrderDomainService.decreaseStock(TICKET, 2)).thenReturn(true);
        when(stockOrderCacheService.getEffectivePrice(TICKET)).thenReturn(5000L);

        PlaceOrderResponse rsp = service.placeOrderCAS(TICKET, 2);

        assertTrue(rsp.isSuccess());
        assertNotNull(rsp.getPlaceOrderTaskId());
        assertTrue(rsp.getPlaceOrderTaskId().startsWith("OKS-SGN-"));

        ArgumentCaptor<TicketOrder> captor = ArgumentCaptor.forClass(TicketOrder.class);
        verify(orderDeductionDomainService).insertOrder(matches("\\d{6}"), captor.capture());
        assertEquals(0, captor.getValue().getOrderStatus());
        assertEquals(2, captor.getValue().getQuantity());
        assertEquals(0, new BigDecimal("10000").compareTo(captor.getValue().getTotalAmount()));
        verify(orderCancelScheduleService).scheduleTimeout(eq(rsp.getPlaceOrderTaskId()), anyString(), eq(TICKET), eq(2));

        // Mỗi đơn chỉ được trừ tồn kho DB một lần, và phải nằm trong transaction
        verify(ticketOrderDomainService, times(1)).decreaseStock(TICKET, 2);
        verify(transactionTemplate, times(1)).execute(any());
        verify(txStatus, never()).setRollbackOnly();
        verify(stockOrderCacheService, never()).increaseStockCacheByLUA(any(), anyInt());
    }

    @Test
    void placeOrder_insertsOrderBeforeDecreasingStock_soRowLockIsHeldShortest() {
        when(stockOrderCacheService.decreaseStockCache(TICKET, 1)).thenReturn(1);
        when(ticketOrderDomainService.decreaseStock(TICKET, 1)).thenReturn(true);
        when(stockOrderCacheService.getEffectivePrice(TICKET)).thenReturn(5000L);

        service.placeOrderCAS(TICKET, 1);

        var order = inOrder(orderDeductionDomainService, ticketOrderDomainService, orderCancelScheduleService);
        order.verify(orderDeductionDomainService).insertOrder(anyString(), any());
        order.verify(ticketOrderDomainService).decreaseStock(TICKET, 1);
        order.verify(orderCancelScheduleService).scheduleTimeout(anyString(), anyString(), eq(TICKET), eq(1));
    }

    @Test
    void placeOrder_scheduleTimeoutFails_orderStillSucceeds() {
        when(stockOrderCacheService.decreaseStockCache(TICKET, 1)).thenReturn(1);
        when(ticketOrderDomainService.decreaseStock(TICKET, 1)).thenReturn(true);
        when(stockOrderCacheService.getEffectivePrice(TICKET)).thenReturn(5000L);
        doThrow(new IllegalStateException("redis")).when(orderCancelScheduleService)
                .scheduleTimeout(anyString(), anyString(), anyString(), anyInt());

        assertTrue(service.placeOrderCAS(TICKET, 1).isSuccess());
        // Đơn đã commit: không được trả vé lại cho Redis (safety-net job sẽ huỷ đơn nếu cần)
        verify(stockOrderCacheService, never()).increaseStockCacheByLUA(any(), anyInt());
    }

    @Test
    void placeOrder_cacheMiss_warmsUpThenSucceeds() {
        when(stockOrderCacheService.decreaseStockCache(TICKET, 1)).thenReturn(-1, 1);
        when(stockOrderCacheService.addStockAvailableToCache(TICKET)).thenReturn(true);
        when(ticketOrderDomainService.decreaseStock(TICKET, 1)).thenReturn(true);
        when(stockOrderCacheService.getEffectivePrice(TICKET)).thenReturn(5000L);

        assertTrue(service.placeOrderCAS(TICKET, 1).isSuccess());
        verify(stockOrderCacheService).addStockAvailableToCache(TICKET);
    }

    @Test
    void placeOrder_cacheMissAndTicketUnknown_failsWithNotFound() {
        when(stockOrderCacheService.decreaseStockCache(TICKET, 1)).thenReturn(-1);
        when(stockOrderCacheService.addStockAvailableToCache(TICKET)).thenReturn(false);

        PlaceOrderResponse rsp = service.placeOrderCAS(TICKET, 1);

        assertFalse(rsp.isSuccess());
        assertEquals("TICKET_NOT_FOUND", rsp.getCode());
        verifyNoInteractions(ticketOrderDomainService, orderDeductionDomainService);
    }

    @Test
    void placeOrder_outOfStock_failsWithoutTouchingDatabase() {
        when(stockOrderCacheService.decreaseStockCache(TICKET, 5)).thenReturn(0);

        PlaceOrderResponse rsp = service.placeOrderCAS(TICKET, 5);

        assertFalse(rsp.isSuccess());
        assertEquals("OUT_OF_STOCK", rsp.getCode());
        verifyNoInteractions(ticketOrderDomainService, orderDeductionDomainService);
        verify(stockOrderCacheService, never()).increaseStockCacheByLUA(any(), anyInt());
    }

    @Test
    void placeOrder_databaseStockConflict_rollsBackRedis() {
        when(stockOrderCacheService.decreaseStockCache(TICKET, 2)).thenReturn(1);
        when(stockOrderCacheService.getEffectivePrice(TICKET)).thenReturn(5000L);
        when(ticketOrderDomainService.decreaseStock(TICKET, 2)).thenReturn(false);

        PlaceOrderResponse rsp = service.placeOrderCAS(TICKET, 2);

        assertEquals("STOCK_CONFLICT", rsp.getCode());
        verify(stockOrderCacheService, times(1)).increaseStockCacheByLUA(TICKET, 2);
        verify(ticketOrderDomainService, times(1)).decreaseStock(TICKET, 2);
        verify(txStatus).setRollbackOnly(); // huỷ cả dòng đơn vừa insert
        verify(orderCancelScheduleService, never()).scheduleTimeout(any(), any(), any(), anyInt());
    }

    @Test
    void placeOrder_priceUnknown_rollsBackRedis() {
        when(stockOrderCacheService.decreaseStockCache(TICKET, 2)).thenReturn(1);
        when(ticketOrderDomainService.decreaseStock(TICKET, 2)).thenReturn(true);
        when(stockOrderCacheService.getEffectivePrice(TICKET)).thenReturn(-1L);

        PlaceOrderResponse rsp = service.placeOrderCAS(TICKET, 2);

        assertEquals("PRICE_NOT_FOUND", rsp.getCode());
        verify(stockOrderCacheService).increaseStockCacheByLUA(TICKET, 2);
        // Không có giá thì không được trừ tồn kho DB (trước đây bị commit nhầm)
        verifyNoInteractions(ticketOrderDomainService, orderDeductionDomainService, transactionTemplate);
    }

    @Test
    void placeOrder_unexpectedErrorAfterRedisDecrement_compensatesRedis() {
        when(stockOrderCacheService.decreaseStockCache(TICKET, 2)).thenReturn(1);
        when(stockOrderCacheService.getEffectivePrice(TICKET)).thenReturn(5000L);
        when(ticketOrderDomainService.decreaseStock(TICKET, 2)).thenThrow(new IllegalStateException("db"));

        PlaceOrderResponse rsp = service.placeOrderCAS(TICKET, 2);

        assertEquals("SERVER_ERROR", rsp.getCode());
        verify(stockOrderCacheService, times(1)).increaseStockCacheByLUA(TICKET, 2);
        verify(orderCancelScheduleService, never()).scheduleTimeout(any(), any(), any(), anyInt());
    }

    @Test
    void placeOrder_errorBeforeRedisDecrement_doesNotRestoreRedis() {
        when(stockOrderCacheService.decreaseStockCache(TICKET, 2)).thenThrow(new IllegalStateException("redis"));

        assertThrows(IllegalStateException.class, () -> service.placeOrderCAS(TICKET, 2));
        verify(stockOrderCacheService, never()).increaseStockCacheByLUA(any(), anyInt());
        verifyNoInteractions(ticketOrderDomainService, orderDeductionDomainService, transactionTemplate);
    }

    // ---------------- decreaseStockCAS ----------------

    @Test
    void decreaseStockCas_noStock_returnsFalse() {
        when(stockOrderCacheService.decreaseStockCacheByLUA(TICKET, 1)).thenReturn(0);

        assertFalse(service.decreaseStockCAS(TICKET, 1));
        verifyNoInteractions(ticketOrderDomainService);
    }

    @Test
    void decreaseStockCas_success_insertsOrder() {
        when(stockOrderCacheService.decreaseStockCacheByLUA(TICKET, 1)).thenReturn(10);
        when(ticketOrderDomainService.decreaseStockCAS(TICKET, 10, 1)).thenReturn(true);

        assertTrue(service.decreaseStockCAS(TICKET, 1));
        verify(orderDeductionDomainService).insertOrder(anyString(), any(TicketOrder.class));
    }

    @Test
    void decreaseStockCas_casLost_doesNotInsertOrder() {
        when(stockOrderCacheService.decreaseStockCacheByLUA(TICKET, 1)).thenReturn(10);
        when(ticketOrderDomainService.decreaseStockCAS(TICKET, 10, 1)).thenReturn(false);

        service.decreaseStockCAS(TICKET, 1);

        verify(orderDeductionDomainService, never()).insertOrder(any(), any());
    }

    @Test
    void decreaseStockCas_exception_returnsFalse() {
        when(stockOrderCacheService.decreaseStockCacheByLUA(TICKET, 1)).thenThrow(new IllegalStateException("x"));

        assertFalse(service.decreaseStockCAS(TICKET, 1));
    }

    // ---------------- queries ----------------

    @Test
    void findAll_mapsRows() {
        when(orderDeductionDomainService.findAll("202610")).thenReturn(List.<Object[]>of(row(USER, 0), row(USER, 1)));

        List<TicketOrderResponse> result = service.findAll("202610");

        assertEquals(2, result.size());
        assertEquals(USER, result.get(0).getUserId());
        assertEquals(1, result.get(1).getOrderStatus());
    }

    @Test
    void findAll_nullFromRepository_returnsEmptyList() {
        when(orderDeductionDomainService.findAll(anyString())).thenReturn(null);

        assertTrue(service.findAll("202610").isEmpty());
    }

    @Test
    void findByDateRange_nullFromRepository_returnsEmptyList() {
        when(orderDeductionDomainService.findByDateRange(any(), any(), any())).thenReturn(null);

        assertTrue(service.findByDateRange("202610", LocalDateTime.now(), LocalDateTime.now()).isEmpty());
    }

    @Test
    void findByOrderNumber_found_mapsAllColumns() {
        when(orderDeductionDomainService.findByOrderNumber(anyString(), eq(orderNumber))).thenReturn(row(USER, 0));

        TicketOrderResponse rsp = service.findByOrderNumber(orderNumber);

        assertEquals(orderNumber, rsp.getOrderNumber());
        assertEquals(TICKET, rsp.getTicketId());
        assertEquals(2, rsp.getQuantity());
        assertEquals(LocalDateTime.of(2026, 10, 1, 10, 0), rsp.getOrderDate());
    }

    @Test
    void findByOrderNumber_notFound_returnsNull() {
        when(orderDeductionDomainService.findByOrderNumber(anyString(), anyString())).thenReturn(null);

        assertNull(service.findByOrderNumber(orderNumber));
    }

    @Test
    void findByOrderNumber_malformedNumber_throwsRuntimeException() {
        assertThrows(RuntimeException.class, () -> service.findByOrderNumber("bad"));
    }

    @Test
    void findPage_fullPage_reportsHasMoreWithoutThrowing() {
        // Id đơn hàng là chuỗi (UUID). Hiện service ép cursor sang Number nên dòng này
        // có thể ném ClassCastException khi trang đầy: test này phát hiện lỗi đó.
        List<Object[]> rows = List.<Object[]>of(row(USER, 0), row(USER, 0));
        when(orderDeductionDomainService.findPage("202610", "", 2)).thenReturn(rows);

        PageResponse<TicketOrderResponse> page = assertDoesNotThrow(() -> service.findPage("202610", "", 2));

        assertTrue(page.isHasMore());
        assertEquals(2, page.getContent().size());
    }

    @Test
    void findPage_partialPage_noMore() {
        when(orderDeductionDomainService.findPage("202610", "", 5)).thenReturn(List.<Object[]>of(row(USER, 0)));

        PageResponse<TicketOrderResponse> page = service.findPage("202610", "", 5);

        assertFalse(page.isHasMore());
        assertNull(page.getNextCursor());
        assertEquals(1, page.getContent().size());
    }

    // ---------------- cancelOrder ----------------

    @Test
    void cancelOrder_lockBusy_returnsFalse() throws Exception {
        when(locker.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        assertFalse(service.cancelOrder(orderNumber));
        verifyNoInteractions(orderDeductionDomainService);
        verify(locker).unlock();
    }

    @Test
    void cancelOrder_orderNotFound_returnsFalse() {
        when(orderDeductionDomainService.findByOrderNumber(anyString(), anyString())).thenReturn(null);

        assertFalse(service.cancelOrder(orderNumber));
    }

    @Test
    void cancelOrder_orderBelongsToSomeoneElse_returnsFalse() {
        when(orderDeductionDomainService.findByOrderNumber(anyString(), anyString())).thenReturn(row("other", 0));

        assertFalse(service.cancelOrder(orderNumber));
        verify(orderDeductionDomainService, never()).compareAndSetOrderStatus(any(), any(), anyInt(), anyInt());
    }

    @Test
    void cancelOrder_success_updatesStatusAndRestoresStock() {
        when(orderDeductionDomainService.findByOrderNumber(anyString(), anyString())).thenReturn(row(USER, 0));
        when(orderDeductionDomainService.compareAndSetOrderStatus(anyString(), eq(orderNumber), eq(0), eq(2))).thenReturn(true);
        when(ticketOrderDomainService.increaseStock(TICKET, 2)).thenReturn(true);
        when(stockOrderCacheService.increaseStockCacheByLUA(TICKET, 2)).thenReturn(true);

        assertTrue(service.cancelOrder(orderNumber));
        verify(ticketOrderDomainService).increaseStock(TICKET, 2);
        verify(stockOrderCacheService).increaseStockCacheByLUA(TICKET, 2);
        verify(locker).unlock();
    }

    @Test
    void cancelOrder_alreadyCancelled_returnsTrueWithoutRestoringStockTwice() {
        when(orderDeductionDomainService.findByOrderNumber(anyString(), anyString())).thenReturn(row(USER, 2));

        assertTrue(service.cancelOrder(orderNumber));
        verify(ticketOrderDomainService, never()).increaseStock(any(), anyInt());
        verify(orderDeductionDomainService, never()).compareAndSetOrderStatus(any(), any(), anyInt(), anyInt());
    }

    @Test
    void cancelOrder_statusUpdateFails_returnsFalseAndKeepsStock() {
        when(orderDeductionDomainService.findByOrderNumber(anyString(), anyString())).thenReturn(row(USER, 0));
        when(orderDeductionDomainService.compareAndSetOrderStatus(anyString(), anyString(), anyInt(), anyInt())).thenReturn(false);

        assertFalse(service.cancelOrder(orderNumber));
        verify(ticketOrderDomainService, never()).increaseStock(any(), anyInt());
    }

    @Test
    void cancelOrder_redisRestoreFails_reWarmsCache() {
        when(orderDeductionDomainService.findByOrderNumber(anyString(), anyString())).thenReturn(row(USER, 0));
        when(orderDeductionDomainService.compareAndSetOrderStatus(anyString(), anyString(), anyInt(), anyInt())).thenReturn(true);
        when(ticketOrderDomainService.increaseStock(TICKET, 2)).thenReturn(true);
        when(stockOrderCacheService.increaseStockCacheByLUA(TICKET, 2)).thenReturn(false);

        assertTrue(service.cancelOrder(orderNumber));
        verify(stockOrderCacheService).addStockAvailableToCache(TICKET);
    }

    @Test
    void cancelOrder_databaseRestoreFails_throwsRuntimeExceptionAndUnlocks() {
        when(orderDeductionDomainService.findByOrderNumber(anyString(), anyString())).thenReturn(row(USER, 0));
        when(orderDeductionDomainService.compareAndSetOrderStatus(anyString(), anyString(), anyInt(), anyInt())).thenReturn(true);
        when(ticketOrderDomainService.increaseStock(TICKET, 2)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> service.cancelOrder(orderNumber));
        verify(locker).unlock();
    }

    // ---------------- cancelOrderAfterPayment ----------------

    @Test
    void cancelAfterPayment_orderMissing_returnsFalse() {
        when(orderDeductionDomainService.findByOrderNumber(anyString(), anyString())).thenReturn(null);

        assertFalse(service.cancelOrderAfterPayment(orderNumber));
    }

    @Test
    void cancelAfterPayment_success_doesNotCheckOwner() {
        when(orderDeductionDomainService.findByOrderNumber(anyString(), anyString())).thenReturn(row("anyone", 0));
        when(orderDeductionDomainService.compareAndSetOrderStatus(anyString(), anyString(), eq(0), eq(2))).thenReturn(true);
        when(ticketOrderDomainService.increaseStock(TICKET, 2)).thenReturn(true);
        when(stockOrderCacheService.increaseStockCacheByLUA(TICKET, 2)).thenReturn(true);

        assertTrue(service.cancelOrderAfterPayment(orderNumber));
    }

    @Test
    void cancelAfterPayment_lockBusy_returnsFalse() throws Exception {
        when(locker.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        assertFalse(service.cancelOrderAfterPayment(orderNumber));
    }

    // ---------------- cancelExpiredOrder / markOrderPaid ----------------

    private PaymentTransaction payment(int status, LocalDateTime createdAt) {
        PaymentTransaction p = PaymentTransaction.builder().orderNumber(orderNumber).paymentStatus(status).build();
        p.setCreatedAt(createdAt);
        return p;
    }

    private void pendingOrderCanBeCancelled() {
        when(orderDeductionDomainService.findByOrderNumber(anyString(), anyString())).thenReturn(row(USER, 0));
        when(orderDeductionDomainService.compareAndSetOrderStatus(anyString(), eq(orderNumber), eq(0), eq(2))).thenReturn(true);
        when(ticketOrderDomainService.increaseStock(TICKET, 2)).thenReturn(true);
        when(stockOrderCacheService.increaseStockCacheByLUA(TICKET, 2)).thenReturn(true);
    }

    @Test
    void cancelExpired_pendingWithoutPayment_cancelsAndRestoresStock() {
        pendingOrderCanBeCancelled();

        assertTrue(service.cancelExpiredOrder(orderNumber));
        verify(ticketOrderDomainService).increaseStock(TICKET, 2);
        verify(stockOrderCacheService).increaseStockCacheByLUA(TICKET, 2);
        verify(redisDistributedService).getRedisDistributedLock("LOCK:PAYMENT:" + orderNumber);
        verify(locker).unlock();
    }

    @Test
    void cancelExpired_paidOrder_isNotCancelled() {
        when(orderDeductionDomainService.findByOrderNumber(anyString(), anyString())).thenReturn(row(USER, 1));

        assertFalse(service.cancelExpiredOrder(orderNumber));
        verify(orderDeductionDomainService, never()).compareAndSetOrderStatus(any(), any(), anyInt(), anyInt());
        verify(ticketOrderDomainService, never()).increaseStock(any(), anyInt());
    }

    @Test
    void cancelExpired_alreadyCancelled_doesNotRestoreStockAgain() {
        when(orderDeductionDomainService.findByOrderNumber(anyString(), anyString())).thenReturn(row(USER, 2));

        assertFalse(service.cancelExpiredOrder(orderNumber));
        verify(ticketOrderDomainService, never()).increaseStock(any(), anyInt());
    }

    @Test
    void cancelExpired_orderMissing_returnsFalse() {
        when(orderDeductionDomainService.findByOrderNumber(anyString(), anyString())).thenReturn(null);

        assertFalse(service.cancelExpiredOrder(orderNumber));
        verify(locker).unlock();
    }

    @Test
    void cancelExpired_unresolvedPayment_isPostponedFiveMinutes() {
        when(orderDeductionDomainService.findByOrderNumber(anyString(), anyString())).thenReturn(row(USER, 0));
        when(paymentTransactionDomainService.findByOrderNumber(orderNumber))
                .thenReturn(payment(1, LocalDateTime.now().minusMinutes(3)));
        long before = System.currentTimeMillis();

        assertFalse(service.cancelExpiredOrder(orderNumber));

        ArgumentCaptor<OrderCancelSchedule.TimeoutPayload> payload = ArgumentCaptor.forClass(OrderCancelSchedule.TimeoutPayload.class);
        ArgumentCaptor<Long> retryAt = ArgumentCaptor.forClass(Long.class);
        verify(orderCancelScheduleService).scheduleAt(payload.capture(), retryAt.capture());
        assertEquals(orderNumber, payload.getValue().getOrderNumber());
        assertEquals(TICKET, payload.getValue().getTicketId());
        assertEquals(2, payload.getValue().getQuantity());
        assertTrue(retryAt.getValue() >= before + 5 * 60_000L
                && retryAt.getValue() <= System.currentTimeMillis() + 5 * 60_000L);

        verify(orderDeductionDomainService, never()).compareAndSetOrderStatus(any(), any(), anyInt(), anyInt());
    }

    @Test
    void cancelExpired_unresolvedPaymentEvenAfterLinkExpired_isLeftToReconciliation() {
        // Người dùng có thể đã trả tiền mà IPN bị mất: chỉ job đối soát mới được kết luận
        when(orderDeductionDomainService.findByOrderNumber(anyString(), anyString())).thenReturn(row(USER, 0));
        when(paymentTransactionDomainService.findByOrderNumber(orderNumber))
                .thenReturn(payment(1, LocalDateTime.now().minusMinutes(30)));

        assertFalse(service.cancelExpiredOrder(orderNumber));
        verify(orderCancelScheduleService).scheduleAt(any(), anyLong());
        verify(ticketOrderDomainService, never()).increaseStock(any(), anyInt());
    }

    @Test
    void cancelExpired_paymentFailedEarlier_cancels() {
        pendingOrderCanBeCancelled();
        when(paymentTransactionDomainService.findByOrderNumber(orderNumber))
                .thenReturn(payment(3, LocalDateTime.now().minusMinutes(1)));

        assertTrue(service.cancelExpiredOrder(orderNumber));
    }

    @Test
    void cancelExpired_lostRaceToPayment_doesNotRestoreStock() {
        when(orderDeductionDomainService.findByOrderNumber(anyString(), anyString())).thenReturn(row(USER, 0));
        when(orderDeductionDomainService.compareAndSetOrderStatus(anyString(), anyString(), anyInt(), anyInt())).thenReturn(false);

        assertFalse(service.cancelExpiredOrder(orderNumber));
        verify(ticketOrderDomainService, never()).increaseStock(any(), anyInt());
        verify(stockOrderCacheService, never()).increaseStockCacheByLUA(any(), anyInt());
    }

    @Test
    void cancelExpired_lockBusy_skipsWithoutTouchingDatabase() throws Exception {
        when(locker.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        assertFalse(service.cancelExpiredOrder(orderNumber));
        verifyNoInteractions(orderDeductionDomainService, paymentTransactionDomainService);
    }

    @Test
    void markOrderPaid_movesPendingToPaid() {
        when(orderDeductionDomainService.compareAndSetOrderStatus(anyString(), eq(orderNumber), eq(0), eq(1))).thenReturn(true);

        assertTrue(service.markOrderPaid(orderNumber));
    }

    @Test
    void markOrderPaid_orderNoLongerPending_returnsFalse() {
        when(orderDeductionDomainService.compareAndSetOrderStatus(anyString(), anyString(), anyInt(), anyInt())).thenReturn(false);

        assertFalse(service.markOrderPaid(orderNumber));
    }

    @Test
    void cancelOrder_paidOrder_isNotCancelledByUser() {
        when(orderDeductionDomainService.findByOrderNumber(anyString(), anyString())).thenReturn(row(USER, 1));
        when(orderDeductionDomainService.compareAndSetOrderStatus(anyString(), anyString(), eq(0), eq(2))).thenReturn(false);

        assertFalse(service.cancelOrder(orderNumber));
        verify(ticketOrderDomainService, never()).increaseStock(any(), anyInt());
    }

    // ---------------- người dùng tự huỷ khi đang thanh toán ----------------

    @Test
    void cancelOrder_usesSameLockAsPayment() {
        pendingOrderCanBeCancelled();

        assertTrue(service.cancelOrder(orderNumber));
        verify(redisDistributedService).getRedisDistributedLock("LOCK:PAYMENT:" + orderNumber);
    }

    @Test
    void cancelOrder_paymentOpenOnVnPay_isRejected() {
        when(orderDeductionDomainService.findByOrderNumber(anyString(), anyString())).thenReturn(row(USER, 0));
        when(paymentTransactionDomainService.findByOrderNumber(orderNumber))
                .thenReturn(payment(1, LocalDateTime.now().minusMinutes(2)));

        assertFalse(service.cancelOrder(orderNumber));
        verify(orderDeductionDomainService, never()).compareAndSetOrderStatus(any(), any(), anyInt(), anyInt());
        verify(ticketOrderDomainService, never()).increaseStock(any(), anyInt());
    }

    @Test
    void cancelOrder_paymentFailedEarlier_canBeCancelled() {
        pendingOrderCanBeCancelled();
        when(paymentTransactionDomainService.findByOrderNumber(orderNumber))
                .thenReturn(payment(3, LocalDateTime.now().minusMinutes(2)));

        assertTrue(service.cancelOrder(orderNumber));
    }

    // ---------------- huỷ tự động: trả vé ----------------

    @Test
    void cancelExpired_restoresStockInDatabaseBeforeRedis() {
        pendingOrderCanBeCancelled();

        service.cancelExpiredOrder(orderNumber);

        var inOrder = inOrder(orderDeductionDomainService, ticketOrderDomainService, stockOrderCacheService);
        inOrder.verify(orderDeductionDomainService).compareAndSetOrderStatus(anyString(), eq(orderNumber), eq(0), eq(2));
        inOrder.verify(ticketOrderDomainService).increaseStock(TICKET, 2);
        inOrder.verify(stockOrderCacheService).increaseStockCacheByLUA(TICKET, 2);
    }

    @Test
    void cancelExpired_databaseStockRestoreFails_throwsSoTransactionRollsBackOrderStatus() {
        when(orderDeductionDomainService.findByOrderNumber(anyString(), anyString())).thenReturn(row(USER, 0));
        when(orderDeductionDomainService.compareAndSetOrderStatus(anyString(), anyString(), eq(0), eq(2))).thenReturn(true);
        when(ticketOrderDomainService.increaseStock(TICKET, 2)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> service.cancelExpiredOrder(orderNumber));
        verify(stockOrderCacheService, never()).increaseStockCacheByLUA(any(), anyInt());
        verify(locker).unlock();
    }

    @Test
    void cancelExpired_redisStockKeyMissing_reloadsStockIntoRedis() {
        pendingOrderCanBeCancelled();
        when(stockOrderCacheService.increaseStockCacheByLUA(TICKET, 2)).thenReturn(false);

        assertTrue(service.cancelExpiredOrder(orderNumber));
        verify(stockOrderCacheService).addStockAvailableToCache(TICKET);
    }

    @Test
    void cancelExpired_restoresExactlyTheOrderedQuantity() {
        when(orderDeductionDomainService.findByOrderNumber(anyString(), anyString())).thenReturn(row(USER, 0));
        when(orderDeductionDomainService.compareAndSetOrderStatus(anyString(), anyString(), eq(0), eq(2))).thenReturn(true);
        when(ticketOrderDomainService.increaseStock(anyString(), anyInt())).thenReturn(true);
        when(stockOrderCacheService.increaseStockCacheByLUA(anyString(), anyInt())).thenReturn(true);

        service.cancelExpiredOrder(orderNumber);

        // row(...) có quantity = 2: không được trả thiếu hay dư vé
        verify(ticketOrderDomainService).increaseStock(TICKET, 2);
        verify(stockOrderCacheService).increaseStockCacheByLUA(TICKET, 2);
        verify(ticketOrderDomainService, times(1)).increaseStock(anyString(), anyInt());
    }
}
