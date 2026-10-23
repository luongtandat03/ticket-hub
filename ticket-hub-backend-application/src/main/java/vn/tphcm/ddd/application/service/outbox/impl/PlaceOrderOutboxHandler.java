/*
 * @ (#) PlaceOrderOutboxHandler.java       1.0     10/3/2026
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
import vn.tphcm.ddd.infrastructure.mq.order.KafkaOrderProducer;
import vn.tphcm.ddd.infrastructure.mq.order.PlaceOrderMQMessage;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlaceOrderOutboxHandler implements OutboxHandler {
    private final KafkaOrderProducer kafkaOrderProducer;

    @Override
    public String eventType() {
        return "PLACE_ORDER";
    }

    @Override
    public void publish(OutboxEvent event) {
        try {
            PlaceOrderMQMessage message = JSON.parseObject(event.getPayload(), PlaceOrderMQMessage.class);
            kafkaOrderProducer.sendAndAwaitAck(message);
        } catch (Exception e) {
            log.error("Outbox publisher [row-by-row]: publish event place order failed eventId: {}", event.getId(), e);
            throw new IllegalStateException("Publish place order event failed: " + event.getId(), e);
        }
    }
}
