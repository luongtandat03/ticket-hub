/*
 * @ (#) KafkaOrderProducer.java       1.0     9/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.mq.order;
/*
 * @author: Luong Tan Dat
 * @date: 9/13/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import vn.tphcm.ddd.infrastructure.config.kafka.KafkaTopicConfig;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "KAFKA-ORDER-PRODUCER")
public class KafkaOrderProducer {
    private final KafkaTemplate<String, PlaceOrderMQMessage> kafkaTemplate;

    private final KafkaTemplate<String, OrderCancelMQMessage> kafkaOrderCancelTemplate;

    public void sendOrderMessage(PlaceOrderMQMessage placeOrderMQMessage) {
        CompletableFuture<SendResult<String, PlaceOrderMQMessage>> completableFuture = kafkaTemplate
                .send(KafkaTopicConfig.ORDER_PLACE_TOPIC, placeOrderMQMessage.getToken(), placeOrderMQMessage);

        completableFuture.whenComplete((result, execption) -> {
            if (execption != null) {
                log.error("KafkaOrderProducer.sendOrderMessage error to send token: {}", placeOrderMQMessage.getToken(), execption);
            } else {
                log.error("KafkaOrderProducer.sendOrderMessage success to send token: {}, partition: {}, offset: {}",
                        placeOrderMQMessage.getToken(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    public void sendAndAwaitAck(PlaceOrderMQMessage placeOrderMQMessage) throws Exception {
        kafkaTemplate.send(KafkaTopicConfig.ORDER_PLACE_TOPIC, placeOrderMQMessage)
                .get(5, TimeUnit.SECONDS);
    }

    public CompletableFuture<SendResult<String, PlaceOrderMQMessage>> sendAsync(PlaceOrderMQMessage message) {
        return kafkaTemplate.send(KafkaTopicConfig.ORDER_PLACE_TOPIC, message.getToken(), message);
    }

    public void sendCancelMessage(OrderCancelMQMessage message) throws Exception {
        kafkaOrderCancelTemplate.send(KafkaTopicConfig.ORDER_CANCEL_TOPIC, message)
                .get(5, TimeUnit.SECONDS);
    }

    public CompletableFuture<SendResult<String, OrderCancelMQMessage>> sendAsyncCancelMessage(OrderCancelMQMessage message) {
        return kafkaOrderCancelTemplate.send(KafkaTopicConfig.ORDER_CANCEL_TOPIC, message.getOrderNumber(), message);
    }
}
