/*
 * @ (#) TicketOrderAppServiceImpl.java       1.0     9/3/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.order.impl;
/*
 * @author: Luong Tan Dat
 * @date: 9/3/2026
 */

import jakarta.persistence.LockTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.PessimisticLockException;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.tphcm.ddd.application.dto.PageResponse;
import vn.tphcm.ddd.application.dto.PlaceOrderResponse;
import vn.tphcm.ddd.application.dto.TicketOrderResponse;
import vn.tphcm.ddd.application.schedule.OrderCancelSchedule;
import vn.tphcm.ddd.application.service.order.TicketOrderAppService;
import vn.tphcm.ddd.application.service.order.cache.StockOrderCacheService;
import vn.tphcm.ddd.domain.model.TicketOrder;
import vn.tphcm.ddd.domain.service.OrderDeductionDomainService;
import vn.tphcm.ddd.domain.service.TicketOrderDomainService;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RedisDistributedLocker;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RedisDistributedService;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j(topic = "TICKET-ORDER-APP-SERVICE")
@RequiredArgsConstructor
public class TicketOrderAppServiceImpl implements TicketOrderAppService {
    private final StockOrderCacheService stockOrderCacheService;
    private final TicketOrderDomainService ticketOrderDomainService;
    private final OrderDeductionDomainService orderDeductionDomainService;
    private final OrderCancelSchedule orderCancelScheduleService;

    private static final String TERMINAL = "OKS-SGN";
    private final RedisDistributedService redisDistributedService;

    private static final String FAILED_STOCK_TICKET = "TICKET_NOT_FOUND";
    private static final String OUT_STOCK_TICKET = "OUT_OF_STOCK";
    private static final String STOCK_CONFLICT = "STOCK_CONFLICT";
    private static final String PRICE_NOT_FOUND = "PRICE_NOT_FOUND";
    private static final String ERROR = "SERVER_ERROR";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean decreaseStockCAS(String ticketId, int quantity) {
        try {
            int oldStockAvailable = stockOrderCacheService.decreaseStockCacheByLUA(ticketId, quantity);

            if (oldStockAvailable == 0) {
                log.info("Case: oldStockAvailable is 0");
                return false;
            }
            log.info("oldStockAvailable is {}", oldStockAvailable);

            boolean isDecreaseStockSuccess = ticketOrderDomainService.decreaseStockCAS(ticketId, oldStockAvailable, quantity);

            if (isDecreaseStockSuccess) {
                LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());

                TicketOrder ticketOrder = createTicketOrder(ticketId, quantity, now);

                String nTable = LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyyMM"));
                orderDeductionDomainService.insertOrder(nTable, ticketOrder);
            }

            return true;
        } catch (PessimisticLockException e) {
            log.warn("Pessimistic Locking failed for ticketId={}", ticketId, e);
            return false;
        } catch (LockTimeoutException e) {
            log.error("Lock timeout while processing ticketId={}", ticketId, e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error when decreasing stock for ticketId={}", ticketId, e);
            return false;
        }
    }

    private static @NonNull TicketOrder createTicketOrder(String ticketId, int quantity, LocalDateTime now) {
        TicketOrder ticketOrder = new TicketOrder();

        ticketOrder.setTicketId(ticketId);
        ticketOrder.setQuantity(quantity);
        ticketOrder.setOrderStatus(1);
        ticketOrder.setTotalAmount(new BigDecimal(quantity * 5000));
        ticketOrder.setTerminalId(TERMINAL);
        ticketOrder.setOrderDate(now);
        ticketOrder.setOrderNotes("Order -> Pending");
        ticketOrder.setCreatedAt(now);
        ticketOrder.setUpdatedAt(now);
        return ticketOrder;
    }

    @Override
    public List<TicketOrderResponse> findAll(String yearMonth) {
        List<Object[]> results = orderDeductionDomainService.findAll(yearMonth);

        if (results != null) {
            return getTicketOrderResponses(results);
        }
        return Collections.emptyList();
    }


    @Override
    public TicketOrderResponse findByOrderNumber(String orderNumber) {
        String yearMonth = extractYearMonthFromOrderNumber(orderNumber);
        Object[] result = orderDeductionDomainService.findByOrderNumber(yearMonth, orderNumber);
        if (result != null) {
            return new TicketOrderResponse(
                    (String) result[0],
                    (String) result[1],
                    (String) result[2],
                    (String) result[3],
                    (int) result[4],
                    (int) result[5],
                    (BigDecimal) result[6],
                    (String) result[7],
                    ((Timestamp) result[8]).toLocalDateTime(),
                    (String) result[9],
                    ((Timestamp) result[10]).toLocalDateTime(),
                    ((Timestamp) result[11]).toLocalDateTime()
            );
        }
        return null;
    }

    @Override
    public List<TicketOrderResponse> findByDateRange(String nTable, LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> results = orderDeductionDomainService.findByDateRange(nTable, startDate, endDate);
        if (results != null) {
            return getTicketOrderResponses(results);
        }

        return Collections.emptyList();
    }

    @Override
    @Transactional
    public boolean cancelOrder(String userId, String orderNumber) {
        log.info("Cancel order with userId: {}, orderNumber: {}", userId, orderNumber);

        String keyLock = genEventCancelKeyLock(orderNumber);
        RedisDistributedLocker locker = redisDistributedService.getRedisDistributedLock(keyLock);

        try {
            boolean isLocker = locker.tryLock(1, 5, TimeUnit.SECONDS);

            if (!isLocker) {
                log.warn("System is processing this order, please wait... {}", orderNumber);
                return false;
            }

            TicketOrderResponse response = findByOrderNumber(orderNumber);

            return handleTicketOrder(response, userId);

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            locker.unlock();
        }
    }

    @Override
    public PageResponse<TicketOrderResponse> findPage(String yearMonth, String lastId, int limit) {
        List<Object[]> results = orderDeductionDomainService.findPage(yearMonth, lastId, limit);

        List<TicketOrderResponse> items = getTicketOrderResponses(results);

        boolean hasMore = results.size() == limit;

        Long nextCursor = hasMore ? ((Number) results.getLast()[0]).longValue() : null;

        return new PageResponse<>(items, nextCursor, hasMore);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlaceOrderResponse placeOrderCAS(String ticketId, int quantity) {
        boolean isRedisDecremented = false;

        try {
            int redisResult = stockOrderCacheService.decreaseStockCacheByLua(ticketId, quantity);

            if (redisResult == -1) {
                log.info("placeOrderCAS: cache miss for ticketId = {}, warning up ...", ticketId);
                boolean warned = stockOrderCacheService.addStockAvailableToCache(ticketId);
                if (!warned) {
                    return PlaceOrderResponse.fail(FAILED_STOCK_TICKET, "Not found event");
                }
                redisResult = stockOrderCacheService.decreaseStockCacheByLua(ticketId, quantity);
            }

            if (redisResult == 0) {
                log.info("placeOrderCAS: cache miss for ticketId = {}", ticketId);
                return PlaceOrderResponse.fail(OUT_STOCK_TICKET, "Out stock, please try again later");
            }

            isRedisDecremented = true;

            boolean isDecreaseStockSuccess = ticketOrderDomainService.decreaseStock(ticketId, quantity);
            if (!isDecreaseStockSuccess) {
                stockOrderCacheService.increaseStockCacheByLUA(ticketId, quantity);
                log.warn("placeOrderCAS: price not found for ticketId = {}, rollback redis", ticketId);
                return PlaceOrderResponse.fail(STOCK_CONFLICT, "Booking unsuccessful, please try again later");
            }

            long unitPrice = stockOrderCacheService.getEffectivePrice(ticketId);

            if (unitPrice <= 0) {
                stockOrderCacheService.increaseStockCacheByLUA(ticketId, quantity);
                log.warn("placeOrderCAS: price not found  for ticketId = {}, rollback redis", ticketId);
                return PlaceOrderResponse.fail(PRICE_NOT_FOUND, "Do not confirm price");
            }

            String userId = UUID.randomUUID().toString();
            String orderNumber = genOrderNumber(userId);
            String nTable = extractYearMonthFromOrderNumber(orderNumber);

            TicketOrder order = TicketOrder.builder()
                    .ticketId(ticketId)
                    .quantity(quantity)
                    .orderStatus(0)
                    .userId(userId)
                    .orderNumber(orderNumber)
                    .totalAmount(new BigDecimal(unitPrice * quantity))
                    .terminalId(TERMINAL)
                    .orderNotes("Order -> Pending")
                    .build();

            orderDeductionDomainService.insertOrder(nTable, order);

            log.info("placeOrderCAS: success | ticketId={} orderNumber={}", ticketId, orderNumber);

            orderCancelScheduleService.scheduleTimeout(orderNumber, nTable, ticketId, quantity);

            return PlaceOrderResponse.success(orderNumber);
        } catch (Exception e) {
            log.error("placeOrderCAS: error for ticketId={}", ticketId, e);
            if (isRedisDecremented) stockOrderCacheService.increaseStockCacheByLUA(ticketId, quantity);

            return PlaceOrderResponse.fail(ERROR, "Server error, please try again later");
        }
    }


    private String genOrderNumber(String userId) {
        return TERMINAL + "-" + System.currentTimeMillis() + "-" + userId.replace("-", "");
    }

    private String extractYearMonthFromOrderNumber(String orderNumber) {
        try {
            String[] parts = orderNumber.split("-");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Order number is invalid");
            }

            long timestamp = Long.parseLong(parts[2]);

            LocalDateTime time = Instant.ofEpochMilli(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            return time.format(DateTimeFormatter.ofPattern("yyyyMM"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract year month from order number: " + orderNumber, e);
        }
    }

    private List<TicketOrderResponse> getTicketOrderResponses(List<Object[]> results) {
        return results.stream().map(row -> new TicketOrderResponse(
                (String) row[0],
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (int) row[4],
                (int) row[5],
                (BigDecimal) row[6],
                (String) row[7],
                ((Timestamp) row[8]).toLocalDateTime(),
                (String) row[9],
                ((Timestamp) row[10]).toLocalDateTime(),
                ((Timestamp) row[11]).toLocalDateTime()
        )).toList();
    }

    private boolean handleTicketOrder(TicketOrderResponse ticketOrderResponse, String userId) {
        if (ticketOrderResponse == null || !ticketOrderResponse.getUserId().equals(userId)) {
            log.info("Order not found or not belong to user: {}", userId);
            return false;
        }

        if (ticketOrderResponse.getOrderStatus() == 2) {
            log.info("Order has been cancelled");
            return true;
        }

        String yearMonth = extractYearMonthFromOrderNumber(ticketOrderResponse.getOrderNumber());
        boolean isUpdated = orderDeductionDomainService.updateOrderStatus(yearMonth, ticketOrderResponse.getOrderNumber(), 2);

        if (!isUpdated) {
            log.error("Failed to update order status: {}", ticketOrderResponse.getOrderNumber());
            return false;
        }

        restoreStockTicketOrder(ticketOrderResponse.getTicketId(), ticketOrderResponse.getQuantity());

        log.info("Cancel Order Successfully: {}", ticketOrderResponse.getOrderNumber());

        return true;
    }

    private void restoreStockTicketOrder(String ticketId, int quantity) {
        log.info("Restoring stock ticketId: {}, quantity: {}", ticketId, quantity);

        try {
            boolean isStockRecoverDatabase = ticketOrderDomainService.increaseStock(ticketId, quantity);

            if (!isStockRecoverDatabase) {
                throw new IllegalAccessException("Failed to restore stock ticketId: " + ticketId);
            }

            boolean isStockRecoveredRedis = stockOrderCacheService.increaseStockCacheByLUA(ticketId, quantity);

            if (!isStockRecoveredRedis) {
                log.warn("Redis stock recovery failed (Inconsistency), ticket: {}", ticketId);
                stockOrderCacheService.addStockAvailableToCache(ticketId);
            }

        } catch (Exception e) {
            log.error("Failed to restore stock ticketId: {}, quantity: {}", ticketId, quantity, e);
            throw new RuntimeException(e);
        }
    }

    private String genEventCancelKeyLock(String orderNumber) {
        return "LOCK:CANCEL:" + orderNumber;
    }


}
