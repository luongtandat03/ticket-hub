/*
 * @ (#) GlobalExceptionHandling.java       1.0     10/10/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.exception;
/*
 * @author: Luong Tan Dat
 * @date: 10/10/2026
 */

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.Date;

import static org.springframework.http.HttpStatus.PAYMENT_REQUIRED;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@RestControllerAdvice
public class GlobalExceptionHandling {
    /**
     * Handle exception when payment is problem.
     *
     * @param e
     * @param req
     * @return
     */
    @ExceptionHandler(PaymentException.class)
    @ResponseStatus(PAYMENT_REQUIRED)
    public ErrorResponse handleAccessDeniedException(PaymentException e, WebRequest req) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimestamp(new Date());
        errorResponse.setPath(req.getDescription(false).replace("url=", ""));
        errorResponse.setStatus(PAYMENT_REQUIRED.value());
        errorResponse.setError(PAYMENT_REQUIRED.getReasonPhrase());
        errorResponse.setMessage(e.getMessage());
        return errorResponse;
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException e, WebRequest req) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimestamp(new Date());
        errorResponse.setPath(req.getDescription(false).replace("url=", ""));
        errorResponse.setStatus(TOO_MANY_REQUESTS.value());
        errorResponse.setError(TOO_MANY_REQUESTS.getReasonPhrase());
        errorResponse.setMessage(e.getMessage());

        return ResponseEntity.status(TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(e.getRetryAfterSeconds()))
                .body(errorResponse);
    }

    @ExceptionHandler(AiChatClientDisconnectedException.class)
    public ResponseEntity<ErrorResponse> handleAiChatClientDisconnectedException(AiChatClientDisconnectedException e, WebRequest req) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimestamp(new Date());
        errorResponse.setPath(req.getDescription(false).replace("url=", ""));
        errorResponse.setStatus(PAYMENT_REQUIRED.value());
        errorResponse.setError(PAYMENT_REQUIRED.getReasonPhrase());
        errorResponse.setMessage(e.getMessage());

        return ResponseEntity.status(PAYMENT_REQUIRED)
                .body(errorResponse);
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInputException(InvalidInputException e, WebRequest req) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimestamp(new Date());
        errorResponse.setPath(req.getDescription(false).replace("url=", ""));
        errorResponse.setStatus(PAYMENT_REQUIRED.value());
        errorResponse.setError(PAYMENT_REQUIRED.getReasonPhrase());
        errorResponse.setMessage(e.getMessage());

        return ResponseEntity.status(PAYMENT_REQUIRED)
                .body(errorResponse);
    }


}
