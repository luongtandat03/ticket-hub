/*
 * @ (#) InvalidInputException.java       1.0     10/20/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */
package vn.tphcm.ddd.application.exception;

/*
 * @author: Luong Tan Dat
 * @date: 10/20/2026
 */

public class InvalidInputException extends RuntimeException {
    public InvalidInputException(String message) {
        super(message);
    }
}
