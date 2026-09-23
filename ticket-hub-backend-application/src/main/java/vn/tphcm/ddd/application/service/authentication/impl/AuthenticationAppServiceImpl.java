/*
 * @ (#) AuthenticationAppServiceImpl.java       1.0     9/21/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.authentication.impl;
/*
 * @author: Luong Tan Dat
 * @date: 9/21/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.tphcm.ddd.application.dto.SignInResponse;
import vn.tphcm.ddd.application.service.authentication.AuthenticationAppService;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "AUTHENTICATION-APP-SERVICE")
public class AuthenticationAppServiceImpl implements AuthenticationAppService {
    @Override
    public SignInResponse login(String username, String password) {
        return null;
    }

    @Override
    public Boolean confirmCode(String code) {
        return null;
    }
}
