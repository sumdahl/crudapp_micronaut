package com.micronaut.crud.application.service;

import com.micronaut.crud.domain.entity.RefreshToken;
import com.micronaut.crud.domain.repository.RefreshTokenRepository;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.mindrot.jbcrypt.BCrypt;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class RefreshTokenService {

    private static final int TOKEN_LENGTH = 32; // 32 bytes = 256 bits
    private static final int REFRESH_TOKEN_VALIDITY_DAYS = 7;
    private static final SecureRandom secureRandom = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public String generateAndStore(UUID userId) {
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        String plainToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        String tokenHash = BCrypt.hashpw(plainToken, BCrypt.gensalt());

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(REFRESH_TOKEN_VALIDITY_DAYS);

        RefreshToken refreshToken = new RefreshToken(userId, tokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);

        return plainToken;
    }

    @Transactional
    public Optional<String> validateAndRotate(String plainToken, UUID userId) {
        if (plainToken == null || plainToken.isEmpty()) {
            return Optional.empty();
        }

        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHash(
                hashTokenForLookup(plainToken));

        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }

        RefreshToken token = tokenOpt.get();

        if (!token.getUserId().equals(userId) || !token.isValid()) {
            return Optional.empty();
        }

        token.setRevoked(true);
        refreshTokenRepository.update(token);

        String newToken = generateAndStore(userId);
        return Optional.of(newToken);
    }

    public Optional<UUID> verifyToken(String plainToken) {
        if (plainToken == null || plainToken.isEmpty()) {
            return Optional.empty();
        }

        String hashedToken = hashTokenForLookup(plainToken);
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHash(hashedToken);

        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }

        RefreshToken token = tokenOpt.get();
        if (token.isValid()) {
            return Optional.of(token.getUserId());
        }

        return Optional.empty();
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Transactional
    public void cleanup() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        refreshTokenRepository.deleteRevokedTokens();
    }

    private String hashTokenForLookup(String plainToken) {
        return plainToken;
    }
}
