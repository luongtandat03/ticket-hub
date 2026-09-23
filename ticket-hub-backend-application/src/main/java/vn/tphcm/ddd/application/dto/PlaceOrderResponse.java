/*
 * @ (#) PlaceOrderResponse.java       1.0     9/12/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.dto;
/*
 * @author: Luong Tan Dat
 * @date: 9/12/2026
 */


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class PlaceOrderResponse {
    private boolean success;
    private String code;
    private String message;
    private String placeOrderTaskId;
    private String orderId;

    public static PlaceOrderResponse success(String placeOrderTaskId) {
        return new PlaceOrderResponse()
                .setSuccess(true)
                .setPlaceOrderTaskId(placeOrderTaskId);
    }

    public static PlaceOrderResponse success() {
        return new PlaceOrderResponse()
                .setSuccess(true);
    }

    public static PlaceOrderResponse fail(String code, String message) {
        return new PlaceOrderResponse()
                .setSuccess(false)
                .setCode(code)
                .setMessage(message);
    }

    public static PlaceOrderResponse isTrue() {
        return new PlaceOrderResponse()
                .setSuccess(true);
    }

}
