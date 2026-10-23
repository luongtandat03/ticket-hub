/*
 * @ (#) KafkaOrderCancelConsumer.java       1.0     10/14/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.order.kafka;
/*
 * @author: Luong Tan Dat
 * @date: 10/14/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.tphcm.ddd.application.service.order.TicketOrderAppService;
import vn.tphcm.ddd.infrastructure.config.kafka.KafkaTopicConfig;
import vn.tphcm.ddd.infrastructure.mq.order.OrderCancelMQMessage;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "KAFKA-ORDER-CANCEL")
public class KafkaOrderCancelConsumer {
    private final TicketOrderAppService ticketOrderAppService;

    @KafkaListener(
            topics = KafkaTopicConfig.ORDER_CANCEL_TOPIC,
            groupId = "order-cancel-group",
            concurrency = "3"
    )
    @Transactional(rollbackFor = Exception.class)
    public void processCancel(OrderCancelMQMessage message) {
        log.info("[MQ-CANCEL] orderNumber = {}", message.getOrderNumber());

        boolean isCancelled = ticketOrderAppService.cancelExpiredOrder(message.getOrderNumber());

        log.info("[MQ-CANCEL] orderNumber = {}, cancelled = {}", message.getOrderNumber(), isCancelled);
    }
}
