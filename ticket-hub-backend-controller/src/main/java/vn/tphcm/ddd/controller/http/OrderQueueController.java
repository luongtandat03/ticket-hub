/*
 * @ (#) OrderQueueController.java       1.0     9/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.controller.http;
/*
 * @author: Luong Tan Dat
 * @date: 9/13/2026
 */

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vn.tphcm.ddd.application.dto.PlaceOrderResponse;
import vn.tphcm.ddd.application.service.order.OrderMQAppService;
import vn.tphcm.ddd.controller.dto.ApiResponse;
import vn.tphcm.ddd.controller.dto.request.PlaceOrderMQRequest;
import vn.tphcm.ddd.domain.model.OrderQueue;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order")
@Slf4j(topic = "ORDER-QUEUE-CONTROLLER")
public class OrderQueueController {
    private final OrderMQAppService orderMQAppService;

    @PostMapping("/mq")
    public ApiResponse<PlaceOrderResponse> placeOrderMQ(@Valid @RequestBody PlaceOrderMQRequest request) {
        log.info("Controller -> placeOrderMQ | ticketId = {}, quantity = {}", request.getTicketId(), request.getQuantity());

        return ApiResponse.<PlaceOrderResponse>builder()
                .status(OK.value())
                .message("Place order mq successful")
                .data(toResponse(orderMQAppService.placeOrderMQ(request.getTicketId(), request.getQuantity())))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @GetMapping("/status/{token}")
    public ApiResponse<OrderQueue> getOrderStatus(@PathVariable("token") String token) {
        log.info("OrderMQController:->getOrderStatus | token={}", token);
        return ApiResponse.<OrderQueue>builder()
                .status(OK.value())
                .message("Order status successful")
                .data(orderMQAppService.getOrderStatus(token))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private PlaceOrderResponse toResponse(OrderQueue orderQueue) {
        if (orderQueue.getStatus() == 2) {
            String msg = orderQueue.getMessage() != null ? orderQueue.getMessage() : "SERVER_ERROR";
            String code = msg.contains(":") ? msg.substring(0, msg.indexOf(":")).trim() : "ERROR";
            return PlaceOrderResponse.fail(code, msg);
        }

        return PlaceOrderResponse.success(orderQueue.getToken());
    }
}
