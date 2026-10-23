/*
 * @ (#) PaymentResponse.java       1.0     10/12/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.dto.response;
/*
 * @author: Luong Tan Dat
 * @date: 10/12/2026
 */

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private String id;
    private String paymentId;
    private String orderNumber;
    private String userId;
    private BigDecimal amount;
    private String paymentMethod;
    private Integer paymentStatus;
    private String gatewayTransactionId;
    private String paymentUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
