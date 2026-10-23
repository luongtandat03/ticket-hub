/*
 * @ (#) SignInRequest.java       1.0     9/21/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.controller.dto.request;
/*
 * @author: Luong Tan Dat
 * @date: 9/21/2026
 */

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignInRequest {
    @NotBlank
    private String usernameOrEmailOrPhoneNumber;
    @NotBlank
    private String password;
}
