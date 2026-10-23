/*
 * @ (#) PaymentTransactionDomainImpl.java       1.0     10/8/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.service.impl;
/*
 * @author: Luong Tan Dat
 * @date: 10/8/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.tphcm.ddd.domain.model.PaymentTransaction;
import vn.tphcm.ddd.domain.repository.PaymentTransactionRepository;
import vn.tphcm.ddd.domain.service.PaymentTransactionDomainService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "PAYMENT-TRANSACTION-DOMAIN")
public class PaymentTransactionDomainImpl implements PaymentTransactionDomainService {
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Override
    public boolean createPaymentTransaction(PaymentTransaction paymentTransaction) {
        log.info("PaymentTransactionDomainImpl createPaymentTransaction");

        if (paymentTransaction.getPaymentStatus() == null) {
            paymentTransaction.setPaymentStatus(0);
        }
        return paymentTransactionRepository.save(paymentTransaction);
    }

    @Override
    public boolean updateTransactionInProgress(String paymentId, String url, String gatewayId, int status) {
        log.info("Domain: updating transaction in progress");

        return paymentTransactionRepository.updateStatus(paymentId, status, gatewayId, url);
    }

    @Override
    public PaymentTransaction findByOrderNumber(String orderNumber) {
        log.info("Domain: find by order number");

        return paymentTransactionRepository.getPaymentTransactionByOrderNumber(orderNumber).orElse(null);
    }

    @Override
    public List<PaymentTransaction> findAll() {
        log.info("Domain: find all payment transactions");

        return paymentTransactionRepository.findAll();
    }

    @Override
    public List<PaymentTransaction> findStuckInProgress(LocalDateTime createdDate, int limit) {
        log.info("Domain: find stuck in progress");

        return paymentTransactionRepository.findStuckInProgress(createdDate, limit);
    }
}
