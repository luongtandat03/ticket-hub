/*
 * @ (#) PlaceCancelMQMessage.java       1.0     9/19/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.mq.order;
/*
 * @author: Luong Tan Dat
 * @date: 9/19/2026
 */

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelMQMessage {
    private String orderNumber;

    private String yearMonth;

    private String ticketId;

    private int quantity;

    private LocalDateTime createdAt;
}
