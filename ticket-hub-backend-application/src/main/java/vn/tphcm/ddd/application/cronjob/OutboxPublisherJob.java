/*
 * @ (#) OutboxPublisherJob.java       1.0     9/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.cronjob;
/*
 * @author: Luong Tan Dat
 * @date: 9/13/2026
 */

import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.tphcm.ddd.domain.model.OutboxEvent;
import vn.tphcm.ddd.domain.repository.OutboxEventRepository;
import vn.tphcm.ddd.infrastructure.mq.order.KafkaOrderProducer;
import vn.tphcm.ddd.infrastructure.mq.order.PlaceOrderMQMessage;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "OUTBOX-PUBLISHER-JOB")
public class OutboxPublisherJob {
    private static final int BATCH_SIZE = 500;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaOrderProducer kafkaOrderProducer;

    @Scheduled(fixedRate = 1000) // Run sau mỗi giây bất kể lần trước xong hay không
    public void publish() {
        publishRowByRow();
    }

    /**
     * Row-by-row
     * <p>
     * + Failed window smail
     * + Easy debug
     * - DB round trip big
     * - Low Throughput
     */
    private void publishRowByRow() {
        List<OutboxEvent> events = outboxEventRepository.findPendingBatch(BATCH_SIZE);

        log.debug("publishRowByRow events size: {}", events.size());

        for (OutboxEvent event : events) {
            try {
                PlaceOrderMQMessage message = JSON.parseObject(event.getPayload(), PlaceOrderMQMessage.class);
                kafkaOrderProducer.sendAndAwaitAck(message);
                outboxEventRepository.markPublished(event.getId(), LocalDateTime.now(ZoneId.systemDefault()));

                log.debug("OutboxPublisher [row-by-row]: published eventId={}, token={}",
                        event.getId(), event.getAggregateId());

            } catch (Exception e) {
                log.error("Outbox publisher [row-by-row]: publish event failed eventId: {}", event.getId(), e);
            }
        }
    }

    /**
     * Batch
     * Send all messages asynchronously, collect results, and perform a single bulk update.
     * + Fewer DB round trip
     * + Higher Throughout
     * - Failure window
     * - Difficult debug
     */
    private void publishBatch() {
        List<OutboxEvent> events = outboxEventRepository.findPendingBatch(BATCH_SIZE);

        if (events.isEmpty()) return;

        log.debug("publishBatch events size: {}", events.size());

        List<String> eventIds = new ArrayList<>();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (OutboxEvent event : events) {
            try {
                PlaceOrderMQMessage message = JSON.parseObject(event.getPayload(), PlaceOrderMQMessage.class);
                CompletableFuture<Void> future = kafkaOrderProducer.sendAsync(message).thenAccept(result -> {

                });

                eventIds.add(event.getId());
                futures.add(future);
            } catch (Exception e) {
                log.error("OutboxPublisher [batch]: parse failed eventId={}", event.getId(), e);
            }
        }

        List<String> successIds = new ArrayList<>();

        for (int i = 0; i < futures.size(); i++) {
            try {
                futures.get(i).get();
                successIds.add(eventIds.get(i));
            } catch (Exception e) {
                log.error("OutboxPublisher [batch]: kafka send failed eventId={}, will retry next cycle",
                        eventIds.get(i), e);
            }
        }

        if (!successIds.isEmpty()) {
            outboxEventRepository.markPublishedBatch(successIds, LocalDateTime.now(ZoneId.systemDefault()));
            log.debug("OutboxPublisher [batch]: marked {} events as PUBLISHED", successIds.size());
        }
    }

}
