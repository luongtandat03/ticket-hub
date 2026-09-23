/*
 * @ (#) KafkaEmailProducer.java       1.0     9/23/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.mq.email;
/*
 * @author: Luong Tan Dat
 * @date: 9/23/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import vn.tphcm.ddd.infrastructure.config.kafka.KafkaTopicConfig;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "KAFKA-EMAIL-PRODUCER")
public class KafkaEmailProducer {
    private final KafkaTemplate<String, EmailMQMessage> kafkaTemplate;

    public void sendAndAwaitAck(EmailMQMessage message) throws Exception {
        kafkaTemplate.send(KafkaTopicConfig.ORDER_PLACE_TOPIC, message)
                .get(5, TimeUnit.SECONDS);
    }
}
