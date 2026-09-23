/*
 * @ (#) OrderMQAppService.java       1.0     9/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.order;

import vn.tphcm.ddd.domain.model.OrderQueue;

/*
 * @author: Luong Tan Dat
 * @date: 9/13/2026
 */
public interface OrderMQAppService {
    /**
     * Order in Kafka, response token
     * @param ticketId
     * @param quantity
     * @return
     */
    OrderQueue placeOrderMQ(String ticketId, int quantity);

    /**
     * Check order by token
     * @param token
     * @return
     */
    OrderQueue getOrderStatus(String token);
}
