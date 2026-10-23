/*
 * @ (#) TokenAppService.java       1.0     9/29/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.authentication;

import java.util.List;

/*
 * @author: Luong Tan Dat
 * @date: 9/29/2026
 */
public interface TokenAppService {
    String generateAccessToken(String username, List<String> authorities);

    String generateRefreshToken(String username);

    void deletedToken(String token);

    String validateAndGetUserId(String jwt, int tokenType);

//    String extractUsername(String token, int tokenType);
//
//    boolean isTokenValid(String token, int tokenType);
//
//    boolean isBlacklist(String jti);
}
