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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.tphcm.ddd.application.dto.SignInResponse;
import vn.tphcm.ddd.application.service.authentication.AuthenticationAppService;
import vn.tphcm.ddd.controller.dto.ApiResponse;
import vn.tphcm.ddd.controller.dto.request.SignInRequest;
import vn.tphcm.ddd.controller.dto.request.VerifyRequest;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequiredArgsConstructor
@RequestMapping("/identity/auth")
@Slf4j(topic = "AUTHENTICATION-CONTROLLER")
public class AuthenticationController {
    private final AuthenticationAppService authenticationAppService;

    @PostMapping("/login")
    public ApiResponse<SignInResponse> login(@RequestBody SignInRequest request) {
        log.info("Login with username or email or phone number: {}", request.getUsernameOrEmailOrPhoneNumber());

        return ApiResponse.<SignInResponse>builder()
                .status(OK.value())
                .message("Login successful")
                .data(authenticationAppService.login(request.getUsernameOrEmailOrPhoneNumber(), request.getPassword()))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @PostMapping("/confirm")
    public ApiResponse<Boolean> confirmCode(@RequestBody VerifyRequest request) {
        log.info("Confirm code with verify request: {}", request.getCode());

        return ApiResponse.<Boolean>builder()
                .status(OK.value())
                .message("Confirm code successful")
                .build();
    }
}
