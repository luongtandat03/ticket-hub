/*
 * @ (#) TicketOrderResponse.java       1.0     9/7/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.dto;
/*
 * @author: Luong Tan Dat
 * @date: 9/7/2026
 */

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketOrderResponse {
    private String id;
    private String userId;
    private String ticketId;
    private String orderNumber;
    private int quantity;
    private int orderStatus;
    private BigDecimal totalAmount;
    private String terminalId;
    private LocalDateTime orderDate;
    private String orderNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
