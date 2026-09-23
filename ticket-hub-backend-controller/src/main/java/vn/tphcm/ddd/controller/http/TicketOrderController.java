/*
 * @ (#) TicketOrderController.java       1.0     8/28/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.controller.http;
/*
 * @author: Luong Tan Dat
 * @date: 8/28/2026
 */

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vn.tphcm.ddd.application.dto.PageResponse;
import vn.tphcm.ddd.application.dto.PlaceOrderResponse;
import vn.tphcm.ddd.application.dto.TicketOrderResponse;
import vn.tphcm.ddd.application.service.order.OrderMQAppService;
import vn.tphcm.ddd.application.service.order.TicketOrderAppService;
import vn.tphcm.ddd.controller.dto.ApiResponse;
import vn.tphcm.ddd.controller.dto.request.CreateBookingRequest;
import vn.tphcm.ddd.controller.dto.request.PlaceOrderMQRequest;
import vn.tphcm.ddd.domain.model.OrderQueue;
import vn.tphcm.ddd.domain.model.TicketOrder;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/order")
@Slf4j(topic = "TICKET-ORDER-CONTROLLER")
@RequiredArgsConstructor
public class TicketOrderController {
    private final TicketOrderAppService ticketOrderAppService;

    @PutMapping("/{ticketId}/{quantity}/cas")
    public ApiResponse<Boolean> orderTicketByCAS(
            @PathVariable("ticketId") String ticketId,
            @PathVariable("quantity") int quantity) {
        log.info("DecreaseStockCAS ticketId={}, quantity={}", ticketId, quantity);
        return ApiResponse.<Boolean>builder()
                .status(OK.value())
                .data(ticketOrderAppService.decreaseStockCAS(ticketId, quantity))
                .message("DecreaseStockCAS successful")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @GetMapping("/{userId}/list")
    public ApiResponse<List<TicketOrderResponse>> getListTicketByUser(
            @PathVariable("userId") String userId,
            @RequestParam("nTable") String nTable
    ) {
        log.info("Get list ticket by user | {}, {}", userId, nTable);
        return ApiResponse.<List<TicketOrderResponse>>builder()
                .status(OK.value())
                .data(ticketOrderAppService.findAll(nTable))
                .message("Get list ticket by user successful")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @GetMapping("/{userId}/{orderNumber}")
    public ApiResponse<TicketOrderResponse> getTicketByOrderNumber(
            @PathVariable("userId") String userId,
            @PathVariable("orderNumber") String orderNumber
    ) {
        log.info("Get ticket by order | {}, {}", userId, orderNumber);
        return ApiResponse.<TicketOrderResponse>builder()
                .status(OK.value())
                .data(ticketOrderAppService.findByOrderNumber(orderNumber))
                .message("Get ticket by order successful")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @GetMapping("/{userId}/{startDate}/{endDate}")
    public ApiResponse<List<TicketOrderResponse>> getTicketByDateRange(
            @PathVariable("userId") String userId,
            @PathVariable("startDate") LocalDateTime startDate,
            @PathVariable("endDate") LocalDateTime endDate,
            @RequestParam("nTable") String nTable
    ) {
        log.info("Get ticket by date |{}, {}, {}", userId, startDate, endDate);
        return ApiResponse.<List<TicketOrderResponse>>builder()
                .status(OK.value())
                .message("Get ticket by date successful")
                .data(ticketOrderAppService.findByDateRange(nTable, startDate, endDate))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @GetMapping("/{userId}/list/page")
    public ApiResponse<PageResponse<TicketOrderResponse>> getListOrderByUserPaged(
            @PathVariable("userId") String userId,
            @RequestParam("nTable") String nTable,
            @RequestParam(value = "cursor", defaultValue = "") String cursor,
            @RequestParam(value = "limit", defaultValue = "50") int limit
    ) {
        log.info("Get list ticket order with userId: {} cursor: {} limit: {}", userId, cursor, limit);
        return ApiResponse.<PageResponse<TicketOrderResponse>>builder()
                .status(OK.value())
                .message("Get list ticket order successful")
                .data(ticketOrderAppService.findPage(nTable, cursor, limit))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @DeleteMapping("/{userId}/{orderNumber}/cancel")
    public ApiResponse<Boolean> cancelTicketByOrderNumber(
            @PathVariable("userId") String userId,
            @PathVariable("orderNumber") String orderNumber
    ) {
        log.info("Cancel ticket by order | {}, {}", userId, orderNumber);

        return ApiResponse.<Boolean>builder()
                .status(OK.value())
                .message("Cancel ticket by order successful")
                .data(ticketOrderAppService.cancelOrder(userId, orderNumber))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @PostMapping("/cas")
    public ApiResponse<PlaceOrderResponse> placeOrderCas(@Valid @RequestBody CreateBookingRequest request) {
        log.info("Controller -> placeOrderCas | ticketId = {}, quantity = {}", request.getTicketId(), request.getQuantity());
        return ApiResponse.<PlaceOrderResponse>builder()
                .status(OK.value())
                .message("Place order cas successful")
                .data(ticketOrderAppService.placeOrderCAS(request.getTicketId(), request.getQuantity()))
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
