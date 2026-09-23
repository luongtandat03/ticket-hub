/*
 * @ (#) TicketAppServiceImpl.java       1.0     9/15/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.ticket.impl;
/*
 * @author: Luong Tan Dat
 * @date: 9/15/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.tphcm.ddd.application.dto.TicketResponse;
import vn.tphcm.ddd.application.mapper.TicketMapper;
import vn.tphcm.ddd.application.service.ticket.TicketAppService;
import vn.tphcm.ddd.application.service.ticket.cache.TicketAppCacheService;
import vn.tphcm.ddd.domain.model.Ticket;
import vn.tphcm.ddd.domain.service.TicketDomainService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "TICKET-APP-SERVICE")
public class TicketAppServiceImpl implements TicketAppService {
    private final TicketAppCacheService ticketAppCacheService;
    private final TicketDomainService ticketDomainService;
    private final TicketMapper ticketMapper;

    @Override
    public TicketResponse getTicketById(String ticketId) {
        Ticket ticket = ticketDomainService.getTicketById(ticketId);

        return ticketMapper.toResponse(ticket);
    }

    @Override
    public List<TicketResponse> getTicketsByStatus(Integer status, String version) {
        return ticketAppCacheService.getTicketsByStatus(status, version);
    }
}
