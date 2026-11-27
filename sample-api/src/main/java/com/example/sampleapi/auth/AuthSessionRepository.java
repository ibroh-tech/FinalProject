package com.example.sampleapi.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {

    Optional<AuthSession> findByRefreshToken(String refreshToken);
}
