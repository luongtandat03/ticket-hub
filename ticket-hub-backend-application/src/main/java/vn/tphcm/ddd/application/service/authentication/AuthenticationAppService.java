/*
 * @ (#) AuthenticationAppService.java       1.0     9/21/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.authentication;
/*
 * @author: Luong Tan Dat
 * @date: 9/21/2026
 */

import vn.tphcm.ddd.application.dto.response.SignInResponse;
import vn.tphcm.ddd.application.dto.response.UserResponse;

public interface AuthenticationAppService {
    SignInResponse login(String usernameOrEmailOrPhoneNumber, String password);

    UserResponse register(String username, String email, String password, String phoneNumber);

    SignInResponse confirmCode(String username, String code);

    Void logout(String jwt);

    Void forgotPassword(String email);
}
