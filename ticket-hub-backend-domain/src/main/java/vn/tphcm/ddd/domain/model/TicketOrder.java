/*
 * @ (#) TicketOrder.java       1.0     9/4/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.model;
/*
 * @author: Luong Tan Dat
 * @date: 9/4/2026
 */

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketOrder {
    private String id;

    private String userId;

    private String orderNumber;

    private String ticketId;

    private int quantity;

    private int orderStatus;

    private BigDecimal totalAmount;

    private String terminalId;

    private LocalDateTime orderDate;

    private String orderNotes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
