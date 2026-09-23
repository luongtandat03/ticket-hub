/*
 * @ (#) OrderDeductionRepository.java       1.0     9/4/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.tphcm.ddd.domain.model.TicketOrder;

import java.time.LocalDateTime;
import java.util.List;

/*
 * @author: Luong Tan Dat
 * @date: 9/4/2026
 */
public interface OrderDeductionRepository {
    void insertOrder(String yearMonth, TicketOrder ticketOrder);
    List<Object[]> findAll(String yearMonth);
    Object[] findByOrderNumber(String nTable, String orderNumber);
    List<Object[]> findByDateRange(String yearMonth, LocalDateTime startDate,  LocalDateTime endDate);
    List<Object[]> findPage(String yearMonth, String lastId, int limit);
    boolean updateOrderStatus(String yearMonth, String orderNumber, int orderStatus);
}
