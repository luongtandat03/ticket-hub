/*
 * @ (#) RateLimitInterceptor.java       1.0     10/13/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.controller.ratelimit;
/*
 * @author: Luong Tan Dat
 * @date: 10/13/2026
 */

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import vn.tphcm.ddd.application.exception.RateLimitExceededException;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RateLimiterService;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {
    private static final int DEFAULT_LIMIT = 50;
    private static final int DEFAULT_WINDOW_SECONDS = 60;

    private final RateLimiterService rateLimiterService;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        RateLimit annotation = method.getMethodAnnotation(RateLimit.class);

        int limit = annotation != null ? annotation.limit() : DEFAULT_LIMIT;
        int windowSeconds = annotation != null ? annotation.windowSeconds() : DEFAULT_WINDOW_SECONDS;

        String key = method.getBeanType().getSimpleName() + "#" + method.getMethod().getName()
                + ":" + clientId(request);

        RateLimiterService.Result result = rateLimiterService.tryAcquire(key, limit, Duration.ofSeconds(windowSeconds));

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));

        if (!result.isAllowed()) {
            throw new RateLimitExceededException(result.retryAfterSeconds());
        }

        return true;
    }

    private String clientId(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "user:" + auth.getName();
        }

        return "ip:" + request.getRemoteAddr();
    }
}
