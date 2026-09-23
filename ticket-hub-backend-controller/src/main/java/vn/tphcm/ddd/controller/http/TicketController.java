/*
 * @ (#) TicketController.java       1.0     9/15/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.controller.http;
/*
 * @author: Luong Tan Dat
 * @date: 9/15/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vn.tphcm.ddd.application.dto.TicketResponse;
import vn.tphcm.ddd.application.service.ticket.TicketAppService;
import vn.tphcm.ddd.controller.dto.ApiResponse;

import java.util.List;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ticket")
@Slf4j(topic = "TICKET-CONTROLLER")
public class TicketController {
    private final TicketAppService ticketAppService;

    @GetMapping("/{ticketId}")
    public ApiResponse<TicketResponse> getTicket(@PathVariable("ticketId") String ticketId) {
        return ApiResponse.<TicketResponse>builder()
                .status(OK.value())
                .message("Get Ticket Success")
                .data(ticketAppService.getTicketById(ticketId))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @GetMapping
    public ApiResponse<List<TicketResponse>> getTicketByStatus(
            @RequestParam(value = "version", required = false) String version
    ) {
        return ApiResponse.<List<TicketResponse>>builder()
                .status(OK.value())
                .message("Get Ticket Success")
                .data(ticketAppService.getTicketsByStatus(1, version))
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
