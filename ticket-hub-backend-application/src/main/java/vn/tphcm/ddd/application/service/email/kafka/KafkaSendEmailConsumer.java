/*
 * @ (#) KafkaSendEmailConsumer.java       1.0     9/23/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.email.kafka;
/*
 * @author: Luong Tan Dat
 * @date: 9/23/2026
 */


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import vn.tphcm.ddd.infrastructure.config.kafka.KafkaTopicConfig;
import vn.tphcm.ddd.infrastructure.mq.email.EmailMQMessage;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "KAFKA-SEND-EMAIL-CONSUMER")
public class KafkaSendEmailConsumer {

    @KafkaListener(
            topics = KafkaTopicConfig.EMAIL_TOPIC,
            groupId = "email-consumer-group",
            concurrency = "5"
    )
    public void processSendEmail(EmailMQMessage message){

    }
}
