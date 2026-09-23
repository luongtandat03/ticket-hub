/*
 * @ (#) TicketDetailRepository.java       1.0     8/6/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.repository;

/*
 * @author: Luong Tan Dat
 * @date: 8/6/2026
 */

import vn.tphcm.ddd.domain.model.TicketDetail;

import java.util.Optional;

public interface TicketDetailRepository {
    Optional<TicketDetail> findById(String ticketId);
}
