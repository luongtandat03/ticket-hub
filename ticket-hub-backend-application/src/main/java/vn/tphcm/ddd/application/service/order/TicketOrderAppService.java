/*
 * @ (#) TicketOrderAppService.java       1.0     9/3/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.order;

/*
 * @author: Luong Tan Dat
 * @date: 9/3/2026
 */

import vn.tphcm.ddd.application.dto.PageResponse;
import vn.tphcm.ddd.application.dto.PlaceOrderResponse;
import vn.tphcm.ddd.application.dto.TicketOrderResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketOrderAppService {
    boolean decreaseStockCAS(String ticketId, int quantity);

    List<TicketOrderResponse> findAll(String yearMonth);

    TicketOrderResponse findByOrderNumber(String orderNumber);

    List<TicketOrderResponse> findByDateRange(String nTable, LocalDateTime startDate, LocalDateTime endDate);

    boolean cancelOrder(String userId, String orderNumber);

    PageResponse<TicketOrderResponse> findPage(String yearMonth, String lastId, int limit);

    PlaceOrderResponse placeOrderCAS(String ticketId, int quantity);
}
