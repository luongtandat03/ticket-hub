/*
 * @ (#) KafkaTopicConfig.java       1.0     9/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.config.kafka;
/*
 * @author: Luong Tan Dat
 * @date: 9/13/2026
 */

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    public static final String ORDER_PLACE_TOPIC = "order-place-topic";
    public static final String ORDER_CANCEL_TOPIC = "order-cancel-topic";
    public static final String EMAIL_TOPIC = "email-topic";

    @Bean
    public NewTopic orderPlaceTopic() {
        return TopicBuilder.name(ORDER_PLACE_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCancelTopic() {
        return TopicBuilder.name(ORDER_CANCEL_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic emailTopic() {
        return TopicBuilder.name(EMAIL_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
