/*
 * @ (#) TicketDomainService.java       1.0     9/15/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.service;

/*
 * @author: Luong Tan Dat
 * @date: 9/15/2026
 */

import vn.tphcm.ddd.domain.model.Ticket;

import java.util.List;

public interface TicketDomainService {
    Ticket getTicketById(String ticketId);
    List<Ticket> findByStatus(Integer status);
}
