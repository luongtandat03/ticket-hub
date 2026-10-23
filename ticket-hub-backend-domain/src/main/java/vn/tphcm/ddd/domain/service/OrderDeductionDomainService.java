/*
 * @ (#) OrderDeductionDomainService.java       1.0     9/5/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.service;

import vn.tphcm.ddd.domain.model.TicketOrder;

import java.time.LocalDateTime;
import java.util.List;

/*
 * @author: Luong Tan Dat
 * @date: 9/5/2026
 */
public interface OrderDeductionDomainService {
    void insertOrder(String yearMonth, TicketOrder ticketOrder);

    List<Object[]> findAll(String yearMonth);

    Object[] findByOrderNumber(String nTable, String orderNumber);

    List<Object[]> findByDateRange(String nTable, LocalDateTime startDate, LocalDateTime endDate);

    List<Object[]> findPage(String yearMonth, String lastId, int limit);

    boolean updateOrderStatus(String yearMonth, String orderNumber, int orderStatus);

    boolean compareAndSetOrderStatus(String yearMonth, String orderNumber, int expectedStatus, int newStatus);

    List<String> findExpiredPendingOrderNumbers(String yearMonth, LocalDateTime createdBefore, int limit);
}
