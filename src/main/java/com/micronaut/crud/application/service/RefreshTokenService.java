package com.micronaut.crud.application.service;

import com.micronaut.crud.domain.entity.RefreshToken;
import com.micronaut.crud.domain.repository.RefreshTokenRepository;
import com.micronaut.crud.presentation.dto.RefreshResult;
import io.micronaut.scheduling.annotation.Scheduled;
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
    public Optional<UUID> validateAndRotate(String plainToken) {
        if (plainToken == null || plainToken.isBlank()) {
            return Optional.empty();
        }


        var validTokens = refreshTokenRepository.findAllValidTokens(LocalDateTime.now());

        for (RefreshToken token : validTokens) {
            if (BCrypt.checkpw(plainToken, token.getTokenHash())) {
                // Revoke current token
                token.setRevoked(true);
                refreshTokenRepository.update(token);

                // Generate new token
                generateAndStore(token.getUserId());

                return Optional.of(token.getUserId());
            }
        }

        return Optional.empty();
    }

    @Transactional
    public Optional<RefreshResult> validateAndRotateWithUser(String plainToken){
        Optional<UUID> userIdOpt = validateAndRotate(plainToken);
        if(userIdOpt.isEmpty()){
            return Optional.empty();
        }

        UUID userId = userIdOpt.get();
        String newToken = generateAndStore(userId);

        return Optional.of(new RefreshResult(userId, newToken));
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Scheduled(fixedDelay = "1h")
    @Transactional
    public void cleanup() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        refreshTokenRepository.deleteRevokedTokens();
    }

    private String hashTokenForLookup(String plainToken) {
        return plainToken;
    }

    public boolean validate(String plainToken) {
        if (plainToken == null || plainToken.isBlank()) return false;

        return refreshTokenRepository.findAllValidTokens(LocalDateTime.now())
                .stream()
                .anyMatch(rt -> BCrypt.checkpw(plainToken, rt.getTokenHash()));
    }


}
