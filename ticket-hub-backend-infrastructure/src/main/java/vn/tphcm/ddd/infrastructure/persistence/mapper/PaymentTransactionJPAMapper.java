/*
 * @ (#) PaymentTransaction.java       1.0     10/8/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.persistence.mapper;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.tphcm.ddd.domain.model.PaymentTransaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/*
 * @author: Luong Tan Dat
 * @date: 10/8/2026
 */
public interface PaymentTransactionJPAMapper extends JpaRepository<PaymentTransaction, String> {
    @Modifying
    @Transactional
    @Query("UPDATE PaymentTransaction p SET p.paymentStatus = :status, " +
            "p.gatewayTransactionId = :gatewayId, p.paymentUrl = :url, p.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE p.paymentId = :paymentId")
    int updateStatus(@Param("paymentId") String paymentId,
                     @Param("status") int status,
                     @Param("gatewayId") String gatewayId,
                     @Param("url") String url);

    Optional<PaymentTransaction> findByOrderNumber(String orderNumber);

    List<PaymentTransaction> findByPaymentStatusAndCreatedAtBeforeOrderByCreatedAt(
            Integer paymentStatus, LocalDateTime createdBefore, Limit limit);
}
