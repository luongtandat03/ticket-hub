package vn.tphcm.ddd.controller.ratelimit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.tphcm.ddd.application.dto.response.SignInResponse;
import vn.tphcm.ddd.application.dto.response.TicketResponse;
import vn.tphcm.ddd.application.exception.GlobalExceptionHandling;
import vn.tphcm.ddd.application.service.authentication.AuthenticationAppService;
import vn.tphcm.ddd.application.service.ticket.TicketAppService;
import vn.tphcm.ddd.controller.http.AuthenticationController;
import vn.tphcm.ddd.controller.http.TicketController;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RateLimiterService;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RateLimitTest {
    private final RateLimiterService limiter = mock(RateLimiterService.class);
    private final AuthenticationAppService authService = mock(AuthenticationAppService.class);
    private final TicketAppService ticketService = mock(TicketAppService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        when(limiter.tryAcquire(anyString(), anyInt(), any(Duration.class)))
                .thenReturn(new RateLimiterService.Result(true, 9, 0));
        when(authService.register(any(), any(), any(), any())).thenReturn(null);
        when(ticketService.getTicketById(any())).thenReturn(new TicketResponse());
        when(authService.login(any(), any())).thenReturn(SignInResponse.builder().build());

        mvc = MockMvcBuilders.standaloneSetup(new AuthenticationController(authService), new TicketController(ticketService))
                .addInterceptors(new RateLimitInterceptor(limiter))
                .setControllerAdvice(new GlobalExceptionHandling())
                .build();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static final String REGISTER_BODY =
            "{\"username\":\"a\",\"email\":\"a@x.com\",\"password\":\"pw\",\"phoneNumber\":\"090\"}";

    @Test
    void allowedRequest_getsRateLimitHeaders() throws Exception {
        mvc.perform(post("/identity/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "10"))
                .andExpect(header().string("X-RateLimit-Remaining", "9"));
    }

    @Test
    void annotatedEndpoint_usesAnnotationLimitAndWindow() throws Exception {
        mvc.perform(post("/identity/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY));

        verify(limiter).tryAcquire(startsWith("AuthenticationController#register:"), eq(10), eq(Duration.ofSeconds(60)));
    }

    @Test
    void endpointWithoutAnnotation_usesDefaultLimit() throws Exception {
        mvc.perform(get("/ticket/t1")).andExpect(status().isOk());

        verify(limiter).tryAcquire(startsWith("TicketController#getTicket:"), eq(50), eq(Duration.ofSeconds(60)));
    }

    @Test
    void anonymousClient_isKeyedByIp() throws Exception {
        mvc.perform(get("/ticket/t1").with(r -> {
            r.setRemoteAddr("10.1.2.3");
            return r;
        }));

        verify(limiter).tryAcquire(endsWith(":ip:10.1.2.3"), anyInt(), any(Duration.class));
    }

    @Test
    void authenticatedClient_isKeyedByUsername() throws Exception {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("alice", "n/a", java.util.List.of()));

        mvc.perform(get("/ticket/t1"));

        verify(limiter).tryAcquire(endsWith(":user:alice"), anyInt(), any(Duration.class));
    }

    @Test
    void overLimit_returns429WithRetryAfterAndDoesNotCallController() throws Exception {
        when(limiter.tryAcquire(anyString(), anyInt(), any(Duration.class)))
                .thenReturn(new RateLimiterService.Result(false, 0, 42));

        mvc.perform(post("/identity/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "42"))
                .andExpect(jsonPath("$.status").value(429));

        verifyNoInteractions(authService);
    }
}
