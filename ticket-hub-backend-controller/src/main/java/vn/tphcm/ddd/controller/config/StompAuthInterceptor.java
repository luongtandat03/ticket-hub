/*
 * @ (#) StompAuthInterceptor.java       1.0     10/16/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.controller.config;
/*
 * @author: Luong Tan Dat
 * @date: 10/16/2026
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import vn.tphcm.ddd.application.service.authentication.TokenAppService;
import vn.tphcm.ddd.domain.service.CustomUserDetailsService;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "STOMP-AUTH")
public class StompAuthInterceptor implements ChannelInterceptor {
    private static final String AGENT_QUEUE = "/topic/agent-queue";


    private final TokenAppService tokenAppService;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    public @Nullable Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message,
                StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
            return message;
        }

        if (StompCommand.SEND.equals(command) || StompCommand.SUBSCRIBE.equals(command)) {
            if (!(accessor.getUser() instanceof Authentication authentication)) {
                throw new AccessDeniedException("Unauthenticated STOMP session");
            }

            if (StompCommand.SUBSCRIBE.equals(command) && AGENT_QUEUE.equals(accessor.getDestination())
                    && !isAgent(authentication)) {
                throw new AccessDeniedException("Only support agents can subscribe to " + AGENT_QUEUE);
            }
        }

        return message;
    }

    private Authentication authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AccessDeniedException("Missing bearer token");
        }

        String jwt = authorization.substring(7);
//
//        if (!tokenAppService.isTokenValid(jwt, 1)) {
//            throw new AccessDeniedException("Invalid token");
//        }
//
//        String userId = tokenAppService.extractUsername(jwt, 1);

        String userId = tokenAppService.validateAndGetUserId(jwt, 1);

        if (userId == null) {
            throw new AccessDeniedException("Invalid token");
        }

        UserDetails user = customUserDetailsService.loadUserById(userId);

        log.info("[STOMP] CONNECT userId={}", userId);

        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    private boolean isAgent(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
