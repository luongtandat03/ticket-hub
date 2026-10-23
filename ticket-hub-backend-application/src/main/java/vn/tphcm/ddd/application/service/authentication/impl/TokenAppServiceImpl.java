/*
 * @ (#) TokenAppServiceImpl.java       1.0     9/29/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.service.authentication.impl;
/*
 * @author: Luong Tan Dat
 * @date: 9/29/2026
 */

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import vn.tphcm.ddd.application.service.authentication.TokenAppService;
import vn.tphcm.ddd.infrastructure.cache.redis.RedisInfrastructureService;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "TOKEN-APP-SERVICE")
public class TokenAppServiceImpl implements TokenAppService {
    private final RedisInfrastructureService redisInfrastructureService;

    public static final String ACCESS_PREFIX = "accessToken:";
    public static final String REFRESH_PREFIX = "refreshToken:";
//    public static final String BLACKLIST_PREFIX = "blacklist:";

    @Value("${jwt.expiryMinutes}")
    private long expiryMinutes;

    @Value("${jwt.expiryDay}")
    private long expiryDay;

    @Value("${jwt.accessKey}")
    private String accessKey;

    @Value("${jwt.refreshKey}")
    private String refreshKey;

    @Override
    public String generateAccessToken(String username, List<String> authorities) {
        log.info("generateAccessToken: {}", username);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", username);
        claims.put("authorities", authorities);

        return generateToken(claims, username, 1);
    }

    @Override
    public String generateRefreshToken(String username) {
        log.info("generateRefreshToken: {}", username);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", username);

        return generateToken(claims, username, 2);
    }

//    @Override
//    public String extractUsername(String token, int tokenType) {
//        log.info("Extracting username from token: {}", token);
//
//        if (token == null || token.trim().isEmpty()) {
//            log.warn("Invalid token: {}", token);
//            throw new AccessDeniedException("Access denied, token is null or empty");
//        }
//
//        Claims claims = extractAllClaim(tokenType, token);
//
//        String userId = claims.getSubject();
//
//        String redisKey = genRedisPrefix(userId, tokenType);
//
//        if (redisInfrastructureService.getToken(redisKey) == null) {
//            throw new AccessDeniedException("Access denied, token is null");
//        }
//
//        return userId;
//    }

    @Override
    public void deletedToken(String token) {
        Claims claims;
        try {
            claims = extractAllClaim(1, token);
        } catch (Exception e) {
            log.warn("Logout with invalid or expired token", e);
            return;
        }

        deleteToken(claims.getSubject());
    }

    @Override
    public String validateAndGetUserId(String jwt, int tokenType) {
        if (jwt == null || jwt.isBlank()) {
            return null;
        }

        try {
            String userId = extractAllClaim(tokenType, jwt).getSubject();

            if (userId == null || userId.isBlank()) {
                return null;
            }

            String storedToken = redisInfrastructureService.getString(genRedisPrefix(userId, tokenType));

            return jwt.equals(storedToken) ? userId : null;
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return null;
        }
    }

//    @Override
//    public boolean isTokenValid(String token, int tokenType) {
//        log.info("Validating token: {}", token);
//
//        try {
//            if (token == null || token.trim().isEmpty()) {
//                return false;
//            }
//

    /// /            String username = extractClaim(1, token, Claims::getSubject);
//
//            Claims claims = extractAllClaim(tokenType, token);
//
//            String userId = claims.getSubject();
//
//            if (userId == null || userId.trim().isEmpty()) {
//                log.error("Username extracted from token is null or empty");
//                return false;
//            }
//
//            if (isBlacklist(userId)) {
//                return false;
//            }
//
//            String storedToken = redisInfrastructureService.getString(genRedisPrefix(userId, tokenType));
//
//            if (storedToken == null || !storedToken.equals(token)) {
//                log.error("Token not found or does not match the stored token in Redis");
//                return false;
//            }
//
//            return true;
//        } catch (Exception e) {
//            log.error("Token validation failed: {}", e.getMessage());
//            return false;
//        }
//
//    }
//
//    @Override
//    public boolean isBlacklist(String jti) {
//        return redisInfrastructureService.isBlacklisted(BLACKLIST_PREFIX + jti);
//    }

//    private <T> T extractClaim(int type, String token, Function<Claims, T> claimsResolver) {
//        log.info("-----------------[EXTRACT CLAIM]-----------------");
//
//        final Claims claims = extractAllClaim(type, token);
//        return claimsResolver.apply(claims);
//    }
    private Claims extractAllClaim(int type, String token) {
        log.info("--------------[ extractAllClaim ]--------------");
        try {
            return Jwts.parser()
                    .verifyWith(getKey(type))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (ExpiredJwtException e) {
            throw new AccessDeniedException("Access Denied, error: " + e.getMessage());
        }
    }

    private String generateToken(Map<String, Object> claims, String subject, int tokenType) {
        long expiry = tokenType == 1 ? expiryMinutes : expiryDay * 24 * 60;

        String token = Jwts.builder()
                .subject(subject)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(Duration.ofMinutes(expiry))))
                .claims(claims)
                .signWith(getKey(tokenType))
                .compact();

        String redisKey = genRedisPrefix(subject, tokenType);
        redisInfrastructureService.setToken(redisKey, token, Duration.ofMinutes(expiry));

        return token;
    }

    private SecretKey getKey(int type) {
        switch (type) {
            case 1 -> {
                return Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessKey));
            }
            case 2 -> {
                return Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshKey));
            }
            default -> throw new IllegalArgumentException("Invalid token type");
        }
    }

    private String genRedisPrefix(String subject, int type) {
        return switch (type) {
            case 1 -> ACCESS_PREFIX + subject;
            case 2 -> REFRESH_PREFIX + subject;
            default -> throw new IllegalArgumentException("Invalid token type");
        };
    }

    private void deleteToken(String subject) {
        redisInfrastructureService.deleteToken(genRedisPrefix(subject, 1));
        redisInfrastructureService.deleteToken(genRedisPrefix(subject, 2));
    }
}
