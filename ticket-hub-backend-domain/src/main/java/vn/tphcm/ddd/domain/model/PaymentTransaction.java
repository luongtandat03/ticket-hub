/*
 * @ (#) PaymentTransaction.java       1.0     10/8/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.model;
/*
 * @author: Luong Tan Dat
 * @date: 10/8/2026
 */

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tbl_payment_transaction")
@Builder
public class PaymentTransaction extends AbstractEntity<String> {
    private String paymentId;

    private String orderNumber;

    private String userId;

    private BigDecimal amount;

    private String paymentMethod;

    private Integer paymentStatus;

    private String gatewayTransactionId;

    private String paymentUrl;

    private String createDate;
}
