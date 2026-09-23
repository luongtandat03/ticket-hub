/*
 * @ (#) TicketRepository.java       1.0     9/15/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.repository;

/*
 * @author: Luong Tan Dat
 * @date: 9/15/2026
 */

import vn.tphcm.ddd.domain.model.Ticket;

import java.util.List;

public interface TicketRepository {
    Ticket findById(String ticketId);
    List<Ticket> findByStatus(Integer status);
}
