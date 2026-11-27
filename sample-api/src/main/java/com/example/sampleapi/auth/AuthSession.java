package com.example.sampleapi.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "auth_sessions")
public class AuthSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false, unique = true, length = 36)
    private String accessToken;

    @Column(nullable = false, unique = true, length = 36)
    private String refreshToken;

    @Column(nullable = false)
    private Instant accessExpiresAt;

    @Column(nullable = false)
    private Instant refreshExpiresAt;

    @Column(nullable = false)
    private boolean revoked;

    protected AuthSession() {
    }

    public AuthSession(Long accountId, String accessToken, String refreshToken,
                       Instant accessExpiresAt, Instant refreshExpiresAt) {
        this.accountId = accountId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessExpiresAt = accessExpiresAt;
        this.refreshExpiresAt = refreshExpiresAt;
        this.revoked = false;
    }

    public void revoke() {
        this.revoked = true;
    }

    public Long getId() {
        return id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public Instant getAccessExpiresAt() {
        return accessExpiresAt;
    }

    public Instant getRefreshExpiresAt() {
        return refreshExpiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }
}
