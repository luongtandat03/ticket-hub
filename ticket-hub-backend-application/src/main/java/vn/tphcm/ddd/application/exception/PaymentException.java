/*
 * @ (#) PaymentException.java       1.0     10/10/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */
package vn.tphcm.ddd.application.exception;

/*
 * @author: Luong Tan Dat
 * @date: 10/10/2026
 */

public class PaymentException extends RuntimeException {
    public PaymentException(String message) {
        super(message);
    }
}
