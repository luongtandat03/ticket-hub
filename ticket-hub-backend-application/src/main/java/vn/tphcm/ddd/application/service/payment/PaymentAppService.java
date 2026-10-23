/*
 * @ (#) PaymentAppService.java       1.0     10/8/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.payment;

/*
 * @author: Luong Tan Dat
 * @date: 10/8/2026
 */

import vn.tphcm.ddd.application.dto.response.PaymentResponse;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

public interface PaymentAppService {
    String paymentOrder(String orderNumber, String method) throws UnsupportedEncodingException;
    Map<String, String> handleVnPayIPN(Map<String, String> request);
    List<PaymentResponse> findAll();
    int reconcileStuckPayment(long expiredTime, int limit);
}
