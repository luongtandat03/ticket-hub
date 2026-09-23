/*
 * @ (#) TicketRepositoryImpl.java       1.0     9/15/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.persistence.repository;
/*
 * @author: Luong Tan Dat
 * @date: 9/15/2026
 */

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.tphcm.ddd.domain.model.Ticket;
import vn.tphcm.ddd.domain.repository.TicketRepository;
import vn.tphcm.ddd.infrastructure.persistence.mapper.TicketJPAMapper;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TicketInfraRepositoryImpl implements TicketRepository {
    private final TicketJPAMapper ticketJPAMapper;

    @Override
    public Ticket findById(String ticketId) {
        return ticketJPAMapper.findById(ticketId).orElse(null);
    }

    @Override
    public List<Ticket> findByStatus(Integer status) {
        return ticketJPAMapper.findByStatus(status);
    }
}
