/*
 * @ (#) PaymentTransactionRepository.java       1.0     10/8/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.repository;

import vn.tphcm.ddd.domain.model.PaymentTransaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/*
 * @author: Luong Tan Dat
 * @date: 10/8/2026
 */
public interface PaymentTransactionRepository {
    boolean save(PaymentTransaction paymentTransaction);

    boolean updateStatus(String paymentId, int status, String gatewayId, String url);

    Optional<PaymentTransaction> getPaymentTransactionByOrderNumber(String orderNumber);

    List<PaymentTransaction> findAll();

    List<PaymentTransaction> findStuckInProgress(LocalDateTime createdDate, int limit);
}
