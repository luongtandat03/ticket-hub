/*
 * @ (#) TicketDetailAppService.java       1.0     8/5/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.ticket;

import vn.tphcm.ddd.application.dto.TicketDetailResponse;
import vn.tphcm.ddd.domain.model.TicketDetail;

/*
 * @author: Luong Tan Dat
 * @date: 8/5/2026
 */

public interface TicketDetailAppService {
    TicketDetailResponse getTicketDetail(String detailId, Long version);
    boolean orderTicketByUser(String ticketId);
}
