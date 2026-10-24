package vn.tphcm.ddd.application.service.authentication;

import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import vn.tphcm.ddd.application.service.authentication.impl.TokenAppServiceImpl;
import vn.tphcm.ddd.infrastructure.cache.redis.RedisInfrastructureService;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Dùng một Redis giả trong bộ nhớ để token được lưu/tra cứu giống môi trường thật. */
class TokenAppServiceImplTest {
    private final Map<String, String> store = new HashMap<>();
    private RedisInfrastructureService redis;
    private TokenAppServiceImpl service;

    private static String newKey() {
        return Encoders.BASE64.encode(Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS512).getEncoded());
    }

    @BeforeEach
    void setUp() {
        redis = mock(RedisInfrastructureService.class);
        doAnswer(inv -> store.put(inv.getArgument(0), inv.getArgument(1)))
                .when(redis).setToken(anyString(), anyString(), any(Duration.class));
        when(redis.getString(anyString())).thenAnswer(inv -> store.get(inv.<String>getArgument(0)));
        doAnswer(inv -> store.remove(inv.<String>getArgument(0))).when(redis).deleteToken(anyString());

        service = new TokenAppServiceImpl(redis);
        ReflectionTestUtils.setField(service, "expiryMinutes", 15L);
        ReflectionTestUtils.setField(service, "expiryDay", 7L);
        ReflectionTestUtils.setField(service, "accessKey", newKey());
        ReflectionTestUtils.setField(service, "refreshKey", newKey());
    }

    @Test
    void accessToken_returnsUserId() {
        String token = service.generateAccessToken("user-1", List.of("ROLE_MEMBER"));

        assertEquals("user-1", service.validateAndGetUserId(token, 1));
    }

    @Test
    void validation_readsRedisOnce() {
        String token = service.generateAccessToken("user-1", List.of());

        service.validateAndGetUserId(token, 1);

        verify(redis, times(1)).getString(TokenAppServiceImpl.ACCESS_PREFIX + "user-1");
        verifyNoMoreInteractions(ignoreStubs(redis));
    }

    @Test
    void refreshToken_isNotAcceptedAsAccessToken() {
        String refresh = service.generateRefreshToken("user-1");

        assertEquals("user-1", service.validateAndGetUserId(refresh, 2));
        assertNull(service.validateAndGetUserId(refresh, 1));
    }

    @Test
    void tamperedBlankOrGarbageToken_isInvalid() {
        String token = service.generateAccessToken("user-1", List.of());

        assertNull(service.validateAndGetUserId(token + "x", 1));
        assertNull(service.validateAndGetUserId(" ", 1));
        assertNull(service.validateAndGetUserId(null, 1));
        assertNull(service.validateAndGetUserId("x.y.z", 1));
    }

    @Test
    void invalidTokenType_isInvalid() {
        String token = service.generateAccessToken("user-1", List.of());

        assertNull(service.validateAndGetUserId(token, 9));
    }

    @Test
    void oldToken_isRejectedAfterNewLogin() {
        String first = service.generateAccessToken("user-1", List.of());
        String second = service.generateAccessToken("user-1", List.of());

        assertNull(service.validateAndGetUserId(first, 1));
        assertEquals("user-1", service.validateAndGetUserId(second, 1));
    }

    @Test
    void tokenMissingFromRedis_isInvalid() {
        String token = service.generateAccessToken("user-1", List.of());
        store.clear();

        assertNull(service.validateAndGetUserId(token, 1));
    }

    @Test
    void expiredToken_isInvalid() {
        ReflectionTestUtils.setField(service, "expiryMinutes", -1L);
        String token = service.generateAccessToken("user-1", List.of());

        assertNull(service.validateAndGetUserId(token, 1));
    }

    @Test
    void logout_revokesStoredTokens() {
        String access = service.generateAccessToken("user-1", List.of());
        service.generateRefreshToken("user-1");

        service.deletedToken(access);

        assertNull(service.validateAndGetUserId(access, 1));
        assertNull(store.get(TokenAppServiceImpl.ACCESS_PREFIX + "user-1"));
        assertNull(store.get(TokenAppServiceImpl.REFRESH_PREFIX + "user-1"));
    }

    @Test
    void reLoginAfterLogout_newTokenWorks() {
        String first = service.generateAccessToken("user-1", List.of());
        service.deletedToken(first);

        String second = service.generateAccessToken("user-1", List.of());

        // Trước đây key blacklist:<userId> chặn luôn token mới cho tới khi token cũ hết hạn (7 ngày)
        assertEquals("user-1", service.validateAndGetUserId(second, 1));
        assertNull(service.validateAndGetUserId(first, 1));
        verify(redis, never()).addBlacklistToken(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void logout_withGarbageToken_doesNotThrow() {
        assertDoesNotThrow(() -> service.deletedToken("not-a-jwt"));
    }
}
