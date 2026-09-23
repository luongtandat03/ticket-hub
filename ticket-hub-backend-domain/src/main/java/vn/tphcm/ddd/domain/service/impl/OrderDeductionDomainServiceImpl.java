/*
 * @ (#) OrderDeductionDomainServiceImpl.java       1.0     9/5/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.service.impl;
/*
 * @author: Luong Tan Dat
 * @date: 9/5/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.tphcm.ddd.domain.model.TicketOrder;
import vn.tphcm.ddd.domain.repository.OrderDeductionRepository;
import vn.tphcm.ddd.domain.service.OrderDeductionDomainService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ORDER-DEDUCTION-DOMAIN-SERVICE")
public class OrderDeductionDomainServiceImpl implements OrderDeductionDomainService {
    private final OrderDeductionRepository orderDeductionRepository;

    @Override
    @Transactional
    public void insertOrder(String yearMonth, TicketOrder ticketOrder) {
        orderDeductionRepository.insertOrder(yearMonth, ticketOrder);
    }

    @Override
    public List<Object[]> findAll(String yearMonth) {
        return orderDeductionRepository.findAll(yearMonth);
    }

    @Override
    public Object[] findByOrderNumber(String nTable, String orderNumber) {
        return orderDeductionRepository.findByOrderNumber(nTable, orderNumber);
    }

    @Override
    public List<Object[]> findByDateRange(String nTable, LocalDateTime startDate, LocalDateTime endDate) {
        return orderDeductionRepository.findByDateRange(nTable, startDate, endDate);
    }

    @Override
    public List<Object[]> findPage(String yearMonth, String lastId, int limit) {
        return orderDeductionRepository.findPage(yearMonth, lastId, limit);
    }

    @Override
    public boolean updateOrderStatus(String yearMonth, String orderNumber, int orderStatus) {
        return orderDeductionRepository.updateOrderStatus(yearMonth, orderNumber, orderStatus);
    }


}
