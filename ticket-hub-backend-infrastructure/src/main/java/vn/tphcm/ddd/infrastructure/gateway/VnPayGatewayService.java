/*
 * @ (#) VnPayGatewayServiceImpl.java       1.0     10/8/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.infrastructure.gateway;
/*
 * @author: Luong Tan Dat
 * @date: 10/8/2026
 */

import vn.tphcm.ddd.domain.model.PaymentTransaction;

import java.io.UnsupportedEncodingException;
import java.util.Map;


public interface VnPayGatewayService {
    String createPaymentUrl(PaymentTransaction transaction) throws UnsupportedEncodingException;
    boolean verifySignature(Map<String, String> params);
    Map<String, String> queryTransaction(PaymentTransaction transaction);
}
