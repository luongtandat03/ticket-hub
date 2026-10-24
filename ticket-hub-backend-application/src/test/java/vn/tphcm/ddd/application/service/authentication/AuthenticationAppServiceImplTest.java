package vn.tphcm.ddd.application.service.authentication;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import vn.tphcm.ddd.application.dto.response.SignInResponse;
import vn.tphcm.ddd.application.dto.response.UserResponse;
import vn.tphcm.ddd.application.exception.RateLimitExceededException;
import vn.tphcm.ddd.application.mapper.UserMapper;
import vn.tphcm.ddd.application.service.authentication.impl.AuthenticationAppServiceImpl;
import vn.tphcm.ddd.domain.model.OutboxEvent;
import vn.tphcm.ddd.domain.model.Role;
import vn.tphcm.ddd.domain.model.User;
import vn.tphcm.ddd.domain.repository.OutboxEventRepository;
import vn.tphcm.ddd.domain.repository.RoleRepository;
import vn.tphcm.ddd.domain.service.UserDomainService;
import vn.tphcm.ddd.infrastructure.cache.redis.RedisInfrastructureService;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RateLimiterService;

import java.time.Duration;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthenticationAppServiceImplTest {
    @Mock TokenAppService tokenAppService;
    @Mock AuthenticationManager authenticationManager;
    @Mock RedisInfrastructureService redis;
    @Mock UserDomainService userDomainService;
    @Mock TransactionTemplate transactionTemplate;
    @Mock OutboxEventRepository outboxEventRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RoleRepository roleRepository;
    @Mock UserMapper userMapper;
    @Mock RateLimiterService rateLimiterService;

    @InjectMocks AuthenticationAppServiceImpl service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        when(transactionTemplate.execute(any())).thenAnswer(inv ->
                ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
        when(rateLimiterService.tryAcquire(anyString(), anyInt(), any(Duration.class)))
                .thenReturn(new RateLimiterService.Result(true, 4, 0));
        when(tokenAppService.generateAccessToken(any(), any())).thenReturn("access");
        when(tokenAppService.generateRefreshToken(any())).thenReturn("refresh");
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private User user(int status) {
        User u = User.builder()
                .username("alice").email("a@x.com").status(status)
                .roles(new ArrayList<>()).build();
        u.setId("u1");
        return u;
    }

    // ---------------- login ----------------

    @Test
    void login_success_returnsTokens() {
        User u = user(1);
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));

        SignInResponse rsp = service.login("alice", "pw");

        assertEquals("access", rsp.getAccessToken());
        assertEquals("refresh", rsp.getRefreshToken());
    }

    @Test
    void login_badCredentials_isAccessDenied() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThrows(AccessDeniedException.class, () -> service.login("alice", "wrong"));
        verifyNoInteractions(tokenAppService);
    }

    @Test
    void login_disabledAccount_isAccessDenied() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("off"));

        assertThrows(AccessDeniedException.class, () -> service.login("alice", "pw"));
    }

    @Test
    void login_unexpectedError_isAccessDenied() {
        when(authenticationManager.authenticate(any())).thenThrow(new IllegalStateException("boom"));

        assertThrows(AccessDeniedException.class, () -> service.login("alice", "pw"));
    }

    @Test
    void login_inactiveUserStatus_isAccessDeniedAndNoTokenIssued() {
        User u = user(0);
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));

        assertThrows(AccessDeniedException.class, () -> service.login("alice", "pw"));
        verify(tokenAppService, never()).generateAccessToken(any(), any());
    }

    // ---------------- register ----------------

    @Test
    void register_duplicateUsername_throwsConflict() {
        when(userDomainService.existsByUsername("alice")).thenReturn(true);

        assertThrows(DataIntegrityViolationException.class,
                () -> service.register("alice", "a@x.com", "pw", "0900000000"));
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        when(userDomainService.existsByEmail("a@x.com")).thenReturn(true);

        assertThrows(DataIntegrityViolationException.class,
                () -> service.register("alice", "a@x.com", "pw", "0900000000"));
    }

    @Test
    void register_duplicatePhone_throwsConflict() {
        when(userDomainService.existsByPhoneNumber("0900000000")).thenReturn(true);

        assertThrows(DataIntegrityViolationException.class,
                () -> service.register("alice", "a@x.com", "pw", "0900000000"));
    }

    @Test
    void register_success_savesInactiveUserWithEncodedPasswordAndQueuesEmail() {
        Role member = new Role();
        member.setKey("MEMBER");
        when(roleRepository.findByKey("MEMBER")).thenReturn(member);
        when(passwordEncoder.encode("pw")).thenReturn("ENC");
        UserResponse mapped = mock(UserResponse.class);
        when(userMapper.toResponse(any())).thenReturn(mapped);

        UserResponse result = service.register("alice", "a@x.com", "pw", "0900000000");

        assertSame(mapped, result);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userDomainService).save(userCaptor.capture());
        assertEquals("ENC", userCaptor.getValue().getPassword());
        assertEquals(0, userCaptor.getValue().getStatus());
        assertEquals("alice", userCaptor.getValue().getUsername());

        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(eventCaptor.capture());
        assertEquals("SEND_EMAIL", eventCaptor.getValue().getEventType());
        assertEquals(0, eventCaptor.getValue().getStatus());
        assertTrue(eventCaptor.getValue().getPayload().contains("VERIFY_EMAIL"));
        verify(redis).setOTP(startsWith("VERIFY:a@x.com:"), anyString());
    }

    @Test
    void register_unexpectedFailure_isWrappedAsAccessDenied() {
        when(roleRepository.findByKey("MEMBER")).thenThrow(new IllegalStateException("db"));

        assertThrows(AccessDeniedException.class,
                () -> service.register("alice", "a@x.com", "pw", "0900000000"));
    }

    // ---------------- confirmCode ----------------

    @Test
    void confirmCode_unknownUser_throws() {
        when(userDomainService.findByEmail("a@x.com")).thenReturn(null);

        assertThrows(DataIntegrityViolationException.class, () -> service.confirmCode("a@x.com", "ABC123"));
    }

    @Test
    void confirmCode_alreadyActive_throws() {
        when(userDomainService.findByEmail("a@x.com")).thenReturn(user(1));

        assertThrows(DataIntegrityViolationException.class, () -> service.confirmCode("a@x.com", "ABC123"));
    }

    @Test
    void confirmCode_wrongOrExpiredCode_throwsAndDoesNotActivate() {
        User u = user(0);
        when(userDomainService.findByEmail("a@x.com")).thenReturn(u);
        when(redis.getString(anyString())).thenReturn(null);

        assertThrows(DataIntegrityViolationException.class, () -> service.confirmCode("a@x.com", "ABC123"));
        assertEquals(0, u.getStatus());
        verify(userDomainService, never()).save(any());
    }

    @Test
    void confirmCode_validCode_activatesUserDeletesOtpAndReturnsTokens() {
        User u = user(0);
        when(userDomainService.findByEmail("a@x.com")).thenReturn(u);
        when(redis.getString("VERIFY:a@x.com:ABC123:EMAIL")).thenReturn("ABC123");

        SignInResponse rsp = service.confirmCode("a@x.com", "ABC123");

        assertEquals(1, u.getStatus());
        assertEquals("access", rsp.getAccessToken());
        verify(userDomainService).save(u);
        verify(redis).deleteObject("VERIFY:a@x.com:ABC123:EMAIL");
    }

    // ---------------- logout / forgotPassword ----------------

    @Test
    void logout_delegatesToTokenService() {
        service.logout("tok");

        verify(tokenAppService).deletedToken("tok");
    }

    @Test
    void forgotPassword_currentlyDoesNothing() {
        // Chức năng chưa được cài đặt: test này ghi nhận hành vi hiện tại
        // và sẽ cần viết lại khi forgotPassword được hoàn thiện.
        assertNull(service.forgotPassword("a@x.com"));
        verifyNoInteractions(outboxEventRepository);
    }

    // ---------------- giới hạn tốc độ theo tài khoản ----------------

    @Test
    void login_tooManyAttempts_isRejectedBeforeAuthenticating() {
        when(rateLimiterService.tryAcquire(eq("login:alice"), anyInt(), any(Duration.class)))
                .thenReturn(new RateLimiterService.Result(false, 0, 120));

        RateLimitExceededException ex = assertThrows(RateLimitExceededException.class,
                () -> service.login("Alice", "pw"));

        assertEquals(120, ex.getRetryAfterSeconds());
        verifyNoInteractions(authenticationManager);
    }

    @Test
    void login_rateLimitKeyIsCaseInsensitive() {
        User u = user(1);
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));

        service.login("ALICE", "pw");

        verify(rateLimiterService).tryAcquire(eq("login:alice"), eq(5), any(Duration.class));
    }

    @Test
    void confirmCode_tooManyAttempts_isRejectedBeforeCheckingOtp() {
        when(rateLimiterService.tryAcquire(eq("otp:a@x.com"), anyInt(), any(Duration.class)))
                .thenReturn(new RateLimiterService.Result(false, 0, 60));

        assertThrows(RateLimitExceededException.class, () -> service.confirmCode("A@x.com", "ABC123"));
        verifyNoInteractions(userDomainService);
    }
}
