/*
 * @ (#) TicketDomainServiceImpl.java       1.0     9/15/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.service.impl;
/*
 * @author: Luong Tan Dat
 * @date: 9/15/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.tphcm.ddd.domain.model.Ticket;
import vn.tphcm.ddd.domain.repository.TicketRepository;
import vn.tphcm.ddd.domain.service.TicketDomainService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "TICKET-DOMAIN-SERIVCE")
public class TicketDomainServiceImpl implements TicketDomainService {
    private final TicketRepository ticketRepository;

    @Override
    public Ticket getTicketById(String ticketId) {
        return ticketRepository.findById(ticketId);
    }

    @Override
    public List<Ticket> findByStatus(Integer status) {
        return ticketRepository.findByStatus(status);
    }

}
