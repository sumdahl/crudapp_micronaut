package com.micronaut.crud.application.utils;

import com.micronaut.crud.presentation.dto.UserResponse;
import java.time.Instant;
import java.util.Map;

public class JwtUtils {


    private static final long ACCESS_TOKEN_EXPIRY_SECONDS = 5 * 60; // 5 minutes

    public static Map<String, Object> buildClaims(UserResponse user) {
        long exp = Instant.now().plusSeconds(ACCESS_TOKEN_EXPIRY_SECONDS).getEpochSecond();

        return Map.of(
                "sub", user.getEmail(),
                "userId", user.getId().toString(),
                "roles", user.getRoles(),
                "exp", exp
        );
    }
}
