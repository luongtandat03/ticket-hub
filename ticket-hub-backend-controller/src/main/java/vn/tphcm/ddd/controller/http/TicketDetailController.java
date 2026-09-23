/*
 * @ (#) TicketDetailController.java       1.0     8/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.controller.http;
/*
 * @author: Luong Tan Dat
 * @date: 8/13/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.tphcm.ddd.application.dto.TicketDetailResponse;
import vn.tphcm.ddd.application.service.ticket.TicketDetailAppService;
import vn.tphcm.ddd.controller.dto.ApiResponse;

@RestController
@RequestMapping("/ticket")
@Slf4j(topic = "TICKET-DETAIL-CONTROLLER")
@RequiredArgsConstructor
public class TicketDetailController {
    private final TicketDetailAppService ticketDetailAppService;

    @GetMapping("/{ticketId}/detail/{detailId}")
    public ApiResponse<TicketDetailResponse> getTicketDetail(
            @PathVariable("ticketId") String ticketId,
            @PathVariable("detailId") String detailId,
            @RequestParam(value = "version", required = false) Long version
    ) {
        return ApiResponse.<TicketDetailResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Get ticket detail successfully")
                .data(ticketDetailAppService.getTicketDetail(detailId, version))
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
