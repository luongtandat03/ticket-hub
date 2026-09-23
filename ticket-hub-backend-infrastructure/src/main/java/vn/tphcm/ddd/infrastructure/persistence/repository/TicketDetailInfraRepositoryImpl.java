/*
 * @ (#) TicketDetailInfrasImpl.java       1.0     9/4/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.persistence.repository;
/*
 * @author: Luong Tan Dat
 * @date: 9/4/2026
 */

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.tphcm.ddd.domain.model.TicketDetail;
import vn.tphcm.ddd.domain.repository.TicketDetailRepository;
import vn.tphcm.ddd.infrastructure.persistence.mapper.TicketDetailJPAMapper;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TicketDetailInfraRepositoryImpl implements TicketDetailRepository {

    private final TicketDetailJPAMapper ticketDetailJPAMapper;

    @Override
    public Optional<TicketDetail> findById(String ticketId) {
        return ticketDetailJPAMapper.findById(ticketId);
    }
}
