/*
 * @ (#) SignInResponse.java       1.0     9/21/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.dto;
/*
 * @author: Luong Tan Dat
 * @date: 9/21/2026
 */

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SignInResponse {
    private String accessToken;
    private String refreshToken;
    private String usernameOrEmailOrPhoneNumber;
    private String userId;
}
