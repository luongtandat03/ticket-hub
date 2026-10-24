package vn.tphcm.ddd.application.service.payment;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import vn.tphcm.ddd.application.dto.response.PaymentResponse;
import vn.tphcm.ddd.application.dto.response.TicketDetailResponse;
import vn.tphcm.ddd.application.dto.response.TicketOrderResponse;
import vn.tphcm.ddd.application.dto.response.TicketResponse;
import vn.tphcm.ddd.application.exception.PaymentException;
import vn.tphcm.ddd.application.mapper.PaymentMapper;
import vn.tphcm.ddd.application.service.order.TicketOrderAppService;
import vn.tphcm.ddd.application.service.payment.impl.PaymentAppServiceImpl;
import vn.tphcm.ddd.application.service.ticket.TicketAppService;
import vn.tphcm.ddd.application.service.ticket.TicketDetailAppService;
import vn.tphcm.ddd.domain.model.OutboxEvent;
import vn.tphcm.ddd.domain.model.PaymentTransaction;
import vn.tphcm.ddd.domain.model.User;
import vn.tphcm.ddd.domain.repository.OutboxEventRepository;
import vn.tphcm.ddd.domain.service.PaymentTransactionDomainService;
import vn.tphcm.ddd.domain.service.UserDomainService;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RedisDistributedLocker;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RedisDistributedService;
import vn.tphcm.ddd.infrastructure.gateway.VnPayGatewayService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentAppServiceImplTest {
    @Mock PaymentTransactionDomainService paymentTransactionDomainService;
    @Mock TicketOrderAppService ticketOrderAppService;
    @Mock TicketAppService ticketAppService;
    @Mock TicketDetailAppService ticketDetailAppService;
    @Mock RedisDistributedService redisDistributedService;
    @Mock VnPayGatewayService vnPayGatewayService;
    @Mock TransactionTemplate transactionTemplate;
    @Mock OutboxEventRepository outboxEventRepository;
    @Mock UserDomainService userDomainService;
    @Mock PaymentMapper paymentMapper;
    @Mock RedisDistributedLocker locker;

    @InjectMocks PaymentAppServiceImpl service;

    private static final String ORDER = "OKS-SGN-1-abc";

    @BeforeEach
    void setUp() throws Exception {
        when(redisDistributedService.getRedisDistributedLock(anyString())).thenReturn(locker);
        when(locker.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(transactionTemplate.execute(any())).thenAnswer(inv ->
                ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
        when(vnPayGatewayService.verifySignature(anyMap())).thenReturn(true);
        doAnswer(inv -> {
            ((java.util.function.Consumer<org.springframework.transaction.TransactionStatus>) inv.getArgument(0)).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private Map<String, String> ipn(String amount, String code) {
        Map<String, String> m = new HashMap<>();
        m.put("vnp_OrderInfo", "Order Payment:" + ORDER);
        m.put("vnp_Amount", amount);
        m.put("vnp_ResponseCode", code);
        m.put("vnp_TransactionStatus", code);
        return m;
    }

    private PaymentTransaction tx(int status) {
        return PaymentTransaction.builder()
                .paymentId("pid").orderNumber(ORDER).userId("u1")
                .paymentUrl("url").gatewayTransactionId("gw")
                .amount(new BigDecimal("1500")).paymentStatus(status).build();
    }

    // ---------------- handleVnPayIPN ----------------

    @Test
    void ipn_invalidSignature_returns97AndTouchesNothing() {
        when(vnPayGatewayService.verifySignature(anyMap())).thenReturn(false);

        Map<String, String> rsp = service.handleVnPayIPN(ipn("150000", "00"));

        assertEquals("97", rsp.get("RspCode"));
        verifyNoInteractions(paymentTransactionDomainService, redisDistributedService);
    }

    @Test
    void ipn_missingOrderInfo_returns01() {
        Map<String, String> req = ipn("150000", "00");
        req.remove("vnp_OrderInfo");

        assertEquals("01", service.handleVnPayIPN(req).get("RspCode"));
    }

    @Test
    void ipn_orderInfoWithoutOrderNumber_returns01InsteadOfThrowing() {
        Map<String, String> req = ipn("150000", "00");
        req.put("vnp_OrderInfo", "garbage");

        Map<String, String> rsp = assertDoesNotThrow(() -> service.handleVnPayIPN(req));

        assertEquals("01", rsp.get("RspCode"));
    }

    @Test
    void ipn_unknownPayment_returns01() {
        when(paymentTransactionDomainService.findByOrderNumber(ORDER)).thenReturn(null);

        assertEquals("01", service.handleVnPayIPN(ipn("150000", "00")).get("RspCode"));
    }

    @Test
    void ipn_wrongAmount_returns04() {
        when(paymentTransactionDomainService.findByOrderNumber(ORDER)).thenReturn(tx(1));

        assertEquals("04", service.handleVnPayIPN(ipn("999", "00")).get("RspCode"));
        verify(paymentTransactionDomainService, never()).updateTransactionInProgress(any(), any(), any(), anyInt());
    }

    @Test
    void ipn_nonNumericAmount_returns04() {
        when(paymentTransactionDomainService.findByOrderNumber(ORDER)).thenReturn(tx(1));

        assertEquals("04", service.handleVnPayIPN(ipn("abc", "00")).get("RspCode"));
    }

    @Test
    void ipn_repeatedCall_isIdempotent() {
        when(paymentTransactionDomainService.findByOrderNumber(ORDER)).thenReturn(tx(2));

        assertEquals("02", service.handleVnPayIPN(ipn("150000", "00")).get("RspCode"));
        verify(paymentTransactionDomainService, never()).updateTransactionInProgress(any(), any(), any(), anyInt());
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void ipn_failedPayment_cancelsOrderAndAcknowledgesWith00() {
        when(paymentTransactionDomainService.findByOrderNumber(ORDER)).thenReturn(tx(1));

        Map<String, String> rsp = service.handleVnPayIPN(ipn("150000", "24"));

        assertEquals("00", rsp.get("RspCode"));
        verify(paymentTransactionDomainService).updateTransactionInProgress(eq("pid"), any(), any(), eq(3));
        verify(ticketOrderAppService).cancelOrderAfterPayment(ORDER);
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void ipn_successfulPayment_marksPaidAndQueuesTicketEmail() {
        when(paymentTransactionDomainService.findByOrderNumber(ORDER)).thenReturn(tx(1));
        when(ticketOrderAppService.markOrderPaid(ORDER)).thenReturn(true);

        TicketOrderResponse order = new TicketOrderResponse();
        order.setOrderNumber(ORDER);
        order.setTicketId("t1");
        order.setQuantity(2);
        order.setTotalAmount(new BigDecimal("1500"));
        when(ticketOrderAppService.findByOrderNumber(ORDER)).thenReturn(order);

        User buyer = new User();
        buyer.setEmail("buyer@x.com");
        when(userDomainService.findById("u1")).thenReturn(buyer);

        TicketDetailResponse detail = new TicketDetailResponse();
        detail.setActivityId("act1");
        detail.setName("VIP");
        when(ticketDetailAppService.getTicketDetail(eq("t1"), any())).thenReturn(detail);

        TicketResponse ticket = new TicketResponse();
        ticket.setDescription("Concert");
        ticket.setStartTime(LocalDateTime.of(2026, 12, 31, 20, 0));
        when(ticketAppService.getTicketById("act1")).thenReturn(ticket);

        Map<String, String> rsp = service.handleVnPayIPN(ipn("150000", "00"));

        assertEquals("00", rsp.get("RspCode"));
        verify(paymentTransactionDomainService).updateTransactionInProgress(eq("pid"), any(), any(), eq(2));
        verify(ticketOrderAppService, never()).cancelOrderAfterPayment(any());

        var captor = org.mockito.ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertEquals("SEND_EMAIL", captor.getValue().getEventType());
        assertTrue(captor.getValue().getPayload().contains("buyer@x.com"));
        assertTrue(captor.getValue().getPayload().contains("PURCHASE"));
    }

    @Test
    void ipn_lockNotAcquired_returns99WithoutProcessing() throws Exception {
        when(locker.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        assertEquals("99", service.handleVnPayIPN(ipn("150000", "00")).get("RspCode"));
        verify(paymentTransactionDomainService, never()).findByOrderNumber(any());
        verify(locker).unlock();
    }

    @Test
    void ipn_unexpectedError_returns99AndReleasesLock() {
        when(paymentTransactionDomainService.findByOrderNumber(ORDER)).thenThrow(new IllegalStateException("db down"));

        assertEquals("99", service.handleVnPayIPN(ipn("150000", "00")).get("RspCode"));
        verify(locker).unlock();
    }

    @Test
    void ipn_usesSameLockKeyAsPaymentOrder() {
        when(paymentTransactionDomainService.findByOrderNumber(ORDER)).thenReturn(null);

        service.handleVnPayIPN(ipn("150000", "00"));

        verify(redisDistributedService).getRedisDistributedLock("LOCK:PAYMENT:" + ORDER);
    }

    // ---------------- paymentOrder ----------------

    private void loginAs(String id) {
        User user = new User();
        user.setId(id);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
    }

    @Test
    void paymentOrder_lockBusy_throwsPaymentException() throws Exception {
        loginAs("u1");
        when(locker.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        assertThrows(PaymentException.class, () -> service.paymentOrder(ORDER, "VNPAY"));
        verify(locker).unlock();
    }

    @Test
    void paymentOrder_orderNotPending_throwsPaymentException() {
        loginAs("u1");
        TicketOrderResponse order = new TicketOrderResponse();
        order.setOrderStatus(2);
        when(ticketOrderAppService.findByOrderNumber(ORDER)).thenReturn(order);

        PaymentException ex = assertThrows(PaymentException.class, () -> service.paymentOrder(ORDER, "VNPAY"));
        assertTrue(ex.getMessage().contains("Payment Order Not Found"));
        verify(locker).unlock();
    }

    @Test
    void paymentOrder_orderMissing_throwsPaymentException() {
        loginAs("u1");
        when(ticketOrderAppService.findByOrderNumber(ORDER)).thenReturn(null);

        assertThrows(PaymentException.class, () -> service.paymentOrder(ORDER, "VNPAY"));
    }

    @Test
    void paymentOrder_success_returnsGatewayUrlAndMarksInProgress() throws Exception {
        loginAs("u1");
        TicketOrderResponse order = new TicketOrderResponse();
        order.setOrderStatus(0);
        order.setTotalAmount(new BigDecimal("1500"));
        when(ticketOrderAppService.findByOrderNumber(ORDER)).thenReturn(order);
        when(vnPayGatewayService.createPaymentUrl(any())).thenReturn("https://pay/url");

        assertEquals("https://pay/url", service.paymentOrder(ORDER, "VNPAY"));
        verify(paymentTransactionDomainService).createPaymentTransaction(any());
        verify(paymentTransactionDomainService).updateTransactionInProgress(any(), eq("https://pay/url"), any(), eq(1));
        verify(locker).unlock();
    }

    // ---------------- findAll ----------------

    @Test
    void findAll_mapsEveryTransaction() {
        PaymentTransaction a = tx(1);
        PaymentTransaction b = tx(2);
        PaymentResponse ra = new PaymentResponse();
        PaymentResponse rb = new PaymentResponse();
        when(paymentTransactionDomainService.findAll()).thenReturn(List.of(a, b));
        when(paymentMapper.toDto(a)).thenReturn(ra);
        when(paymentMapper.toDto(b)).thenReturn(rb);

        assertEquals(List.of(ra, rb), service.findAll());
    }

    @Test
    void findAll_emptyList() {
        when(paymentTransactionDomainService.findAll()).thenReturn(List.of());

        assertTrue(service.findAll().isEmpty());
    }

    // ---------------- tiền về sau khi đơn đã bị huỷ ----------------

    @Test
    void ipn_paymentArrivesAfterOrderCancelled_marksRefundRequiredAndSendsNoTicket() {
        when(paymentTransactionDomainService.findByOrderNumber(ORDER)).thenReturn(tx(1));
        when(ticketOrderAppService.markOrderPaid(ORDER)).thenReturn(false);

        Map<String, String> rsp = service.handleVnPayIPN(ipn("150000", "00"));

        assertEquals("00", rsp.get("RspCode"));
        verify(paymentTransactionDomainService).updateTransactionInProgress(eq("pid"), any(), any(), eq(4));
        verify(paymentTransactionDomainService, never()).updateTransactionInProgress(any(), any(), any(), eq(2));
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void ipn_wrongAmount_neverMarksOrderPaid() {
        // Bảo mật: số tiền sai thì tuyệt đối không được xác nhận đơn
        when(paymentTransactionDomainService.findByOrderNumber(ORDER)).thenReturn(tx(1));
        when(ticketOrderAppService.markOrderPaid(ORDER)).thenReturn(true);

        service.handleVnPayIPN(ipn("100", "00"));

        verify(ticketOrderAppService, never()).markOrderPaid(any());
        verifyNoInteractions(outboxEventRepository);
    }

    // ---------------- paymentOrder: thời điểm tạo link và dùng lại link ----------------

    @Test
    void paymentOrder_storesVnPayCreateDateBeforeBuildingUrl() throws Exception {
        loginAs("u1");
        TicketOrderResponse order = new TicketOrderResponse();
        order.setOrderStatus(0);
        order.setTotalAmount(new BigDecimal("1500"));
        when(ticketOrderAppService.findByOrderNumber(ORDER)).thenReturn(order);
        when(vnPayGatewayService.createPaymentUrl(any())).thenReturn("https://pay/url");

        service.paymentOrder(ORDER, "VNPAY");

        var captor = org.mockito.ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionDomainService).createPaymentTransaction(captor.capture());
        String createDate = captor.getValue().getCreateDate();
        assertNotNull(createDate, "querydr cần đúng vnp_CreateDate đã gửi cho VNPay");
        assertTrue(createDate.matches("\\d{14}"), "định dạng yyyyMMddHHmmss: " + createDate);
    }

    @Test
    void paymentOrder_openPaymentExists_returnsSameLinkWithoutNewTransaction() throws Exception {
        loginAs("u1");
        TicketOrderResponse order = new TicketOrderResponse();
        order.setOrderStatus(0);
        when(ticketOrderAppService.findByOrderNumber(ORDER)).thenReturn(order);
        PaymentTransaction open = tx(1);
        open.setPaymentUrl("https://pay/old");
        when(paymentTransactionDomainService.findByOrderNumber(ORDER)).thenReturn(open);

        assertEquals("https://pay/old", service.paymentOrder(ORDER, "VNPAY"));
        verify(paymentTransactionDomainService, never()).createPaymentTransaction(any());
        verify(vnPayGatewayService, never()).createPaymentUrl(any());
    }

    // ---------------- reconcileStuckPayment ----------------

    private void stuck(PaymentTransaction transaction) {
        when(paymentTransactionDomainService.findStuckInProgress(any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of(transaction));
        when(paymentTransactionDomainService.findByOrderNumber(ORDER)).thenReturn(transaction);
    }

    private Map<String, String> query(String responseCode, String transactionStatus, String amount) {
        Map<String, String> m = new HashMap<>();
        m.put("vnp_ResponseCode", responseCode);
        if (transactionStatus != null) {
            m.put("vnp_TransactionStatus", transactionStatus);
        }
        if (amount != null) {
            m.put("vnp_Amount", amount);
        }
        m.put("vnp_TransactionNo", "14123456");
        return m;
    }

    private void ticketDataForEmail() {
        TicketOrderResponse order = new TicketOrderResponse();
        order.setOrderNumber(ORDER);
        order.setTicketId("t1");
        order.setQuantity(2);
        order.setTotalAmount(new BigDecimal("1500"));
        when(ticketOrderAppService.findByOrderNumber(ORDER)).thenReturn(order);
        User buyer = new User();
        buyer.setEmail("buyer@x.com");
        when(userDomainService.findById("u1")).thenReturn(buyer);
        TicketDetailResponse detail = new TicketDetailResponse();
        detail.setActivityId("act1");
        when(ticketDetailAppService.getTicketDetail(eq("t1"), any())).thenReturn(detail);
        TicketResponse ticket = new TicketResponse();
        ticket.setStartTime(LocalDateTime.of(2026, 12, 31, 20, 0));
        when(ticketAppService.getTicketById("act1")).thenReturn(ticket);
    }

    @Test
    void reconcile_onlyLooksAtPaymentsOlderThanGivenMinutes() {
        when(paymentTransactionDomainService.findStuckInProgress(any(LocalDateTime.class), anyInt())).thenReturn(List.of());
        LocalDateTime before = LocalDateTime.now();

        assertEquals(0, service.reconcileStuckPayment(16, 50));

        var cutoff = org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
        verify(paymentTransactionDomainService).findStuckInProgress(cutoff.capture(), eq(50));
        assertTrue(Math.abs(java.time.Duration.between(before.minusMinutes(16), cutoff.getValue()).toSeconds()) < 5);
    }

    @Test
    void reconcile_paidOnVnPay_marksPaidStoresVnPayTransactionNoAndSendsTicket() {
        stuck(tx(1));
        ticketDataForEmail();
        when(vnPayGatewayService.queryTransaction(any())).thenReturn(query("00", "00", "150000"));
        when(ticketOrderAppService.markOrderPaid(ORDER)).thenReturn(true);

        assertEquals(1, service.reconcileStuckPayment(16, 50));

        verify(ticketOrderAppService).markOrderPaid(ORDER);
        verify(paymentTransactionDomainService).updateTransactionInProgress(eq("pid"), any(), eq("14123456"), eq(2));
        verify(paymentTransactionDomainService, never()).updateTransactionInProgress(any(), any(), any(), eq(3));
        verify(ticketOrderAppService, never()).cancelOrderAfterPayment(any());
        verify(outboxEventRepository).save(any(OutboxEvent.class));
        verify(redisDistributedService).getRedisDistributedLock("LOCK:PAYMENT:" + ORDER);
        verify(locker).unlock();
    }

    @Test
    void reconcile_paidButOrderAlreadyCancelled_marksRefundRequired() {
        stuck(tx(1));
        when(vnPayGatewayService.queryTransaction(any())).thenReturn(query("00", "00", "150000"));
        when(ticketOrderAppService.markOrderPaid(ORDER)).thenReturn(false);

        service.reconcileStuckPayment(16, 50);

        verify(paymentTransactionDomainService).updateTransactionInProgress(eq("pid"), any(), any(), eq(4));
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void reconcile_failedOnVnPay_marksFailedAndCancelsOrder() {
        stuck(tx(1));
        when(vnPayGatewayService.queryTransaction(any())).thenReturn(query("00", "02", "150000"));

        assertEquals(1, service.reconcileStuckPayment(16, 50));

        verify(paymentTransactionDomainService).updateTransactionInProgress(eq("pid"), any(), any(), eq(3));
        verify(ticketOrderAppService).cancelOrderAfterPayment(ORDER);
        verify(ticketOrderAppService, never()).markOrderPaid(any());
    }

    @Test
    void reconcile_transactionUnknownToVnPay_marksFailedAndCancelsOrder() {
        stuck(tx(1));
        when(vnPayGatewayService.queryTransaction(any())).thenReturn(query("91", null, null));

        assertEquals(1, service.reconcileStuckPayment(16, 50));

        verify(paymentTransactionDomainService).updateTransactionInProgress(eq("pid"), any(), any(), eq(3));
        verify(ticketOrderAppService).cancelOrderAfterPayment(ORDER);
    }

    @Test
    void reconcile_stillPendingOnVnPay_isLeftForNextRound() {
        stuck(tx(1));
        when(vnPayGatewayService.queryTransaction(any())).thenReturn(query("00", "01", "150000"));

        assertEquals(0, service.reconcileStuckPayment(16, 50));

        verify(paymentTransactionDomainService, never()).updateTransactionInProgress(any(), any(), any(), anyInt());
        verify(ticketOrderAppService, never()).cancelOrderAfterPayment(any());
    }

    @Test
    void reconcile_amountMismatch_neverMarksPaid() {
        stuck(tx(1));
        when(vnPayGatewayService.queryTransaction(any())).thenReturn(query("00", "00", "100"));

        assertEquals(0, service.reconcileStuckPayment(16, 50));

        verify(ticketOrderAppService, never()).markOrderPaid(any());
        verify(paymentTransactionDomainService, never()).updateTransactionInProgress(any(), any(), any(), anyInt());
    }

    @Test
    void reconcile_queryFailedOrBadSignature_isLeftForNextRound() {
        stuck(tx(1));
        when(vnPayGatewayService.queryTransaction(any())).thenReturn(Map.of());

        assertEquals(0, service.reconcileStuckPayment(16, 50));

        verify(paymentTransactionDomainService, never()).updateTransactionInProgress(any(), any(), any(), anyInt());
    }

    @Test
    void reconcile_otherResponseCode_isLeftForNextRound() {
        stuck(tx(1));
        when(vnPayGatewayService.queryTransaction(any())).thenReturn(query("97", null, null));

        assertEquals(0, service.reconcileStuckPayment(16, 50));

        verify(paymentTransactionDomainService, never()).updateTransactionInProgress(any(), any(), any(), anyInt());
    }

    @Test
    void reconcile_ipnArrivedWhileWaitingForLock_doesNotQueryVnPay() {
        when(paymentTransactionDomainService.findStuckInProgress(any(LocalDateTime.class), anyInt())).thenReturn(List.of(tx(1)));
        when(paymentTransactionDomainService.findByOrderNumber(ORDER)).thenReturn(tx(2));

        assertEquals(0, service.reconcileStuckPayment(16, 50));

        verify(vnPayGatewayService, never()).queryTransaction(any());
    }

    @Test
    void reconcile_lockBusy_skips() throws Exception {
        stuck(tx(1));
        when(locker.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        assertEquals(0, service.reconcileStuckPayment(16, 50));

        verify(vnPayGatewayService, never()).queryTransaction(any());
    }

    @Test
    void reconcile_oneFailureDoesNotStopOthers() {
        PaymentTransaction first = tx(1);
        first.setOrderNumber("OKS-SGN-1-first");
        PaymentTransaction second = tx(1);
        when(paymentTransactionDomainService.findStuckInProgress(any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of(first, second));
        when(paymentTransactionDomainService.findByOrderNumber("OKS-SGN-1-first")).thenThrow(new IllegalStateException("db"));
        when(paymentTransactionDomainService.findByOrderNumber(ORDER)).thenReturn(second);
        when(vnPayGatewayService.queryTransaction(any())).thenReturn(query("00", "02", "150000"));

        assertEquals(1, service.reconcileStuckPayment(16, 50));
        verify(ticketOrderAppService).cancelOrderAfterPayment(ORDER);
    }
}
