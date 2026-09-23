/*
 * @ (#) TicketJPAMapper.java       1.0     9/15/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.persistence.mapper;

/*
 * @author: Luong Tan Dat
 * @date: 9/15/2026
 */

import org.springframework.data.jpa.repository.JpaRepository;
import vn.tphcm.ddd.domain.model.Ticket;

import java.util.List;

public interface TicketJPAMapper extends JpaRepository<Ticket, String> {
    List<Ticket> findByStatus(Integer status);
}
