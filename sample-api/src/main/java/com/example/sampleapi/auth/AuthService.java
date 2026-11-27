package com.example.sampleapi.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

    private final AccountRepository accountRepository;
    private final AuthSessionRepository authSessionRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AccountRepository accountRepository, AuthSessionRepository authSessionRepository,
                       PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.authSessionRepository = authSessionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (accountRepository.existsByEmail(email)) {
            throw new DuplicateAccountException(email);
        }
        Account account = new Account(
                request.name().strip(),
                email,
                passwordEncoder.encode(request.password()));
        accountRepository.save(account);
        return issueTokens(account.getId());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Account account = accountRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        return issueTokens(account.getId());
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        AuthSession session = authSessionRepository.findByRefreshToken(request.refreshToken())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired refresh token"));
        Instant now = Instant.now();
        if (session.isRevoked() || !session.getRefreshExpiresAt().isAfter(now)) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }
        session.revoke();
        return issueTokens(session.getAccountId());
    }

    private AuthResponse issueTokens(Long accountId) {
        Instant now = Instant.now();
        String accessToken = UUID.randomUUID().toString();
        String refreshToken = UUID.randomUUID().toString();
        AuthSession session = new AuthSession(
                accountId,
                accessToken,
                refreshToken,
                now.plus(ACCESS_TOKEN_TTL),
                now.plus(REFRESH_TOKEN_TTL));
        authSessionRepository.save(session);
        return new AuthResponse(accessToken, refreshToken, "Bearer", ACCESS_TOKEN_TTL.toSeconds());
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
