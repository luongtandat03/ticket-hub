/*
 * @ (#) TicketDetailDomainServiceImpl.java       1.0     8/6/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.domain.service.impl;
/*
 * @author: Luong Tan Dat
 * @date: 8/6/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.tphcm.ddd.domain.model.TicketDetail;
import vn.tphcm.ddd.domain.repository.TicketDetailRepository;
import vn.tphcm.ddd.domain.service.TicketDetailDomainService;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "TICKET-DETAIL-DOMAIN-SERVICE")
public class TicketDetailDomainServiceImpl implements TicketDetailDomainService {
    private final TicketDetailRepository ticketDetailRepository;
    @Override
    public TicketDetail getTicketDefaultCacheVip(String id) {
        log.info("Implement Domain: {}",id);
        return ticketDetailRepository.findById(id).orElse(null);
    }
}
