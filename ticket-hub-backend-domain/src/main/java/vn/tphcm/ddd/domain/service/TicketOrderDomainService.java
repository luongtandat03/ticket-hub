/*
 * @ (#) TicketOrderDomainService.java       1.0     9/4/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.service;

/*
 * @author: Luong Tan Dat
 * @date: 9/4/2026
 */
public interface TicketOrderDomainService {
    boolean decreaseStock(String ticketId, int quantity);
    boolean decreaseStockCAS(String ticketId, int oldStockAvailable,int quantity);

    boolean increaseStock(String ticketId, int quantity);
}
