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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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
    private final TransactionTemplate transactionTemplate;

    private static final String TERMINAL = "OKS-SGN";
    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyyMM");
    private static final AtomicLong LAST_ORDER_MILLIS = new AtomicLong();

//    @KafkaListener(
//            topics = "order-place-topic",
//            groupId = "order-consumer-group",
//            concurrency = "10"
//    )
//    @Transactional(rollbackOn = Exception.class)
//    public void processOrder(PlaceOrderMQMessage message) {
//        String token = message.getToken();
//        String ticketId = message.getTicketId();
//        int quantity = message.getQuantity();
//
//        boolean isNew = idempotencyKeyRepository.tryInsert(token, LocalDateTime.now(ZoneId.systemDefault()).plusHours(24));
//
//        if (!isNew) {
//            log.info("[IDENPOTENCY] Duplicate skip token = {}", token);
//            return;
//        }
//
//        log.info("[MQ] Processing token = {} ticketId={} qty={}", token, ticketId, quantity);
//
//        boolean stockDescreased = ticketOrderDomainService.decreaseStock(ticketId, quantity);
//
//        if (!stockDescreased) {
//            stockOrderCacheService.increaseStockCacheByLUA(ticketId, quantity);
//            orderQueueRepository.updateStatus(token, 2, null, "Sold out");
//            log.warn("[MQ] Out of stock token={}", token);
//            return;
//        }
//
//        String orderNumber = genOrderNumber(message.getUserId());
//        String nTable = LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyyMM"));
//
//        TicketOrder ticketOrder = TicketOrder.builder()
//                .ticketId(ticketId)
//                .quantity(quantity)
//                .userId(message.getUserId())
//                .orderNumber(orderNumber)
//                .totalAmount(new BigDecimal(message.getUnitPrice() * quantity))
//                .orderStatus(0)
//                .terminalId("MQ-SGN")
//                .orderNotes("MQ Order -> Pending")
//                .build();
//
//        orderDeductionDomainService.insertOrder(nTable, ticketOrder);
//
//        orderQueueRepository.updateStatus(token, 1, orderNumber, null);
//
//        orderCancelSchedule.scheduleTimeout(orderNumber, nTable, ticketId, quantity);
//
//        log.info("[MQ] Success token = {} orderNumber={}", token, orderNumber);
//    }

    private record PlacedOrder(String nTable, TicketOrder ticketOrder) {
    }

    @KafkaListener(
            topics = "order-place-topic",
            groupId = "order-consumer-group",
            concurrency = "10",
            batch = "true"
    )
    public void processOrders(List<PlaceOrderMQMessage> messages) {
        messages.stream()
                .collect(Collectors.groupingBy(PlaceOrderMQMessage::getTicketId, LinkedHashMap::new, Collectors.toList()))
                .forEach(this::processTicketPatch);
    }

    private void processTicketPatch(String ticketId, List<PlaceOrderMQMessage> messages) {
        List<PlacedOrder> placed = transactionTemplate.execute(status -> {
            List<PlacedOrder> result = new ArrayList<>();

            for (PlaceOrderMQMessage message : messages) {
                PlacedOrder order = newOrder(message);

                orderDeductionDomainService.insertOrder(order.nTable(), order.ticketOrder());

                if (!idempotencyKeyRepository.tryInsert(message.getToken(), expiresAt())) {
                    status.setRollbackOnly();
                    return null;
                }

                orderQueueRepository.updateStatus(message.getToken(), 1, order.ticketOrder().getOrderNumber(), null);

                result.add(order);
            }

            int total = messages.stream().mapToInt(PlaceOrderMQMessage::getQuantity).sum();

            if (!ticketOrderDomainService.decreaseStock(ticketId, total)) {
                status.setRollbackOnly();
                return null;
            }

            return result;
        });

        if (placed == null) {
            messages.forEach(this::processOrder);
            return;
        }

        placed.forEach(this::scheduleTimeout);
        log.info("[MQ] Batch done ticketId={} orders={}", ticketId, placed.size());

    }

    private void processOrder(PlaceOrderMQMessage message) {
        String token = message.getToken();
        PlacedOrder order = newOrder(message);

        Boolean placed = transactionTemplate.execute(status -> {
            orderDeductionDomainService.insertOrder(order.nTable(), order.ticketOrder());

            if (!idempotencyKeyRepository.tryInsert(token, expiresAt())) {
                status.setRollbackOnly();
                return null;
            }

            orderQueueRepository.updateStatus(token, 1, order.ticketOrder().getOrderNumber(), null);

            if (!ticketOrderDomainService.decreaseStock(message.getTicketId(), message.getQuantity())) {
                status.setRollbackOnly();
                return false;
            }

            return true;
        });

        if (placed == null) {
            log.info("[IDENPOTENCY] Duplicate skip token = {}", token);
            return;
        }

        if (!placed) {
            markSoldOut(message);
            return;
        }

        scheduleTimeout(order);
        log.info("[MQ] Success token = {} orderNumber={}", token, order.ticketOrder().getOrderNumber());
    }

    private void markSoldOut(PlaceOrderMQMessage message) {
        boolean marked = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            if (!idempotencyKeyRepository.tryInsert(message.getToken(), expiresAt())) {
                return false;
            }

            orderQueueRepository.updateStatus(message.getToken(), 2, null, "Sold out");
            return true;
        }));

        if (marked) {
            stockOrderCacheService.increaseStockCacheByLUA(message.getTicketId(), message.getQuantity());
            log.warn("[MQ] Out of stock token={}", message.getToken());
        }
    }

    private void scheduleTimeout(PlacedOrder placedOrder) {
        TicketOrder ticketOrder = placedOrder.ticketOrder();

        try {
            orderCancelSchedule.scheduleTimeout(ticketOrder.getOrderNumber(), placedOrder.nTable(),
                    ticketOrder.getTicketId(), ticketOrder.getQuantity());
        } catch (Exception e) {
            log.error("[MQ] scheduleTimeout failed, safety-net job will cancel {}", ticketOrder.getOrderNumber(), e);
        }
    }

    private PlacedOrder newOrder(PlaceOrderMQMessage message) {
        TicketOrder order = TicketOrder.builder()
                .ticketId(message.getTicketId())
                .quantity(message.getQuantity())
                .userId(message.getUserId())
                .orderNumber(genOrderNumber(message.getUserId()))
                .totalAmount(new BigDecimal(message.getUnitPrice() * message.getQuantity()))
                .orderStatus(0)
                .terminalId("MQ-SGN")
                .orderNotes("MQ Order -> Pending")
                .build();

        return new PlacedOrder(LocalDateTime.now(ZoneId.systemDefault()).format(YEAR_MONTH), order);
    }

    private static LocalDateTime expiresAt() {
        return LocalDateTime.now(ZoneId.systemDefault()).plusHours(24);
    }

    private String genOrderNumber(String userId) {
        long millis = LAST_ORDER_MILLIS.updateAndGet(last -> Math.max(System.currentTimeMillis(), last + 1));
        return TERMINAL + "-" + millis + "-" + userId.replace("-", "");
    }
}
