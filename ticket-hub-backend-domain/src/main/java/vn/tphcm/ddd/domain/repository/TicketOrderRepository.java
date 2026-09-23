/*
 * @ (#) TicketOrderRepository.java       1.0     9/4/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.repository;

/*
 * @author: Luong Tan Dat
 * @date: 9/4/2026
 */

import org.springframework.stereotype.Repository;

@Repository
public interface TicketOrderRepository {
    boolean decreaseStock(String ticketId, int quantity);

    boolean decreaseStockCAS(String ticketId, int oldStockAvailable, int quantity);

    boolean increaseStock(String ticketId, int quantity);
}
