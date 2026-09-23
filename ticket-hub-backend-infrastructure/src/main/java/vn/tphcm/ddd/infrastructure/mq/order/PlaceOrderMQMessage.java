/*
 * @ (#) PlaceOrderMQMessage.java       1.0     9/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.mq.order;
/*
 * @author: Luong Tan Dat
 * @date: 9/13/2026
 */

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderMQMessage {
    private String token;
    private String ticketId;
    private int quantity;
    private String userId;
    private long unitPrice;
    private long timestamp;
}
