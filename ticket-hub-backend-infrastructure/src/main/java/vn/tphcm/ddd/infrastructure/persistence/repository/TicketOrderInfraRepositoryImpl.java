/*
 * @ (#) TicketOrderInfrasimpl.java       1.0     9/4/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.persistence.repository;
/*
 * @author: Luong Tan Dat
 * @date: 9/4/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import vn.tphcm.ddd.domain.repository.TicketOrderRepository;
import vn.tphcm.ddd.infrastructure.persistence.mapper.TicketOrderJPAMapper;

@Repository
@RequiredArgsConstructor
@Slf4j(topic = "TICKET-ORDER-INFRAS")
public class TicketOrderInfraRepositoryImpl implements TicketOrderRepository {
    private final TicketOrderJPAMapper ticketOrderJPAMapper;

    @Override
    public boolean decreaseStock(String ticketId, int quantity) {
        log.info("Run: decreaseStock with: {}, {}", ticketId, quantity);
        return ticketOrderJPAMapper.decreaseStock(ticketId, quantity) > 0;
    }

    @Override
    public boolean decreaseStockCAS(String ticketId, int oldStockAvailable, int quantity) {
        log.info("Run: decreaseStockCAS with: | {}, {}, {}", ticketId, oldStockAvailable, quantity);
        return ticketOrderJPAMapper.decreaseStockCAS(ticketId, oldStockAvailable, quantity) > 0;
    }

    @Override
    public boolean increaseStock(String ticketId, int quantity) {
        log.info("Run: increaseStock with: | {}, {}", ticketId, quantity);
        return ticketOrderJPAMapper.increaseStock(ticketId, quantity) > 0;
    }
}
