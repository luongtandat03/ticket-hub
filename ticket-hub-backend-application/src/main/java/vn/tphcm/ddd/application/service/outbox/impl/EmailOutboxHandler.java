/*
 * @ (#) EmailOutboxHandler.java       1.0     10/3/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.outbox.impl;
/*
 * @author: Luong Tan Dat
 * @date: 10/3/2026
 */

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.tphcm.ddd.application.service.outbox.OutboxHandler;
import vn.tphcm.ddd.domain.model.OutboxEvent;
import vn.tphcm.ddd.infrastructure.mq.email.EmailMQMessage;
import vn.tphcm.ddd.infrastructure.mq.email.KafkaEmailProducer;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailOutboxHandler implements OutboxHandler {
    private final KafkaEmailProducer kafkaEmailProducer;

    @Override
    public String eventType() {
        return "SEND_EMAIL";
    }

    @Override
    public void publish(OutboxEvent event) {
        try {
            EmailMQMessage message = JSON.parseObject(event.getPayload(), EmailMQMessage.class);
            kafkaEmailProducer.sendAndAwaitAck(message);
        } catch (Exception e) {
            log.error("Outbox publisher [row-by-row]: publish event send email failed eventId: {}", event.getId(), e);
            throw new IllegalStateException("Publish email event failed: " + event.getId(), e);
        }
    }
}
