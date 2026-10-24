package vn.tphcm.ddd.application.cronjob;

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.annotation.KafkaListener;
import vn.tphcm.ddd.application.schedule.OrderCancelSchedule;
import vn.tphcm.ddd.application.schedule.OrderCancelSchedule.TimeoutPayload;
import vn.tphcm.ddd.application.service.order.TicketOrderAppService;
import vn.tphcm.ddd.application.service.order.kafka.KafkaOrderCancelConsumer;
import vn.tphcm.ddd.domain.service.OrderDeductionDomainService;
import vn.tphcm.ddd.infrastructure.cache.redis.RedisInfrastructureService;
import vn.tphcm.ddd.infrastructure.config.kafka.KafkaTopicConfig;
import vn.tphcm.ddd.infrastructure.mq.order.KafkaOrderProducer;
import vn.tphcm.ddd.infrastructure.mq.order.OrderCancelMQMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderTimeoutTest {

    private final String raw = JSON.toJSONString(new TimeoutPayload("OKS-SGN-1-abc", "202610", "t1", 2));

    // ---------------- OrderCancelSchedule ----------------

    @Test
    void schedule_storesJsonMemberWithExpiryAsScore() {
        RedisInfrastructureService redis = mock(RedisInfrastructureService.class);
        OrderCancelSchedule schedule = new OrderCancelSchedule(redis);
        long before = System.currentTimeMillis();

        schedule.scheduleTimeout("OKS-SGN-1-abc", "202610", "t1", 2);

        ArgumentCaptor<Object> member = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Long> score = ArgumentCaptor.forClass(Long.class);
        verify(redis).zAdd(eq("order:cancel"), member.capture(), score.capture());

        assertEquals(raw, member.getValue(), "member phải là chuỗi JSON để đọc và xoá lại được");
        long fiveMinutes = OrderCancelSchedule.PAYMENT_TIMEOUT_MINUTES * 60_000L;
        assertTrue(score.getValue() >= before + fiveMinutes && score.getValue() <= System.currentTimeMillis() + fiveMinutes);
    }

    @Test
    void schedule_samePayloadAlwaysProducesSameMember() {
        // Lùi lịch (scheduleAt lần hai) phải cập nhật đúng member cũ, không tạo bản trùng
        RedisInfrastructureService redis = mock(RedisInfrastructureService.class);
        OrderCancelSchedule schedule = new OrderCancelSchedule(redis);

        schedule.scheduleAt(new TimeoutPayload("OKS-SGN-1-abc", "202610", "t1", 2), 1L);
        schedule.scheduleAt(new TimeoutPayload("OKS-SGN-1-abc", "202610", "t1", 2), 2L);

        verify(redis).zAdd("order:cancel", raw, 1L);
        verify(redis).zAdd("order:cancel", raw, 2L);
    }

    @Test
    void schedule_parseIsInverseOfStoredMember() {
        OrderCancelSchedule schedule = new OrderCancelSchedule(mock(RedisInfrastructureService.class));

        TimeoutPayload payload = schedule.parse(raw);

        assertEquals("OKS-SGN-1-abc", payload.getOrderNumber());
        assertEquals("202610", payload.getYearMonth());
        assertEquals("t1", payload.getTicketId());
        assertEquals(2, payload.getQuantity());
    }

    @Test
    void schedule_pollExpired_readsUpToNow() {
        RedisInfrastructureService redis = mock(RedisInfrastructureService.class);
        when(redis.zRangeByScore(eq("order:cancel"), eq(0L), anyLong(), eq(100))).thenReturn(Set.of(raw));

        List<String> expired = new OrderCancelSchedule(redis).pollExpired(100);

        assertEquals(List.of(raw), expired);
    }

    // ---------------- OrderTimeoutWorker ----------------

    private OrderCancelSchedule scheduleWithOneExpired(boolean claimed) {
        OrderCancelSchedule schedule = mock(OrderCancelSchedule.class);
        when(schedule.pollExpired(anyInt())).thenReturn(List.of(raw));
        when(schedule.remove(raw)).thenReturn(claimed);
        when(schedule.parse(raw)).thenReturn(JSON.parseObject(raw, TimeoutPayload.class));
        return schedule;
    }

    @Test
    void worker_claimsExpiredOrderAndSendsCancelMessage() throws Exception {
        OrderCancelSchedule schedule = scheduleWithOneExpired(true);
        KafkaOrderProducer producer = mock(KafkaOrderProducer.class);

        new OrderTimeoutWorker(schedule, producer).pollAndDispatch();

        ArgumentCaptor<OrderCancelMQMessage> captor = ArgumentCaptor.forClass(OrderCancelMQMessage.class);
        verify(producer).sendCancelMessage(captor.capture());
        assertEquals("OKS-SGN-1-abc", captor.getValue().getOrderNumber());
        assertEquals("202610", captor.getValue().getYearMonth());
        assertEquals("t1", captor.getValue().getTicketId());
        assertEquals(2, captor.getValue().getQuantity());
    }

    @Test
    void worker_removesFromScheduleBeforeSending() throws Exception {
        OrderCancelSchedule schedule = scheduleWithOneExpired(true);
        KafkaOrderProducer producer = mock(KafkaOrderProducer.class);

        new OrderTimeoutWorker(schedule, producer).pollAndDispatch();

        var inOrder = inOrder(schedule, producer);
        inOrder.verify(schedule).remove(raw);
        inOrder.verify(producer).sendCancelMessage(any());
    }

    @Test
    void worker_memberClaimedByAnotherInstance_isSkipped() throws Exception {
        OrderCancelSchedule schedule = scheduleWithOneExpired(false);
        KafkaOrderProducer producer = mock(KafkaOrderProducer.class);

        new OrderTimeoutWorker(schedule, producer).pollAndDispatch();

        verify(producer, never()).sendCancelMessage(any());
    }

    @Test
    void worker_kafkaDown_putsOrderBackIntoScheduleForRetry() throws Exception {
        // Đơn đã bị xoá khỏi ZSET trước khi gửi: nếu gửi lỗi mà không đặt lại thì
        // đơn chỉ được huỷ khi job dự phòng quét tới (sau hơn 15 phút).
        OrderCancelSchedule schedule = scheduleWithOneExpired(true);
        KafkaOrderProducer producer = mock(KafkaOrderProducer.class);
        doThrow(new RuntimeException("kafka down")).when(producer).sendCancelMessage(any());

        new OrderTimeoutWorker(schedule, producer).pollAndDispatch();

        verify(schedule).scheduleAt(argThat(p -> "OKS-SGN-1-abc".equals(p.getOrderNumber())), anyLong());
    }

    @Test
    void worker_oneBadMemberDoesNotStopOthers() throws Exception {
        String other = JSON.toJSONString(new TimeoutPayload("OKS-SGN-2-def", "202610", "t2", 1));
        OrderCancelSchedule schedule = mock(OrderCancelSchedule.class);
        when(schedule.pollExpired(anyInt())).thenReturn(List.of("not-json", other));
        when(schedule.remove(anyString())).thenReturn(true);
        when(schedule.parse("not-json")).thenThrow(new RuntimeException("bad json"));
        when(schedule.parse(other)).thenReturn(JSON.parseObject(other, TimeoutPayload.class));
        KafkaOrderProducer producer = mock(KafkaOrderProducer.class);

        new OrderTimeoutWorker(schedule, producer).pollAndDispatch();

        verify(producer).sendCancelMessage(argThat(m -> "OKS-SGN-2-def".equals(m.getOrderNumber())));
    }

    @Test
    void worker_nothingExpired_doesNothing() {
        OrderCancelSchedule schedule = mock(OrderCancelSchedule.class);
        KafkaOrderProducer producer = mock(KafkaOrderProducer.class);
        when(schedule.pollExpired(anyInt())).thenReturn(List.of());

        new OrderTimeoutWorker(schedule, producer).pollAndDispatch();

        verifyNoInteractions(producer);
        verify(schedule, never()).remove(any());
    }

    // ---------------- KafkaOrderCancelConsumer ----------------

    @Test
    void consumer_delegatesToCancelExpiredOrder() {
        TicketOrderAppService service = mock(TicketOrderAppService.class);

        new KafkaOrderCancelConsumer(service).processCancel(
                new OrderCancelMQMessage("OKS-SGN-1-abc", "202610", "t1", 2, LocalDateTime.now()));

        verify(service).cancelExpiredOrder("OKS-SGN-1-abc");
    }

    @Test
    void consumer_listensOnCancelTopic() throws Exception {
        // Nếu nghe nhầm topic đặt vé, consumer sẽ đọc message đặt vé như lệnh huỷ
        // và lệnh huỷ thật gửi lên order-cancel-topic sẽ không ai xử lý.
        KafkaListener listener = KafkaOrderCancelConsumer.class
                .getMethod("processCancel", OrderCancelMQMessage.class)
                .getAnnotation(KafkaListener.class);

        assertArrayEquals(new String[]{KafkaTopicConfig.ORDER_CANCEL_TOPIC}, listener.topics());
    }

    @Test
    void consumer_usesGroupDifferentFromPlaceOrderConsumer() throws Exception {
        KafkaListener listener = KafkaOrderCancelConsumer.class
                .getMethod("processCancel", OrderCancelMQMessage.class)
                .getAnnotation(KafkaListener.class);

        assertNotEquals("order-consumer-group", listener.groupId());
    }

    // ---------------- OrderTimeoutSafetyNetJob ----------------

    @Test
    void safetyNet_scansCurrentAndPreviousMonthTables() {
        OrderDeductionDomainService orders = mock(OrderDeductionDomainService.class);
        TicketOrderAppService service = mock(TicketOrderAppService.class);
        when(orders.findExpiredPendingOrderNumbers(anyString(), any(), anyInt())).thenReturn(List.of("A"), List.of("B"));

        new OrderTimeoutSafetyNetJob(orders, service).cancelStalePendingOrders();

        LocalDateTime now = LocalDateTime.now();
        String thisMonth = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        String lastMonth = now.minusMonths(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        verify(orders).findExpiredPendingOrderNumbers(eq(thisMonth), any(), anyInt());
        verify(orders).findExpiredPendingOrderNumbers(eq(lastMonth), any(), anyInt());
        verify(service).cancelExpiredOrder("A");
        verify(service).cancelExpiredOrder("B");
    }

    @Test
    void safetyNet_onlyPicksOrdersOlderThanTimeoutPlusGrace() {
        OrderDeductionDomainService orders = mock(OrderDeductionDomainService.class);
        when(orders.findExpiredPendingOrderNumbers(anyString(), any(), anyInt())).thenReturn(List.of());

        LocalDateTime before = LocalDateTime.now();
        new OrderTimeoutSafetyNetJob(orders, mock(TicketOrderAppService.class)).cancelStalePendingOrders();

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orders, atLeastOnce()).findExpiredPendingOrderNumbers(anyString(), cutoff.capture(), anyInt());

        // 5 phút hạn thanh toán + 10 phút chờ thêm
        LocalDateTime expected = before.minusMinutes(OrderCancelSchedule.PAYMENT_TIMEOUT_MINUTES + 10);
        assertTrue(Math.abs(java.time.Duration.between(expected, cutoff.getValue()).toSeconds()) < 5);
    }

    @Test
    void safetyNet_missingMonthTable_isSkipped() {
        OrderDeductionDomainService orders = mock(OrderDeductionDomainService.class);
        TicketOrderAppService service = mock(TicketOrderAppService.class);
        when(orders.findExpiredPendingOrderNumbers(anyString(), any(), anyInt()))
                .thenThrow(new RuntimeException("table doesn't exist"))
                .thenReturn(List.of("B"));

        new OrderTimeoutSafetyNetJob(orders, service).cancelStalePendingOrders();

        verify(service).cancelExpiredOrder("B");
    }

    @Test
    void safetyNet_oneFailureDoesNotStopOthers() {
        OrderDeductionDomainService orders = mock(OrderDeductionDomainService.class);
        TicketOrderAppService service = mock(TicketOrderAppService.class);
        when(orders.findExpiredPendingOrderNumbers(anyString(), any(), anyInt())).thenReturn(List.of("A", "B"), List.of());
        when(service.cancelExpiredOrder("A")).thenThrow(new IllegalStateException("db"));

        new OrderTimeoutSafetyNetJob(orders, service).cancelStalePendingOrders();

        verify(service).cancelExpiredOrder("B");
    }
}
