/*
 * @ (#) TicketOrderDomainServiceImpl.java       1.0     9/4/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.service.impl;
/*
 * @author: Luong Tan Dat
 * @date: 9/4/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.tphcm.ddd.domain.repository.TicketOrderRepository;
import vn.tphcm.ddd.domain.service.TicketOrderDomainService;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "TICKET-ORDER-DOMAIN-SERVICE")
public class TicketOrderDomainServiceImpl implements TicketOrderDomainService {
    private final TicketOrderRepository ticketOrderRepository;

    @Override
    public boolean decreaseStock(String ticketId, int quantity) {
        log.info("decreaseStock with ticketId: {}, quantity: {}", ticketId, quantity);

        return ticketOrderRepository.decreaseStock(ticketId, quantity);
    }

    @Override
    public boolean decreaseStockCAS(String ticketId, int oldStockAvailable, int quantity) {
        log.info("decreaseStockCAS with {} , {}, {}", ticketId, oldStockAvailable, quantity);

        return ticketOrderRepository.decreaseStockCAS(ticketId, oldStockAvailable, quantity);
    }

    @Override
    public boolean increaseStock(String ticketId, int quantity) {
        log.info("increaseStock with {}, {}", ticketId, quantity);

        return ticketOrderRepository.increaseStock(ticketId, quantity);
    }
}
