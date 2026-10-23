/*
 * @ (#) PaymentTransactionRepository.java       1.0     10/8/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.persistence.repository;
/*
 * @author: Luong Tan Dat
 * @date: 10/8/2026
 */

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
import vn.tphcm.ddd.domain.model.PaymentTransaction;
import vn.tphcm.ddd.domain.repository.PaymentTransactionRepository;
import vn.tphcm.ddd.infrastructure.persistence.mapper.PaymentTransactionJPAMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentTransactionRepositoryImpl implements PaymentTransactionRepository {
    private final PaymentTransactionJPAMapper paymentTransactionJPAMapper;

    @Override
    public boolean save(PaymentTransaction paymentTransaction) {
        paymentTransactionJPAMapper.save(paymentTransaction);
        return true;
    }

    @Override
    public boolean updateStatus(String paymentId, int status, String gatewayId, String url) {
        return paymentTransactionJPAMapper.updateStatus(paymentId, status, gatewayId, url) > 0;
    }

    @Override
    public Optional<PaymentTransaction> getPaymentTransactionByOrderNumber(String orderNumber) {
        return paymentTransactionJPAMapper.findByOrderNumber(orderNumber);
    }

    @Override
    public List<PaymentTransaction> findAll() {
        return paymentTransactionJPAMapper.findAll();
    }

    @Override
    public List<PaymentTransaction> findStuckInProgress(LocalDateTime createdDate, int limit) {
        return paymentTransactionJPAMapper.findByPaymentStatusAndCreatedAtBeforeOrderByCreatedAt(1, createdDate, Limit.of(limit));
    }
}
