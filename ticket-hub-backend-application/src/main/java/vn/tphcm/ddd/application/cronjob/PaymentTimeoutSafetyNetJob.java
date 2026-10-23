/*
 * @ (#) PaymentTimeoutSafetyNetJob.java       1.0     10/14/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.cronjob;
/*
 * @author: Luong Tan Dat
 * @date: 10/14/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.tphcm.ddd.application.service.payment.PaymentAppService;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "PAYMENT-TIMEOUT")
public class PaymentTimeoutSafetyNetJob {
    private static final long RECONCILE_AFTER_MINUTES = 16;
    private static final int RECONCILE_BATCH_SIZE = 50;

    private final PaymentAppService paymentAppService;

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void reconcile() {
        int resolved = paymentAppService.reconcileStuckPayment(RECONCILE_AFTER_MINUTES, RECONCILE_BATCH_SIZE);

        if (resolved > 0) {
            log.info("[RECONCILE] resolved {} stuck payments", resolved);
        }
    }
}
