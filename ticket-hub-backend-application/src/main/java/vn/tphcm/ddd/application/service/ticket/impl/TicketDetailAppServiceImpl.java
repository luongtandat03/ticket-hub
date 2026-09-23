/*
 * @ (#) TicketDetailAppServiceImpl.java       1.0     8/5/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.ticket.impl;
/*
 * @author: Luong Tan Dat
 * @date: 8/5/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.tphcm.ddd.application.dto.TicketDetailResponse;
import vn.tphcm.ddd.application.service.ticket.TicketDetailAppService;
import vn.tphcm.ddd.application.service.ticket.cache.TicketDetailCacheService;
import vn.tphcm.ddd.domain.model.TicketDetail;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "TICKET-DETAIL-APP-SERVICE")
public class TicketDetailAppServiceImpl implements TicketDetailAppService {
    private final TicketDetailCacheService ticketDetailCacheService;

    @Override
    public TicketDetailResponse getTicketDetail(String ticketId, Long version) {
        return ticketDetailCacheService.getTicketDetail(ticketId, version);
    }

    @Override
    public boolean orderTicketByUser(String ticketId) {
        return ticketDetailCacheService.orderTicketByUser(ticketId);
    }
}
