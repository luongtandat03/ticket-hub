/*
 * @ (#) AuthenticationController.java       1.0     9/21/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.controller.http;
/*
 * @author: Luong Tan Dat
 * @date: 9/21/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vn.tphcm.ddd.application.dto.response.SignInResponse;
import vn.tphcm.ddd.application.dto.response.UserResponse;
import vn.tphcm.ddd.application.service.authentication.AuthenticationAppService;
import vn.tphcm.ddd.controller.dto.ApiResponse;
import vn.tphcm.ddd.controller.dto.request.CreationRequest;
import vn.tphcm.ddd.controller.dto.request.SignInRequest;
import vn.tphcm.ddd.controller.dto.request.VerifyRequest;
import vn.tphcm.ddd.controller.ratelimit.RateLimit;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequiredArgsConstructor
@RequestMapping("/identity")
@Slf4j(topic = "AUTHENTICATION-CONTROLLER")
public class AuthenticationController {
    private final AuthenticationAppService authenticationAppService;

    @PostMapping("/auth/register")
    @RateLimit(limit = 10, windowSeconds = 60)
    public ApiResponse<UserResponse> register(@RequestBody CreationRequest request) {
        log.info("Received request to register a new user");

        return ApiResponse.<UserResponse>builder()
                .status(OK.value())
                .message("Successfully registered a new user. Check your email to confirm your registration")
                .data(authenticationAppService.register(request.getUsername(), request.getEmail(), request.getPassword(), request.getPhoneNumber()))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @PostMapping("/auth/login")
    public ApiResponse<SignInResponse> login(@RequestBody SignInRequest request) {
        log.info("Login with username or email or phone number: {}", request.getUsernameOrEmailOrPhoneNumber());

        return ApiResponse.<SignInResponse>builder()
                .status(OK.value())
                .message("Login successful")
                .data(authenticationAppService.login(request.getUsernameOrEmailOrPhoneNumber(), request.getPassword()))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @PostMapping("/auth/confirm")
    public ApiResponse<SignInResponse> confirmCode(@RequestBody VerifyRequest request) {
        log.info("Confirm code with verify request: {}", request.getCode());

        return ApiResponse.<SignInResponse>builder()
                .status(OK.value())
                .data(authenticationAppService.confirmCode(request.getEmail(), request.getCode()))
                .message("Confirm code successful")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authorization) {
        log.info("Logout request");

        String jwt = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;

        return ApiResponse.<Void>builder()
                .status(OK.value())
                .message("Logout successful")
                .data(authenticationAppService.logout(jwt))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @PostMapping
    public ApiResponse<Void> forgotPassword(@RequestBody String email) {
        log.info("Forgot password request");

        return ApiResponse.<Void>builder()
                .status(OK.value())
                .message("Forgot password successful")
                .data(authenticationAppService.forgotPassword(email))
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
