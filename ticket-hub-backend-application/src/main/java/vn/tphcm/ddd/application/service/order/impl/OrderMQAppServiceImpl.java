/*
 * @ (#) OrderMQAppServiceImpl.java       1.0     9/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.order.impl;
/*
 * @author: Luong Tan Dat
 * @date: 9/13/2026
 */

import com.alibaba.fastjson.JSON;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import vn.tphcm.ddd.application.service.order.OrderMQAppService;
import vn.tphcm.ddd.application.service.order.cache.StockOrderCacheService;
import vn.tphcm.ddd.domain.model.OrderQueue;
import vn.tphcm.ddd.domain.model.OutboxEvent;
import vn.tphcm.ddd.domain.repository.OrderQueueRepository;
import vn.tphcm.ddd.domain.repository.OutboxEventRepository;
import vn.tphcm.ddd.infrastructure.mq.order.PlaceOrderMQMessage;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ORDER-MQ-APP-SERVICE")
public class OrderMQAppServiceImpl implements OrderMQAppService {
    private final StockOrderCacheService stockOrderCacheService;

    private final OutboxEventRepository outboxEventRepository;

    private final OrderQueueRepository orderQueueRepository;

    private final TransactionTemplate transactionTemplate;

    private static final String NOT_FOUND = "TICKET_NOT_FOUND";
    private static final String SOLD_OUT = "OUT_OF_STOCK";
    private static final String PRICE_NOT_FOUND = "PRICE_NOT_FOUND";
    private static final String ERROR_SERVER = "INTERNAL_ERROR";

    @Override
    public OrderQueue placeOrderMQ(String ticketId, int quantity) {
        int redisResult = stockOrderCacheService.decreaseStockCacheByLua(ticketId, quantity);

        if (redisResult == -1) {
            log.info("placeOrderMQ: cache miss for ticketId={}, warming up...", ticketId);
            boolean warned = stockOrderCacheService.addStockAvailableToCache(ticketId);
            if (!warned) {
                return failedQueue(NOT_FOUND, "Not found event");
            }

            redisResult = stockOrderCacheService.decreaseStockCacheByLua(ticketId, quantity);
        }

        if (redisResult == 0) {
            log.info("placeOrderMQ: Redis OOS for ticketId={}", ticketId);
            return failedQueue(SOLD_OUT, "Sold out");
        }

        long unitPrice = stockOrderCacheService.getEffectivePrice(ticketId);

        if (unitPrice <= 0) {
            stockOrderCacheService.increaseStockCacheByLUA(ticketId, quantity);
            log.warn("placeOrderCAS: price not found  for ticketId = {}, rollback redis", ticketId);
            return failedQueue(PRICE_NOT_FOUND, "Do not confirm price");
        }

        String userId = UuidCreator.getTimeOrderedEpoch().toString();

        String token = "MQ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        try {
            OrderQueue queue = transactionTemplate.execute(txStatus -> {
                OrderQueue orderQueue = OrderQueue.builder()
                        .token(token)
                        .userId(userId)
                        .ticketId(ticketId)
                        .quantity(quantity)
                        .status(0)
                        .createdAt(LocalDateTime.now(ZoneId.systemDefault()))
                        .updatedAt(LocalDateTime.now(ZoneId.systemDefault()))
                        .build();

                orderQueueRepository.save(orderQueue);

                PlaceOrderMQMessage message = new PlaceOrderMQMessage(
                        token, ticketId, quantity, userId, unitPrice, System.currentTimeMillis()
                );
                OutboxEvent event = OutboxEvent.builder()
                        .aggregateId(token)
                        .eventType("ORDER_PLACED")
                        .payload(JSON.toJSONString(message))
                        .status(0)
                        .createdAt(LocalDateTime.now(ZoneId.systemDefault()))
                        .build();

                outboxEventRepository.save(event);

                return orderQueue;
            });

            log.info("placeOrderMQ: queued token={} ticketId={}", token, ticketId);

            return queue;
        } catch (Exception e) {
            stockOrderCacheService.increaseStockCacheByLUA(ticketId, quantity);
            log.error("placeOrderMQ: transaction failed, compensated Redis for ticketId={}", ticketId, e);
            return failedQueue(ERROR_SERVER, "Server error, please try again later");
        }
    }

    @Override
    public OrderQueue getOrderStatus(String token) {
        return orderQueueRepository.findByToken(token).orElse(null);
    }

    private OrderQueue failedQueue(String code, String msg) {
        return OrderQueue.builder()
                .status(2)
                .message(code + ": " + msg)
                .build();
    }
}
