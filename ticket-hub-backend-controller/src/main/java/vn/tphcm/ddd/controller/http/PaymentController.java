/*
 * @ (#) PaymentController.java       1.0     10/9/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.controller.http;
/*
 * @author: Luong Tan Dat
 * @date: 10/9/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.tphcm.ddd.application.dto.response.PaymentResponse;
import vn.tphcm.ddd.application.service.payment.PaymentAppService;
import vn.tphcm.ddd.controller.dto.ApiResponse;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j(topic = "PAYMENT-CONTROLLER")
public class PaymentController {
    private final PaymentAppService paymentAppService;

    @PostMapping("/create")
    public ApiResponse<String> paymentOrder(@RequestParam("orderNumber") String orderNumber,
                                            @RequestParam("method") String method) throws UnsupportedEncodingException {
        log.info("Controller: -> paymentOrder | {}, {}", orderNumber, method);

        return ApiResponse.<String>builder()
                .status(OK.value())
                .message("Create payment transaction successful")
                .data(paymentAppService.paymentOrder(orderNumber, method))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @GetMapping("/callback")
    public Map<String, String> callback(@RequestParam Map<String, String> parmas) {
        log.info("Controller: -> callback | {}", parmas);

        return paymentAppService.handleVnPayIPN(parmas);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') and authentication.name == 'admin'")
    public ApiResponse<List<PaymentResponse>> getPayment() {
        log.info("Controller -> getPayment");

        return ApiResponse.<List<PaymentResponse>>builder()
                .status(OK.value())
                .message("Get Payment successful")
                .data(paymentAppService.findAll())
                .timestamp(System.currentTimeMillis())
                .build();
    }

}
