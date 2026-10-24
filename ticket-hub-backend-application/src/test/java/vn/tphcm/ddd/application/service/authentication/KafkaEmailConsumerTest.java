package vn.tphcm.ddd.application.service.authentication;

import org.junit.jupiter.api.Test;
import vn.tphcm.ddd.application.service.authentication.kafka.KafkaEmailConsumer;
import vn.tphcm.ddd.application.util.EmailSenderUtil;
import vn.tphcm.ddd.domain.repository.IdempotencyKeyRepository;
import vn.tphcm.ddd.infrastructure.mq.email.EmailMQMessage;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class KafkaEmailConsumerTest {
    private final EmailSenderUtil sender = mock(EmailSenderUtil.class);
    private final IdempotencyKeyRepository idempotency = mock(IdempotencyKeyRepository.class);
    private final KafkaEmailConsumer consumer = new KafkaEmailConsumer(sender, idempotency);

    private EmailMQMessage message() {
        return new EmailMQMessage("tok-1", "mid-1", "VERIFY_EMAIL", "a@x.com", "123456", Map.of(), 1L);
    }

    @Test
    void newMessage_isSent() {
        when(idempotency.tryInsert(eq("tok-1"), any(LocalDateTime.class))).thenReturn(true);

        consumer.processEmail(message());

        verify(sender).sendTextEmail(any(EmailMQMessage.class));
    }

    @Test
    void duplicateMessage_isSkipped() {
        when(idempotency.tryInsert(eq("tok-1"), any(LocalDateTime.class))).thenReturn(false);

        consumer.processEmail(message());

        verify(sender, never()).sendTextEmail(any());
    }

    @Test
    void sendFailure_isRethrownSoKafkaCanRetry() {
        when(idempotency.tryInsert(any(), any())).thenReturn(true);
        doThrow(new IllegalStateException("smtp down")).when(sender).sendTextEmail(any());

        assertThrows(IllegalStateException.class, () -> consumer.processEmail(message()));
    }
}
