/*
 * @ (#) PaymentTransactionDomainService.java       1.0     10/8/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.service;

import vn.tphcm.ddd.domain.model.PaymentTransaction;

import java.time.LocalDateTime;
import java.util.List;

/*
 * @author: Luong Tan Dat
 * @date: 10/8/2026
 */
public interface PaymentTransactionDomainService {
    boolean createPaymentTransaction(PaymentTransaction paymentTransaction);

    boolean updateTransactionInProgress(String paymentId, String url, String gatewayId, int status);

    PaymentTransaction findByOrderNumber(String orderNumber);

    List<PaymentTransaction> findAll();

    List<PaymentTransaction> findStuckInProgress(LocalDateTime createdDate, int limit);
}
