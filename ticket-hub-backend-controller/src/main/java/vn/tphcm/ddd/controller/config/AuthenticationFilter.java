/*
 * @ (#) AuthenticationFilter.java       1.0     9/29/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.controller.config;
/*
 * @author: Luong Tan Dat
 * @date: 9/29/2026
 */

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.tphcm.ddd.application.service.authentication.TokenAppService;
import vn.tphcm.ddd.domain.service.CustomUserDetailsService;
import vn.tphcm.ddd.infrastructure.cache.redis.RedisInfrastructureService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "AUTHENTICATION-FILTER")
public class AuthenticationFilter extends OncePerRequestFilter {
    private final TokenAppService tokenAppService;
    private final CustomUserDetailsService customUserDetailsService;
    private final RedisInfrastructureService redisInfrastructureService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);

//        if (!tokenAppService.isTokenValid(jwt, 1)) {
//            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
//            return;
//        }
//
//        String username = tokenAppService.extractUsername(jwt, 1);
//
//        if (tokenAppService.isBlacklist(username)) {
//            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has been expired");
//            return;
//        }

        String userId = tokenAppService.validateAndGetUserId(jwt, 1);

        if (userId == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails user = customUserDetailsService.loadUserById(userId);
            if (user == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not found");
                return;
            }
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);


        }

        filterChain.doFilter(request, response);
    }
}
