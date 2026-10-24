package vn.tphcm.ddd.controller.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import vn.tphcm.ddd.application.service.authentication.TokenAppService;
import vn.tphcm.ddd.domain.service.CustomUserDetailsService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StompAuthInterceptorTest {
    private TokenAppService tokenAppService;
    private CustomUserDetailsService userDetailsService;
    private StompAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        tokenAppService = mock(TokenAppService.class);
        userDetailsService = mock(CustomUserDetailsService.class);
        interceptor = new StompAuthInterceptor(tokenAppService, userDetailsService);

        // Giống UserDetailsServiceImpl thật: tìm theo username/email/phone, không tìm theo id
        when(userDetailsService.loadUserByUsername(any())).thenThrow(new UsernameNotFoundException("not a username"));
    }

    private StompHeaderAccessor connect(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", authorization);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        interceptor.preSend(message, mock(MessageChannel.class));
        return accessor;
    }

    @Test
    void connect_validToken_loadsUserById() {
        UserDetails user = mock(UserDetails.class);
        doReturn(List.of()).when(user).getAuthorities();
        when(tokenAppService.validateAndGetUserId("good", 1)).thenReturn("u1");
        when(userDetailsService.loadUserById("u1")).thenReturn(user);

        StompHeaderAccessor accessor = connect("Bearer good");

        // Token mang userId nên phải tìm theo id; loadUserByUsername(userId) không bao giờ tìm thấy
        Authentication authentication = (Authentication) accessor.getUser();
        assertNotNull(authentication);
        assertSame(user, authentication.getPrincipal());
    }

    @Test
    void connect_invalidToken_isRejected() {
        when(tokenAppService.validateAndGetUserId("bad", 1)).thenReturn(null);

        assertThrows(AccessDeniedException.class, () -> connect("Bearer bad"));
        verify(userDetailsService, never()).loadUserById(any());
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void connect_withoutBearerToken_isRejected() {
        assertThrows(AccessDeniedException.class, () -> connect(null));
        verifyNoInteractions(tokenAppService);
    }
}
