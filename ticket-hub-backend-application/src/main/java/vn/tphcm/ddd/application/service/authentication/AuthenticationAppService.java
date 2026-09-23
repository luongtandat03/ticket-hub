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

import vn.tphcm.ddd.application.dto.SignInResponse;

public interface AuthenticationAppService {
    SignInResponse login(String username, String password);

    Boolean confirmCode(String code);
}
