/*
 * @ (#) TicketAppService.java       1.0     9/15/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.ticket;

/*
 * @author: Luong Tan Dat
 * @date: 9/15/2026
 */

import lombok.RequiredArgsConstructor;
import vn.tphcm.ddd.application.dto.TicketResponse;

import java.util.List;

public interface TicketAppService {
    TicketResponse getTicketById(String ticketId);
    List<TicketResponse> getTicketsByStatus(Integer status, String version);
}
