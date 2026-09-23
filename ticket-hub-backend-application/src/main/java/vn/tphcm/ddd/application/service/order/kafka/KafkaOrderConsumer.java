/*
 * @ (#) KafkaOrderConsumer.java       1.0     9/15/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.order.kafka;
/*
 * @author: Luong Tan Dat
 * @date: 9/15/2026
 */

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import vn.tphcm.ddd.application.schedule.OrderCancelSchedule;
import vn.tphcm.ddd.application.service.order.cache.StockOrderCacheService;
import vn.tphcm.ddd.domain.model.TicketOrder;
import vn.tphcm.ddd.domain.repository.IdempotencyKeyRepository;
import vn.tphcm.ddd.domain.repository.OrderQueueRepository;
import vn.tphcm.ddd.domain.service.OrderDeductionDomainService;
import vn.tphcm.ddd.domain.service.TicketOrderDomainService;
import vn.tphcm.ddd.infrastructure.mq.order.PlaceOrderMQMessage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "KAFKA-ORDER-CONSUMER")
public class KafkaOrderConsumer {
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final TicketOrderDomainService ticketOrderDomainService;
    private final StockOrderCacheService stockOrderCacheService;
    private final OrderQueueRepository orderQueueRepository;
    private final OrderDeductionDomainService orderDeductionDomainService;
    private final OrderCancelSchedule orderCancelSchedule;

    @KafkaListener(
            topics = "order-place-topic",
            groupId = "order-consumer-group",
            concurrency = "10"
    )
    @Transactional(rollbackOn = Exception.class)
    public void processOrder(PlaceOrderMQMessage message) {
        String token = message.getToken();
        String ticketId = message.getTicketId();
        int quantity = message.getQuantity();

        boolean isNew = idempotencyKeyRepository.tryInsert(token, LocalDateTime.now(ZoneId.systemDefault()).plusHours(24));

        if (!isNew) {
            log.info("[IDENPOTENCY] Duplicate skip token = {}", token);
            return;
        }

        log.info("[MQ] Processing token = {} ticketId={} qty={}", token, ticketId, quantity);

        boolean stockDescreased = ticketOrderDomainService.decreaseStock(ticketId, quantity);

        if (!stockDescreased) {
            stockOrderCacheService.increaseStockCacheByLUA(ticketId, quantity);
            orderQueueRepository.updateStatus(token, 2, null, "Sold out");
            log.warn("[MQ] Out of stock token={}", token);
            return;
        }

        String orderNumber = "MQ-" + message.getUserId().replace("-", "") + "-" + System.currentTimeMillis();
        String nTable = LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyyMM"));

        TicketOrder ticketOrder = TicketOrder.builder()
                .ticketId(ticketId)
                .quantity(quantity)
                .userId(message.getUserId())
                .orderNumber(orderNumber)
                .totalAmount(new BigDecimal(message.getUnitPrice() * quantity))
                .orderStatus(0)
                .terminalId("MQ-SGN")
                .orderNotes("MQ Order -> Pending")
                .build();

        orderDeductionDomainService.insertOrder(nTable, ticketOrder);
        orderQueueRepository.updateStatus(token, 1, orderNumber, null);

        orderCancelSchedule.scheduleTimeout(orderNumber, nTable, ticketId, quantity);

        log.info("[MQ] Success token = {} orderNumber={}", token, orderNumber);
    }
}
