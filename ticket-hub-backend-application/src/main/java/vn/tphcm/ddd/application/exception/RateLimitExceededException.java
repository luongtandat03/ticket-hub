/*
 * @ (#) RateLimitExceededException.java       1.0     10/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */
package vn.tphcm.ddd.application.exception;

/*
 * @author: Luong Tan Dat
 * @date: 10/13/2026
 */

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {
    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Too many requests, please try again later");
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
