package vn.tphcm.ddd.application.service.outbox;

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vn.tphcm.ddd.application.cronjob.OutboxPublisherJob;
import vn.tphcm.ddd.application.service.outbox.impl.EmailOutboxHandler;
import vn.tphcm.ddd.application.service.outbox.impl.PlaceOrderOutboxHandler;
import vn.tphcm.ddd.domain.model.OutboxEvent;
import vn.tphcm.ddd.domain.repository.OutboxEventRepository;
import vn.tphcm.ddd.infrastructure.mq.email.EmailMQMessage;
import vn.tphcm.ddd.infrastructure.mq.email.KafkaEmailProducer;
import vn.tphcm.ddd.infrastructure.mq.order.KafkaOrderProducer;
import vn.tphcm.ddd.infrastructure.mq.order.PlaceOrderMQMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OutboxTest {

    private OutboxEvent event(String id, String type, String payload) {
        return OutboxEvent.builder().id(id).eventType(type).aggregateId("agg-" + id).payload(payload).status(0).build();
    }

    // ---------------- EmailOutboxHandler ----------------

    @Test
    void emailHandler_handlesSendEmailEvents() {
        assertEquals("SEND_EMAIL", new EmailOutboxHandler(mock(KafkaEmailProducer.class)).eventType());
    }

    @Test
    void emailHandler_parsesPayloadAndSendsToKafka() throws Exception {
        KafkaEmailProducer producer = mock(KafkaEmailProducer.class);
        EmailOutboxHandler handler = new EmailOutboxHandler(producer);
        EmailMQMessage message = new EmailMQMessage("tok", "mid", "VERIFY_EMAIL", "a@x.com", "123456", Map.of(), 1L);

        handler.publish(event("1", "SEND_EMAIL", JSON.toJSONString(message)));

        ArgumentCaptor<EmailMQMessage> captor = ArgumentCaptor.forClass(EmailMQMessage.class);
        verify(producer).sendAndAwaitAck(captor.capture());
        assertEquals("a@x.com", captor.getValue().getRecipient());
        assertEquals("VERIFY_EMAIL", captor.getValue().getEventType());
    }

    @Test
    void emailHandler_kafkaFailure_mustPropagateSoEventIsNotMarkedPublished() throws Exception {
        // Handler hiện nuốt exception; khi đó OutboxPublisherJob vẫn gọi markPublished
        // và email bị mất. Test này mô tả hành vi đúng: lỗi phải được ném ra ngoài.
        KafkaEmailProducer producer = mock(KafkaEmailProducer.class);
        doThrow(new RuntimeException("kafka down")).when(producer).sendAndAwaitAck(any());
        EmailOutboxHandler handler = new EmailOutboxHandler(producer);

        EmailMQMessage message = new EmailMQMessage("tok", "mid", "PURCHASE", "a@x.com", null, Map.of(), 1L);

        assertThrows(RuntimeException.class,
                () -> handler.publish(event("1", "SEND_EMAIL", JSON.toJSONString(message))));
    }

    // ---------------- PlaceOrderOutboxHandler ----------------

    @Test
    void placeOrderHandler_handlesPlaceOrderEvents() {
        assertEquals("PLACE_ORDER", new PlaceOrderOutboxHandler(mock(KafkaOrderProducer.class)).eventType());
    }

    @Test
    void placeOrderHandler_parsesPayloadAndSendsToKafka() throws Exception {
        KafkaOrderProducer producer = mock(KafkaOrderProducer.class);
        PlaceOrderOutboxHandler handler = new PlaceOrderOutboxHandler(producer);
        PlaceOrderMQMessage message = new PlaceOrderMQMessage("tok", "t1", 2, "u1", 5000L, 1L);

        handler.publish(event("1", "PLACE_ORDER", JSON.toJSONString(message)));

        ArgumentCaptor<PlaceOrderMQMessage> captor = ArgumentCaptor.forClass(PlaceOrderMQMessage.class);
        verify(producer).sendAndAwaitAck(captor.capture());
        assertEquals("t1", captor.getValue().getTicketId());
        assertEquals(2, captor.getValue().getQuantity());
    }

    @Test
    void placeOrderHandler_kafkaFailure_mustPropagate() throws Exception {
        KafkaOrderProducer producer = mock(KafkaOrderProducer.class);
        doThrow(new RuntimeException("kafka down")).when(producer).sendAndAwaitAck(any());
        PlaceOrderOutboxHandler handler = new PlaceOrderOutboxHandler(producer);

        PlaceOrderMQMessage message = new PlaceOrderMQMessage("tok", "t1", 1, "u1", 1L, 1L);

        assertThrows(RuntimeException.class,
                () -> handler.publish(event("1", "PLACE_ORDER", JSON.toJSONString(message))));
    }

    // ---------------- OutboxPublisherJob ----------------

    private OutboxPublisherJob job(OutboxEventRepository repo, PlaceOrderOutboxHandler orders, EmailOutboxHandler emails) {
        return new OutboxPublisherJob(repo, mock(KafkaOrderProducer.class), orders, emails);
    }

    private static String orderPayload(String token) {
        return JSON.toJSONString(new PlaceOrderMQMessage(token, "t1", 1, "u1", 5000L, 1L));
    }

    @Test
    void job_publishesPendingOrderEventsAsOneBatch() {
        OutboxEventRepository repo = mock(OutboxEventRepository.class);
        KafkaOrderProducer producer = mock(KafkaOrderProducer.class);
        PlaceOrderOutboxHandler orders = mock(PlaceOrderOutboxHandler.class);
        EmailOutboxHandler emails = mock(EmailOutboxHandler.class);
        when(orders.eventType()).thenReturn("PLACE_ORDER");
        when(repo.findPendingBatch("PLACE_ORDER", 500)).thenReturn(List.of(
                event("1", "PLACE_ORDER", orderPayload("a")), event("2", "PLACE_ORDER", orderPayload("b"))));
        when(producer.sendAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        new OutboxPublisherJob(repo, producer, orders, emails).publishOrders();

        // Gửi bất đồng bộ cả lô rồi đánh dấu một lần, không gửi/ghi DB từng event
        verify(producer, times(2)).sendAsync(any());
        verify(repo).markPublishedBatch(eq(List.of("1", "2")), any(LocalDateTime.class));
        verify(repo, never()).markPublished(any(), any());
        verifyNoInteractions(emails);
    }

    @Test
    void job_publishesEmailEventsWithEmailHandler() {
        OutboxEventRepository repo = mock(OutboxEventRepository.class);
        PlaceOrderOutboxHandler orders = mock(PlaceOrderOutboxHandler.class);
        EmailOutboxHandler emails = mock(EmailOutboxHandler.class);
        when(emails.eventType()).thenReturn("SEND_EMAIL");
        OutboxEvent e = event("1", "SEND_EMAIL", "{}");
        when(repo.findPendingBatch("SEND_EMAIL", 500)).thenReturn(List.of(e));

        job(repo, orders, emails).publishEmails();

        verify(emails).publish(e);
        verify(repo).markPublished(eq("1"), any(LocalDateTime.class));
    }

    @Test
    void job_noPendingEvents_doesNothing() {
        OutboxEventRepository repo = mock(OutboxEventRepository.class);
        PlaceOrderOutboxHandler orders = mock(PlaceOrderOutboxHandler.class);
        when(orders.eventType()).thenReturn("PLACE_ORDER");
        when(repo.findPendingBatch(anyString(), anyInt())).thenReturn(List.of());

        job(repo, orders, mock(EmailOutboxHandler.class)).publishOrders();

        verify(repo, never()).markPublished(any(), any());
        verify(repo, never()).markPublishedBatch(any(), any());
    }

    @Test
    void job_kafkaFailure_eventStaysPendingOthersMarkedPublished() {
        OutboxEventRepository repo = mock(OutboxEventRepository.class);
        KafkaOrderProducer producer = mock(KafkaOrderProducer.class);
        PlaceOrderOutboxHandler orders = mock(PlaceOrderOutboxHandler.class);
        when(orders.eventType()).thenReturn("PLACE_ORDER");
        when(repo.findPendingBatch("PLACE_ORDER", 500)).thenReturn(List.of(
                event("1", "PLACE_ORDER", orderPayload("bad")), event("2", "PLACE_ORDER", orderPayload("good"))));
        when(producer.sendAsync(argThat(m -> m != null && "bad".equals(m.getToken()))))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("kafka")));
        when(producer.sendAsync(argThat(m -> m != null && "good".equals(m.getToken()))))
                .thenReturn(CompletableFuture.completedFuture(null));

        new OutboxPublisherJob(repo, producer, orders, mock(EmailOutboxHandler.class)).publishOrders();

        // Event lỗi giữ trạng thái pending để vòng sau gửi lại
        verify(repo).markPublishedBatch(eq(List.of("2")), any(LocalDateTime.class));
        verify(repo, never()).markPublished(any(), any());
    }
}
