/*
 * @ (#) OrderTimeoutSafetyNetJob.java       1.0     9/20/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.cronjob;
/*
 * @author: Luong Tan Dat
 * @date: 9/20/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.tphcm.ddd.application.schedule.OrderCancelSchedule;
import vn.tphcm.ddd.application.service.order.TicketOrderAppService;
import vn.tphcm.ddd.domain.service.OrderDeductionDomainService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "ORDER-TIMEOUT-SAFETY")
public class OrderTimeoutSafetyNetJob {
    private static final int BATCH_SIZE = 50;
    private static final long GRACE_MINUTES = 10;
    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final OrderDeductionDomainService orderDeductionDomainService;
    private final TicketOrderAppService ticketOrderAppService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void cancelStalePendingOrders() {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());

        LocalDateTime createdBefore = now.minusMinutes(OrderCancelSchedule.PAYMENT_TIMEOUT_MINUTES + GRACE_MINUTES);

        for (String yearMonth : List.of(now.format(YEAR_MONTH), now.minusMonths(1).format(YEAR_MONTH))) {
            List<String> orderNumbers;

            try {
                orderNumbers = orderDeductionDomainService.findExpiredPendingOrderNumbers(yearMonth, createdBefore, BATCH_SIZE);
            } catch (Exception ex) {
                log.warn("[SAFETY-NET] skip table {}: {}", yearMonth, ex.getMessage());
                continue;
            }

            for (String orderNumber : orderNumbers) {
                try {
                    ticketOrderAppService.cancelExpiredOrder(orderNumber);
                } catch (Exception ex) {
                    log.error("[SAFETY-NET] failed to cancel order {}", orderNumber, ex);
                }
            }
        }

    }
}
