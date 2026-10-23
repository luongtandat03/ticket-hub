/*
 * @ (#) KafkaEmailConsumer.java       1.0     9/30/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.authentication.kafka;
/*
 * @author: Luong Tan Dat
 * @date: 9/30/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import vn.tphcm.ddd.application.util.EmailSenderUtil;
import vn.tphcm.ddd.domain.repository.IdempotencyKeyRepository;
import vn.tphcm.ddd.infrastructure.mq.email.EmailMQMessage;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "KAFKA-EMAIL-CONSUMER")
public class KafkaEmailConsumer {
    private final EmailSenderUtil emailSenderUtil;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @KafkaListener(
            topics = "email-topic",
            groupId = "email-consumer-group",
            concurrency = "5"
    )
    public void processEmail(EmailMQMessage message) {
        try {
            String token = message.getToken();
            boolean isNew = idempotencyKeyRepository.tryInsert(token, LocalDateTime.now(ZoneId.systemDefault()).plusHours(24));

            if (!isNew) {
                log.info("[IDEMPOTENCY] Duplicate skip token = {}", token);
                return;
            }

            emailSenderUtil.sendTextEmail(message);
        } catch (Exception e) {
            log.error("Send email failed messageId={}", message.getMessageId(), e);
            throw e;
        }
    }
}
