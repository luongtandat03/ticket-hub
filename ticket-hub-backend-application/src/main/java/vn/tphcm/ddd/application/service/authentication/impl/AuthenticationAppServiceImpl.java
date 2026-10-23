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

import com.alibaba.fastjson.JSON;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import vn.tphcm.ddd.application.dto.response.SignInResponse;
import vn.tphcm.ddd.application.dto.response.UserResponse;
import vn.tphcm.ddd.application.exception.RateLimitExceededException;
import vn.tphcm.ddd.application.mapper.UserMapper;
import vn.tphcm.ddd.application.service.authentication.AuthenticationAppService;
import vn.tphcm.ddd.application.service.authentication.TokenAppService;
import vn.tphcm.ddd.domain.model.OutboxEvent;
import vn.tphcm.ddd.domain.model.Role;
import vn.tphcm.ddd.domain.model.User;
import vn.tphcm.ddd.domain.repository.OutboxEventRepository;
import vn.tphcm.ddd.domain.repository.RoleRepository;
import vn.tphcm.ddd.domain.service.UserDomainService;
import vn.tphcm.ddd.infrastructure.cache.redis.RedisInfrastructureService;
import vn.tphcm.ddd.infrastructure.distributed.redisson.RateLimiterService;
import vn.tphcm.ddd.infrastructure.mq.email.EmailMQMessage;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "AUTHENTICATION-APP-SERVICE")
public class AuthenticationAppServiceImpl implements AuthenticationAppService {
    private final TokenAppService tokenAppService;
    private final AuthenticationManager authenticationManager;
    private final RedisInfrastructureService redisInfrastructureService;
    private final UserDomainService userDomainService;
    private final TransactionTemplate transactionTemplate;
    private final OutboxEventRepository outboxEventRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final RateLimiterService rateLimiterService;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    @Override
    public SignInResponse login(String username, String password) {
        guard("login:" + username.toLowerCase(), 5, Duration.ofMinutes(5));

        log.info("Get Access Token");

        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            username, password
                    )
            );
        } catch (BadCredentialsException e) {
            log.error("Authentication failed for user: {}", username, e);
            throw new AccessDeniedException("Invalid username or password");
        } catch (DisabledException e) {
            log.error("Account disabled: {}", e.getMessage());
            throw new AccessDeniedException("Account is disabled");
        } catch (Exception e) {
            log.error("Unexpected error during login: {}", e.getMessage());
            throw new AccessDeniedException("Login failed");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = (User) authentication.getPrincipal();

        if (user.getStatus() != 1) {
            log.warn("User {} has been disabled", user.getUsername());
            throw new AccessDeniedException("User disabled");
        }

        log.info("Authentication successful for user: {}", user.getUsername());
        return genToken(user);
    }

    @Override
    public UserResponse register(String username, String email, String password, String phoneNumber) {
        if (userDomainService.existsByUsername(username)) {
            log.warn("Username {} already exists", username);
            throw new DataIntegrityViolationException("Data is already in use");
        }

        if (userDomainService.existsByEmail(email)) {
            log.warn("Email {} already exists", email);
            throw new DataIntegrityViolationException("Data is already in use");
        }

        if (userDomainService.existsByPhoneNumber(phoneNumber)) {
            log.warn("Phone number {} already exists", phoneNumber);
            throw new DataIntegrityViolationException("Data is already in use");
        }

        String token = "MQ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String code = generateCode();

        String key = generateKeyRedis(code, email);
        redisInfrastructureService.setOTP(key, code);
        String passwordEncode = passwordEncoder.encode(password);

        try {
            User user = transactionTemplate.execute(txStatus -> {
                Role role = roleRepository.findByKey("MEMBER");

                User userRegister = User.builder()
                        .username(username)
                        .email(email)
                        .password(passwordEncode)
                        .phoneNumber(phoneNumber)
                        .status(0)
                        .roles(new ArrayList<>(List.of(role)))
                        .build();

                userDomainService.save(userRegister);

                String messageId = UuidCreator.getTimeOrderedEpoch().toString();

                EmailMQMessage message = new EmailMQMessage(
                        token, messageId, "VERIFY_EMAIL", userRegister.getEmail(), code, null, System.currentTimeMillis()
                );

                OutboxEvent event = OutboxEvent.builder()
                        .aggregateId(token)
                        .eventType("SEND_EMAIL")
                        .payload(JSON.toJSONString(message))
                        .status(0)
                        .createdAt(LocalDateTime.now(ZoneId.systemDefault()))
                        .build();

                outboxEventRepository.save(event);

                return userRegister;
            });

            return userMapper.toResponse(user);

        } catch (DataIntegrityViolationException e) {
            log.warn("Registration conflict on unique constraint");
            throw e;
        } catch (Exception e) {
            log.error("Failed to register account", e);
            throw new AccessDeniedException("Registration failed", e);
        }
    }

    @Override
    public SignInResponse confirmCode(String email, String code) {
        guard("otp:" + email.toLowerCase(), 5, Duration.ofMinutes(5));


        User user = userDomainService.findByEmail(email);

        if (user == null) {
            log.error("User {} does not exist", email);
            throw new DataIntegrityViolationException("User not found");
        }

        if (user.getStatus() != 0) {
            log.error("User {} has been disabled", email);
            throw new DataIntegrityViolationException("User is already in use");
        }

        String key = generateKeyRedis(code, email);
        String storageRedis = redisInfrastructureService.getString(key);

        if (storageRedis == null || !storageRedis.equals(code)) {
            log.error("Invalid code: {}", code);
            throw new DataIntegrityViolationException("Invalid code");
        }

        user.setStatus(1);

        userDomainService.save(user);

        redisInfrastructureService.deleteObject(key);

        log.info("Authentication successful for user after confirm code: {}", user.getUsername());
        return genToken(user);
    }

    private SignInResponse genToken(User user) {
        List<String> authorities = user.getAuthorities().stream()
                .map(Object::toString)
                .toList();

        String accessToken = tokenAppService.generateAccessToken(user.getId(), authorities);
        String refreshToken = tokenAppService.generateRefreshToken(user.getId());

        return SignInResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public Void logout(String token) {
        tokenAppService.deletedToken(token);
        return null;
    }

    @Override
    public Void forgotPassword(String email) {
        return null;
    }

    private String generateKeyRedis(String key, String email) {
        return "VERIFY:" + email + ":" + key + ":EMAIL";
    }

    private final SecureRandom secureRandom = new SecureRandom();

    private String generateCode() {
        StringBuilder stringBuilder = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            int index = secureRandom.nextInt(CHARACTERS.length());
            stringBuilder.append(CHARACTERS.charAt(index));
        }

        return stringBuilder.toString();
    }

    private void guard(String key, int limit, Duration window) {
        RateLimiterService.Result result = rateLimiterService.tryAcquire(key, limit, window);

        if (!result.isAllowed()) {
            throw new RateLimitExceededException(result.retryAfterSeconds());
        }
    }
}
